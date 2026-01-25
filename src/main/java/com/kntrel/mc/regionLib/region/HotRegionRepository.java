package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.mc.regionLib.util.Grid;
import com.kntrel.util.HashCounter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Stream;

class HotRegionRepository implements RegionReadRepository, Listener {

    private record Bounds(double minX, double minZ, double maxX, double maxZ) {
        Bounds(Region region) {
            this(region.getMinX(), region.getMinZ(), region.getMaxX(), region.getMaxZ());
        }
    }


    //FIELDS
    private final RegionRepository delegate_;
    private final Grid.CellSize gridSize_;
    private final Map<Long, Region> regionMap_;
    private final Map<Grid.Cell, Set<Long>> cellRegionMap_;
    private final Map<Long, Bounds> boundsCache_;
    private final HashCounter<Grid.Cell> chunksInCellCount_;
    private final HashCounter<Long> chunksInRegionCount_;
    private final HashCounter<Long> cellsInRegionCount_;



    //CONSTRUCTOR
    public HotRegionRepository(RegionRepository delegate, Grid.CellSize gridSize) {
        this.delegate_ = delegate;
        this.gridSize_ = gridSize;
        this.regionMap_ = new HashMap<>();
        this.cellRegionMap_ = new HashMap<>();
        this.boundsCache_ = new HashMap<>();
        this.chunksInCellCount_ = new HashCounter<>();
        this.chunksInRegionCount_ = new HashCounter<>();
        this.cellsInRegionCount_ = new HashCounter<>();
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
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
    void save(Region... regions) {
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
    @EventHandler public void handleChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        this.regionsInChunk(chunk)
                .map(Region::getId)
                .forEach(this::incrementHot);
        this.chunksInCellCount_.increment(this.cellOfChunk(chunk));
    }
    @EventHandler public void handleChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Grid.Cell cell = this.cellOfChunk(chunk);
        if (isCellLoaded(cell)) {
            this.regionsInChunk(chunk)
                    .map(Region::getId)
                    .forEach(this::decrementHot);
        }

        if (this.chunksInCellCount_.decrementAndCheckZero(cell)) { this.unloadCell(cell); }
    }


    //GETTERS
    public List<Region> getWarmRegions() {
        return List.copyOf(this.regionMap_.values());
    }


    //PRIVATE
    private void linkToCell(Grid.Cell cell, Region region) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) { return; }                    // not loaded

        long id = region.getId();
        if (!ids.add(id)) { return; }                   // already linked

        this.cellsInRegionCount_.increment(id);         // keeping invariants
        this.regionMap_.put(id, region);
        this.boundsCache_.computeIfAbsent(id, k -> new Bounds(region));
    }
    private void unlinkFromCell(Grid.Cell cell, long id) {
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
    private Grid.Cell cellOfChunk(Chunk chunk) {
        return new Grid.Cell(
                chunk.getX() >> cellToChunkShift(),
                chunk.getZ() >> cellToChunkShift(),
                chunk.getWorld()
        );
    }

    private void loadCell(Grid.Cell cell) {
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
    private void unloadCell(Grid.Cell cell) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) { return; }                            // already unloaded

        this.chunksInCellCount_.clear(cell);                    // sanity
        for (long id : ids.toArray(Long[]::new)) {              // unlinking regions
            unlinkFromCell(cell, id);
        }

        this.cellRegionMap_.remove(cell);                       // unloading
    }
    private boolean isCellLoaded(Grid.Cell cell) {
        return this.cellRegionMap_.containsKey(cell);
    }
    private Optional<Region> regionOfId(long id) {
        Region reg = this.regionMap_.get(id);
        if (reg != null) { return Optional.of(reg); }
        return this.delegate_.get(id);
    }
    private Stream<Region> regionsInCell(Grid.Cell cell) {
        Set<Long> regs = this.cellRegionMap_.get(cell);
        if (regs == null) { return Stream.empty(); }
        return regs.stream()
                .map(this::regionOfId)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
    private Stream<Region> regionsInChunk(Chunk chunk) {
        Grid.Cell cell = this.cellOfChunk(chunk);
        if (!this.isCellLoaded(cell)) { this.loadCell(cell); }
        Condition inChunk = Condition.inChunk(chunk);
        return this.regionsInCell(cell).filter(inChunk);
    }
    private Grid.Cell cellAt(double x, double z, World world) {
        int size = this.gridSize_.getSize();
        int cellX = Math.floorDiv((int) Math.floor(x), size);
        int cellZ = Math.floorDiv((int) Math.floor(z), size);
        return new Grid.Cell(cellX, cellZ, world);
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
                Grid.Cell cell = new Grid.Cell(
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
        for (Grid.Cell cell : cellsIn(bounds, world)) {
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
        for (Grid.Cell cell : cellsIn(bounds, world)) {
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

        Set<Grid.Cell> oldCells = cellsIn(old, world).cellSet();
        cellsIn(current, world).cellStream()
                .filter(c -> !oldCells.remove(c) && isCellLoaded(c))
                .forEach(c -> this.linkToCell(c, region));

        oldCells.stream()
                .filter(this::isCellLoaded)
                .forEach(c -> this.unlinkFromCell(c, region.getId()));

        Set<Grid.Cell> oldChunks = chunksIn(old, world).cellSet();
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
    private static boolean isChunkLoaded(Grid.Cell chunk) {
        return chunk.world().isChunkLoaded(chunk.x(), chunk.z());
    }
}
