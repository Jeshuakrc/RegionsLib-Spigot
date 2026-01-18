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
import java.util.*;
import java.util.stream.Stream;

class HotRegionRepository implements RegionRepository, Listener {

    //CONSTANTS
    private static final int CHUNK_SHIFT = 4; // 16x16 chunks

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


    //FIELDS
    private final RegionRepository delegate_;
    private final GridSize gridSize_;
    private final Map<Long, Region> regionMap_;
    private final Map<GridCell, Set<Long>> cellRegionMap_;
    private final HashCounter<GridCell> chunksInCellCount_;
    private final HashCounter<Long> chunksInRegionCount_;
    private final HashCounter<Long> cellsInRegionCount_;



    //CONSTRUCTOR
    public HotRegionRepository(RegionRepository delegate, GridSize gridSize) {
        this.delegate_ = delegate;
        this.gridSize_ = gridSize;
        this.regionMap_ = new HashMap<>();
        this.cellRegionMap_ = new HashMap<>();
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
                .filter(r -> this.chunksInRegionCount_.contains(r.getId()))
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
    public void save(Region... region) {
        this.delegate_.save(region);
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        this.regionsInChunk(chunk)
                .map(Region::getId)
                .forEach(this.chunksInRegionCount_::incrementAndGet);
        this.chunksInCellCount_.incrementAndGet(this.cellOfChunk(chunk));
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        GridCell cell = this.cellOfChunk(chunk);
        if (isCellLoaded(cell)) {
            this.regionsInChunk(chunk)
                    .map(Region::getId)
                    .forEach(this.chunksInRegionCount_::decrementAndGet);
        }

        if (this.chunksInCellCount_.decrementAndGet(cell) < 1) { this.unloadCell(cell); }
    }


    //PRIVATE
    private GridCell cellOfChunk(Chunk chunk) {
        return new GridCell(
                chunk.getX() >> (this.gridSize_.shiftBy() - CHUNK_SHIFT),
                chunk.getZ() >> (this.gridSize_.shiftBy() - CHUNK_SHIFT),
                chunk.getWorld()
        );
    }

    private void loadCell(GridCell cell) {
        if (this.cellRegionMap_.containsKey(cell)) { return; }      // Already loaded

        double  minX = cell.x() << this.gridSize_.shiftBy(),
                minZ = cell.z() << this.gridSize_.shiftBy(),
                maxX = minX + this.gridSize_.getSize(),
                maxZ = minZ + this.gridSize_.getSize();

        List<Region> regions = this.delegate_.where()
                .lessThan(RegionField.MIN_X, maxX)
                .greaterThan(RegionField.MAX_X, minX)
                .lessThan(RegionField.MIN_Z, maxZ)
                .greaterThan(RegionField.MAX_Z, minZ)
                .equal(RegionField.WORLD, cell.world())
                .get();

        Set<Long> ids = new HashSet<>();
        regions.forEach(r -> {
            this.regionMap_.put(r.getId(), r);
            ids.add(r.getId());
            this.cellsInRegionCount_.incrementAndGet(r.getId());
        });
        this.cellRegionMap_.put(cell, ids);
    }
    private void unloadCell(GridCell cell) {
        this.chunksInCellCount_.clear(cell);
        Set<Long> regIds = this.cellRegionMap_.remove(cell);
        if (regIds == null) { return; }

        for (long id : regIds) {
            if (this.cellsInRegionCount_.decrementAndGet(id) > 0) { continue; }
            this.regionMap_.remove(id);
            this.cellsInRegionCount_.clear(id);
        }
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
    private List<GridCell> cellsIn(double minX, double minZ, double maxX, double maxZ, World world) {
        List<GridCell> cells = new ArrayList<>();
        int size = this.gridSize_.getSize();

        int minCellX = Math.floorDiv((int) Math.floor(minX), size);
        int minCellZ = Math.floorDiv((int) Math.floor(minZ), size);
        int maxCellX = Math.floorDiv((int) Math.floor(maxX), size);
        int maxCellZ = Math.floorDiv((int) Math.floor(maxZ), size);

        for (int x = minCellX; x <= maxCellX; x++) {
            for (int z = minCellZ; z <= maxCellZ; z++) {
                cells.add(new GridCell(x, z, world));
            }
        }

        return cells;
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
                int shift = this.gridSize_.shiftBy();
                GridCell cell = new GridCell(
                        ic.x() >> (shift - CHUNK_SHIFT),
                        ic.z() >> (shift - CHUNK_SHIFT),
                        ic.world()
                );
                out.addAll(this.regionsInCell(cell).toList());
            }
        }
        return out;
    }
}
