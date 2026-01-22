package com.kntrel.mc.regionLib;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.util.HashCounter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class HotRegionRepository implements RegionRepository, Listener {

    //SUBTYPES
    public enum GridSize {
        //CONSTANTS
        SIZE_16(4),
        SIZE_32(5),
        SIZE_64(6),
        SIZE_128(7),
        SIZE_256(8);

        //FINDERS
        public static GridSize fromSize(int size) {
            for (GridSize gridSize : values()) {
                if (gridSize.getSize() == size) {
                    return gridSize;
                }
            }
            throw new IllegalArgumentException("No GridSize with size " + size);
        }
        public static GridSize fromShift(int shift) {
            for (GridSize gridSize : values()) {
                if (gridSize.shiftBy() == shift) {
                    return gridSize;
                }
            }
            throw new IllegalArgumentException("No GridSize with shift " + shift);
        }

        //FIELDS
        private final int shift_;

        //CONSTRUCTORS
        GridSize(int shift) { this.shift_ = shift; }

        //GETTERS
        public int shiftBy() { return this.shift_; }
        public int getSize() { return 1 << this.shift_; }
    }
    private record Bounds(double minX, double minZ, double maxX, double maxZ) {
        Bounds(Region region) {
            this(region.getMinX(), region.getMinZ(), region.getMaxX(), region.getMaxZ());
        }
    }
    private record GridCell(int x, int z, World world) {
        @Override public boolean equals(Object o) {
            if (o == null) { return false; }
            if (o == this) { return true; }
            if (!(o instanceof GridCell other)) { return false; }
            return this.x == other.x && this.z == other.z && this.world.getUID().equals(other.world.getUID());
        }
        @Override public int hashCode() {
            return Objects.hash(this.x, this.z, this.world.getUID());
        }
    }
    private static class Grid implements Iterable<GridCell> {

        private final int minX_, minZ_, length_, size_;
        private final World world_;

        Grid(int minX, int minZ, int maxX, int maxZ, World world) {
            this.minX_ = minX;
            this.minZ_ = minZ;
            this.length_ = maxX - minX + 1;
            this.size_ = this.length_ * (maxZ - minZ + 1);
            this.world_ = world;
        }

        @NotNull @Override
        public Iterator<GridCell> iterator() {
            return new GridIterator();
        }
        public Stream<GridCell> cellStream() {
            return StreamSupport.stream(this.spliterator(), false);
        }
        public Set<GridCell> cellSet() {
            return this.cellStream().collect(Collectors.toSet());
        }
        public boolean contains(GridCell cell) {
            return cell.x() >= this.minX_ && cell.x() < this.minX_ + this.length_
                && cell.z() >= this.minZ_ && cell.z() < this.minZ_ + this.size_ / this.length_;
        }

        private class GridIterator implements Iterator<GridCell> {
            private int pos_ = 0;

            @Override
            public boolean hasNext() {
                return this.pos_ < Grid.this.size_;
            }

            @Override
            public GridCell next() {
                int x = this.pos_ / Grid.this.length_,
                    z = this.pos_ % Grid.this.length_;
                this.pos_++;
                return new GridCell(Grid.this.minX_ + x, Grid.this.minZ_ + z, Grid.this.world_);
            }
        }
    }


    //FIELDS
    private final RegionRepository delegate_;
    private final GridSize gridSize_;
    private final Map<Long, Region> regionMap_;
    private final Map<GridCell, Set<Long>> cellRegionMap_;
    private final Map<Long, Bounds> boundsCache_;
    private final HashCounter<GridCell> chunksInCellCount_;
    private final HashCounter<Long> chunksInRegionCount_;
    private final HashCounter<Long> cellsInRegionCount_;



    //CONSTRUCTOR
    public HotRegionRepository(RegionRepository delegate, GridSize gridSize) {
        this.delegate_ = delegate;
        this.gridSize_ = gridSize;
        this.regionMap_ = new HashMap<>();
        this.cellRegionMap_ = new HashMap<>();
        this.boundsCache_ = new HashMap<>();
        this.chunksInCellCount_ = new HashCounter<>();
        this.chunksInRegionCount_ = new HashCounter<>();
        this.cellsInRegionCount_ = new HashCounter<>();
    }


    @Override
    public List<Region> get(Query query) {
        if (query.includesDestroyed()) {
            return this.delegate_.get(query);
        }

        Stream<Region> out = this.stageRegions(query.getCondition()).stream()
                .filter(r -> this.isHot(r.getId()))
                .filter(query.getCondition());

        Query.Ordering ordering = query.getOrdering().orElse(null);
        if (ordering != null) {
            final RegionField<? extends Comparable<?>> field = ordering.field();
            Comparator<Region> cmp = Comparator.comparing(r -> (Comparable) field.extract(r));
            if (!ordering.ascending()) {
                cmp = cmp.reversed();
            }
            out = out.sorted(cmp);
        }

        if (query.getLimit() > 0) {
            out = out.limit(query.getLimit());
        }

        return out.toList();
    }

    @Override
    public void save(Region... regions) {
        List<Region> toInsert = new ArrayList<>(regions.length);
        for (Region r : regions) {
            if (r.getId() == null) {
                toInsert.add(r);
                continue;
            }
            if (r.isDestroyed()) {
                this.remove(r);
                continue;
            }
            Bounds previous = this.boundsCache_.get(r.getId());
            if (previous == null) {
                previous = this.delegate_.get(r.getId()).map(Bounds::new).orElse(null);
                if (previous != null) { this.boundsCache_.put(r.getId(), previous); }
            }
            if (previous == null) {     //Theoretically we shouldn't get here.
                this.remove(r);
                this.insert(r);
                continue;
            }
            this.repair(previous, r);
        }

        this.delegate_.save(regions);

        for (Region r : toInsert) {
            if (r.getId() == null) {    //Sanity check that the delegate actually assigned and ID
                throw new RuntimeException("Delegate RegionRepository didn't assign and ID to a fresh region.");
            }
            this.insert(r);
        }
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        this.regionsInChunk(chunk)
                .map(Region::getId)
                .forEach(this::incrementHot);
        this.chunksInCellCount_.increment(this.cellOfChunk(chunk));
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        GridCell cell = this.cellOfChunk(chunk);
        if (isCellLoaded(cell)) {
            this.regionsInChunk(chunk)
                    .map(Region::getId)
                    .forEach(this::decrementHot);
        }

        if (this.chunksInCellCount_.decrementAndCheckZero(cell)) { this.unloadCell(cell); }
    }


    //PRIVATE
    private void linkToCell(GridCell cell, Region region) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) { return; }                    // not loaded

        long id = region.getId();
        if (!ids.add(id)) { return; }                   // already linked

        this.cellsInRegionCount_.increment(id);         // keeping invariants
        this.regionMap_.put(id, region);
        this.boundsCache_.computeIfAbsent(id, k -> new Bounds(region));
    }
    private void unlinkFromCell(GridCell cell, long id) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) { return; }                                        // not loaded

        if (!ids.remove(id)) { return; }                                    // wasn't linked

        if (this.cellsInRegionCount_.decrementAndGet(id) > 0) { return; }   // stop here if region is still on other loaded cells
        this.regionMap_.remove(id);                                         // keeping invariants
        this.boundsCache_.remove(id);
    }
    private void incrementHot(long id) {
        this.chunksInRegionCount_.incrementAndGet(id);
    }
    private void decrementHot(long id) {
        this.chunksInRegionCount_.decrementAndGet(id);
    }
    private boolean isHot(long id) {
        return !this.chunksInRegionCount_.isZero(id);
    }
    private int cellToChunkShift() {
        return this.gridSize_.shiftBy() - Constants.CHUNK_SHIFT;
    }
    private GridCell cellOfChunk(Chunk chunk) {
        return new GridCell(
                chunk.getX() >> cellToChunkShift(),
                chunk.getZ() >> cellToChunkShift(),
                chunk.getWorld()
        );
    }

    private void loadCell(GridCell cell) {
        if (this.cellRegionMap_.containsKey(cell)) { return; }  // already loaded
        this.cellRegionMap_.put(cell, new HashSet<>());         // loading

        double  minX = cell.x() << this.gridSize_.shiftBy(),
                minZ = cell.z() << this.gridSize_.shiftBy(),
                maxX = minX + this.gridSize_.getSize(),
                maxZ = minZ + this.gridSize_.getSize();

        List<Region> regions = this.delegate_.where()           // querying
                .lessThan(RegionField.MIN_X, maxX)
                .greaterThan(RegionField.MAX_X, minX)
                .lessThan(RegionField.MIN_Z, maxZ)
                .greaterThan(RegionField.MAX_Z, minZ)
                .equal(RegionField.WORLD, cell.world())
                .get();

        regions.forEach(r -> this.linkToCell(cell, r));         // linking
    }
    private void unloadCell(GridCell cell) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) { return; }                            // already unloaded

        this.chunksInCellCount_.clear(cell);                    // sanity
        for (long id : ids.toArray(Long[]::new)) {              // unlinking regions
            unlinkFromCell(cell, id);
        }

        this.cellRegionMap_.remove(cell);                       // unloading
    }
    private boolean isCellLoaded(GridCell cell) {
        return this.cellRegionMap_.containsKey(cell);
    }
    private Optional<Region> regionOfId(long id) {
        Region reg = this.regionMap_.get(id);
        if (reg != null) { return Optional.of(reg); }
        return this.delegate_.get(id);
    }
    private Stream<Region> regionsInCell(GridCell cell) {
        Set<Long> regs = this.cellRegionMap_.get(cell);
        if (regs == null) { return Stream.empty(); }
        return regs.stream()
                .map(this::regionOfId)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
    private Stream<Region> regionsInChunk(Chunk chunk) {
        GridCell cell = this.cellOfChunk(chunk);
        if (!this.isCellLoaded(cell)) { this.loadCell(cell); }
        Condition inChunk = Condition.inChunk(chunk);
        return this.regionsInCell(cell).filter(inChunk);
    }
    private GridCell cellAt(double x, double z, World world) {
        int size = this.gridSize_.getSize();
        int cellX = Math.floorDiv((int) Math.floor(x), size);
        int cellZ = Math.floorDiv((int) Math.floor(z), size);
        return new GridCell(cellX, cellZ, world);
    }
    private Grid cellsIn(double minX, double minZ, double maxX, double maxZ, World world) {
        return sizedGridIn(minX, minZ, maxX, maxZ, this.gridSize_.getSize(), world);
    }
    private Grid cellsIn(Bounds bounds, World world) {
        return cellsIn(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ(), world);
    }
    private Collection<Region> stageRegions(Condition condition) {
        Deque<Condition> stack = new ArrayDeque<>();
        stack.push(condition);
        List<Condition.Positional> positionals = new ArrayList<>();

        while (!stack.isEmpty()) {
            Condition current = stack.pop();
            if (current instanceof Condition.Composed cmp) {
                cmp.conditions().forEach(stack::push);
                continue;
            }
            if (current instanceof Condition.Positional pos) { positionals.add(pos); }

        }

        if (positionals.isEmpty()) { return regionMap_.values(); }

        Set<Region> out = new HashSet<>();
        for (Condition.Positional pos : positionals) switch (pos) {
            case Condition.At at -> {
                Location loc = at.point();
                out.addAll(this.regionsInCell(this.cellAt(loc.getX(), loc.getZ(), loc.getWorld())).toList());
            }
            case Condition.In in -> {
                Area area = in.area();
                this.cellsIn(area.getMinX(), area.getMinZ(), area.getMaxX(), area.getMaxZ(), area.getWorld())
                        .forEach(c -> out.addAll(this.regionsInCell(c).toList()));
            }
            case Condition.InChunk ic -> {
                GridCell cell = new GridCell(
                        ic.x() >> cellToChunkShift(),
                        ic.z() >> cellToChunkShift(),
                        ic.world()
                );
                out.addAll(this.regionsInCell(cell).toList());
            }
        }
        return out;
    }
    private void insert(Region region) {
        Bounds bounds = new Bounds(region);
        World world = region.getWorld();

        this.boundsCache_.put(region.getId(), bounds);
        for (GridCell cell : cellsIn(bounds, world)) {
            this.linkToCell(cell, region);
        }
        int chunks = (int) chunksIn(bounds, region.getWorld()).cellStream()
                .filter(HotRegionRepository::isChunkLoaded)
                .count();
        this.chunksInRegionCount_.incrementBy(region.getId(), chunks);
    }
    private void remove(Region region) {
        long id = region.getId();
        Bounds bounds = this.boundsCache_.get(id);
        if (bounds == null) { bounds = delegate_.get(id).map(Bounds::new).orElse(new Bounds(region)); }

        World world = region.getWorld();
        for (GridCell cell : cellsIn(bounds, world)) {
            this.unlinkFromCell(cell, id);
        }
        this.regionMap_.remove(id);
        this.boundsCache_.remove(id);
        this.chunksInRegionCount_.clear(id);
    }
    private void repair(@Nullable Bounds old, Region region) {
        if (old == null) { this.insert(region); return; }
        World world = region.getWorld();
        Bounds current = new Bounds(region);
        this.boundsCache_.put(region.getId(), current);

        Set<GridCell> oldCells = cellsIn(old, world).cellSet();
        cellsIn(current, world).cellStream()
                .filter(c -> !oldCells.remove(c) && isCellLoaded(c))
                .forEach(c -> this.linkToCell(c, region));

        oldCells.stream()
                .filter(this::isCellLoaded)
                .forEach(c -> this.unlinkFromCell(c, region.getId()));

        Set<GridCell> oldChunks = chunksIn(old, world).cellSet();
        int newChunksCount = chunksIn(current, world).cellStream()
                .filter(c -> !oldChunks.remove(c) && isChunkLoaded(c))
                .reduce(0, (i, c) -> ++i, Integer::sum);
        int oldChunksCount = oldChunks.stream()
                .filter(HotRegionRepository::isChunkLoaded)
                .reduce(0, (i, c) -> ++i, Integer::sum);

        this.chunksInRegionCount_.incrementBy(region.getId(), newChunksCount - oldChunksCount);
    }

    //HELPERS
    private static Grid sizedGridIn(double minX, double minZ, double maxX, double maxZ, int size, World world) {
        int minCellX = Math.floorDiv((int) Math.floor(minX), size);
        int minCellZ = Math.floorDiv((int) Math.floor(minZ), size);
        int maxCellX = Math.floorDiv((int) Math.floor(maxX), size);
        int maxCellZ = Math.floorDiv((int) Math.floor(maxZ), size);

        return new Grid(minCellX, minCellZ, maxCellX, maxCellZ, world);
    }
    private static Grid chunksIn(double minX, double minZ, double maxX, double maxZ, World world) {
        return sizedGridIn(minX, minZ, maxX, maxZ, Constants.CHUNK_SIZE, world);
    }
    private static Grid chunksIn(Bounds bounds, World world) {
        return chunksIn(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ(), world);
    }
    private static boolean isChunkLoaded(GridCell chunk) {
        return chunk.world().isChunkLoaded(chunk.x(), chunk.z());
    }
}
