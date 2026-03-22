package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.Constants;
import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

class HotRegionRepository implements RegionReadRepository, Listener {

    //CONSTANTS
    private static final Logger LOGGER = LoggerFactory.getLogger(HotRegionRepository.class);

    private record AliasSync(Region alias, Region canonical) {}


    //FIELDS
    private final RegionRepository delegate_;
    private final RegionIdentityRegistry identityRegistry_;
    private final Grid.CellSize gridSize_;
    private final RegionCache cache_;
    private final Map<Long, Region> regionMap_;
    private final Map<Grid.Cell, Set<Long>> cellRegionMap_;
    private final HashCounter<Grid.Cell> chunksInCellCount_;
    private final HashCounter<Long> chunksInRegionCount_;
    private final HashCounter<Long> cellsInRegionCount_;
    private final List<BiConsumer<Region, Chunk>> loadConsumers_, unloadConsumers_;


    //CONSTRUCTOR
    public HotRegionRepository(RegionCache cache, RegionIdentityRegistry identityRegistry, RegionRepository delegate, Grid.CellSize gridSize) {
        this.delegate_ = delegate;
        this.identityRegistry_ = identityRegistry;
        this.gridSize_ = gridSize;
        this.cache_ = cache;
        this.regionMap_ = new HashMap<>();
        this.cellRegionMap_ = new HashMap<>();
        this.chunksInCellCount_ = new HashCounter<>();
        this.chunksInRegionCount_ = new HashCounter<>();
        this.cellsInRegionCount_ = new HashCounter<>();
        this.loadConsumers_ = new ArrayList<>();
        this.unloadConsumers_ = new ArrayList<>();
        LOGGER.debug("Initialized HotRegionRepository (gridSize={})", this.gridSize_.getSize());
    }
    public HotRegionRepository(RegionCache cache, RegionRepository delegate, Grid.CellSize gridSize) {
        this(cache, new RegionIdentityRegistry(), delegate, gridSize);
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        LOGGER.trace("Resolving query (includesDestroyed={}, limit={}, hasOrdering={}).",
                query.includesDestroyed(), query.getLimit(), query.getOrdering().isPresent());
        if (query.includesDestroyed()) {
            LOGGER.trace("Query includes destroyed regions. Delegating directly.");
            return this.delegate_.get(query).stream().map(this::canonicalize).toList();
        }

        Condition condition = query.getCondition();
        List<Region> staged = this.stageRegions(condition).stream().filter(r -> this.isHot(r.getId())).toList();
        LOGGER.trace("Staged {} hot region(s) before condition filters.", staged.size());

        Stream<Region> out = staged.stream().filter(condition);

        Query.Ordering ordering = query.getOrdering().orElse(null);
        if (ordering != null) {
            LOGGER.trace("Applying ordering by {} ascending={}", ordering.field(), ordering.ascending());
            final RegionField<? extends Comparable<?>> field = ordering.field();
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Comparator<Region> cmp = Comparator.comparing(r -> (Comparable) field.extract(r));
            if (!ordering.ascending()) {
                cmp = cmp.reversed();
            }
            out = out.sorted(cmp);
        }

        if (query.getLimit() > 0) {
            LOGGER.trace("Applying query limit: {}.", query.getLimit());
            out = out.limit(query.getLimit());
        }

        List<Region> result = out.toList();
        LOGGER.trace("Resolved query with {} region(s).", result.size());
        return result;
    }
    void save(Region... regions) {
        LOGGER.debug("Saving {} region(s).", regions.length);

        List<Region> prepared = new ArrayList<>(regions.length);
        List<Region> toInsert = new ArrayList<>(regions.length);
        List<AliasSync> aliases = new ArrayList<>();
        Set<Long> seenExisting = new HashSet<>();
        Set<Region> seenNew = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Region input : regions) {
            Region region = input;
            Long id = input.getId();
            if (id != null) {
                region = this.identityRegistry_.canonicalize(input);
                if (region != input) {
                    RegionStateSync.mergeIntoCanonical(region, input);
                    aliases.add(new AliasSync(input, region));
                }
                if (!seenExisting.add(region.getId())) {
                    continue;
                }
            } else if (!seenNew.add(region)) {
                continue;
            }

            prepared.add(region);

            if (region.getId() == null) {
                toInsert.add(region);
                continue;
            }
            if (region.isDestroyed()) {
                LOGGER.trace("Region {} is destroyed. Removing from hot repository.", region.getId());
                this.remove(region);
                continue;
            }

            RegionSnapshot previous = this.persistedStateOf(region);
            if (previous == null) {
                LOGGER.warn("Region {} had no previous snapshot. Rebuilding links via remove/insert.", region.getId());
                this.remove(region);
                this.insert(region);
                continue;
            }
            this.repair(previous, region);
        }

        if (prepared.isEmpty()) {
            return;
        }

        this.delegate_.save(prepared.toArray(Region[]::new));
        LOGGER.trace("Delegate save completed for {} region(s).", prepared.size());

        for (Region region : toInsert) {
            if (region.getId() == null) {
                throw new RuntimeException("Delegate RegionRepository didn't assign and ID to a fresh region.");
            }
            this.identityRegistry_.canonicalize(region);
            this.insert(region);
        }

        Map<Long, RegionSnapshot> persisted = new HashMap<>(prepared.size() * 2);
        for (Region region : prepared) {
            if (region.getId() == null) {
                throw new RuntimeException("Saved region did not retain its ID.");
            }
            this.regionMap_.computeIfPresent(region.getId(), (l, b) -> region);
            RegionSnapshot snapshot = new RegionSnapshot(region);
            region.rememberState(snapshot);
            this.cache_.put(snapshot);
            persisted.put(region.getId(), snapshot);
        }

        for (AliasSync alias : aliases) {
            Long id = alias.canonical().getId();
            RegionSnapshot snapshot = (id == null) ? null : persisted.get(id);
            if (snapshot == null) {
                continue;
            }
            RegionStateSync.overwrite(alias.alias(), alias.canonical());
            alias.alias().rememberState(snapshot);
        }

        LOGGER.debug("Save flow completed (insertedFresh={}, trackedWarm={}).", toInsert.size(), this.regionMap_.size());
    }
    @EventHandler public void handleChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        LOGGER.trace("Handling chunk load ({}, {}, world={}).", chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        this.chunksInCellCount_.increment(this.cellOfChunk(chunk));
        List<Region> loaded = new ArrayList<>();
        for (Region region : this.regionsInChunk(chunk)) {
            this.cacheRememberedState(region);
            int previousCount = this.incrementHot(region.getId());
            if (previousCount < 1) {
                loaded.add(region);
            }
            LOGGER.debug("Chunk load touched region {} (previousHotCount={}).", region.getId(), previousCount);
        }
        this.loadConsumers_.forEach(c -> loaded.forEach(r -> c.accept(r, chunk)));
        LOGGER.trace("Chunk load completed with {} newly hot region(s).", loaded.size());
    }
    @EventHandler public void handleChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        LOGGER.debug("Handling chunk unload ({}, {}, world={}).", chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        Grid.Cell cell = this.cellOfChunk(chunk);
        if (isCellLoaded(cell)) for (Region r : this.regionsInChunk(chunk)) {
            if (this.decrementHot(r.getId())) {
                LOGGER.debug("Region {} is no longer hot after unloading chunk.", r.getId());
                this.unloadConsumers_.forEach(c -> c.accept(r, chunk));
            }
        }

        if (this.chunksInCellCount_.decrementAndCheckZero(cell)) {
            LOGGER.debug("Chunk unload emptied cell {}. Unloading cell state.", cell);
            this.unloadCell(cell);
        }
    }


    //GETTERS
    public List<Region> getWarmRegions() {
        return List.copyOf(this.regionMap_.values());
    }


    //API
    public void onLoadedRegion(BiConsumer<Region, Chunk> action) {
        this.loadConsumers_.add(action);
        LOGGER.debug("Registered onLoadedRegion consumer (total={}).", this.loadConsumers_.size());
    }
    public void onUnloadedRegion(BiConsumer<Region, Chunk> action) {
        this.unloadConsumers_.add(action);
        LOGGER.debug("Registered onUnloadedRegion consumer (total={}).", this.unloadConsumers_.size());
    }


    //PRIVATE
    private void linkToCell(Grid.Cell cell, Region region) {
        region = this.canonicalize(region);
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) {
            LOGGER.trace("Skipping link; cell {} is not loaded.", cell);
            return;
        }

        long id = region.getId();
        if (!ids.add(id)) {
            LOGGER.trace("Skipping link; region {} is already linked to cell {}.", id, cell);
            return;
        }

        this.cellsInRegionCount_.increment(id);
        boolean fresh = this.regionMap_.put(id, region) == null;
        LOGGER.trace("Linked region {} to cell {} (freshWarmRegion={}).", id, cell, fresh);
    }
    private void unlinkFromCell(Grid.Cell cell, long id) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) {
            LOGGER.trace("Skipping unlink; cell {} is not loaded.", cell);
            return;
        }

        if (!ids.remove(id)) {
            LOGGER.trace("Skipping unlink; region {} is not linked to cell {}.", id, cell);
            return;
        }

        int remainingCells = this.cellsInRegionCount_.decrementAndGet(id);
        if (remainingCells > 0) {
            LOGGER.trace("Unlinked region {} from cell {} (remainingCells={}).", id, cell, remainingCells);
            return;
        }
        this.regionMap_.remove(id);
        this.cache_.evict(id);
        LOGGER.trace("Unlinked region {} from last loaded cell {}. Evicted warm/cache entries.", id, cell);
    }
    private int incrementHot(long id) {
        return this.chunksInRegionCount_.getAndIncrement(id);
    }
    private boolean decrementHot(long id) {
        return this.chunksInRegionCount_.decrementAndCheckEmptied(id);
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
        if (this.cellRegionMap_.containsKey(cell)) {
            LOGGER.trace("Skipping cell load; {} already loaded.", cell);
            return;
        }

        this.cellRegionMap_.put(cell, new HashSet<>());

        double minX = cell.x() << this.gridSize_.shiftBy(),
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

        regions.forEach(region -> this.linkToCell(cell, region));
        LOGGER.trace("Loaded cell {} with {} candidate region(s).", cell, regions.size());
    }
    private void unloadCell(Grid.Cell cell) {
        Set<Long> ids = this.cellRegionMap_.get(cell);
        if (ids == null) {
            LOGGER.trace("Skipping cell unload; {} already unloaded.", cell);
            return;
        }
        LOGGER.debug("Unloading cell {} with {} linked region id(s).", cell, ids.size());

        this.chunksInCellCount_.clear(cell);
        for (long id : ids.toArray(Long[]::new)) {
            this.unlinkFromCell(cell, id);
        }

        this.cellRegionMap_.remove(cell);
    }
    private boolean isCellLoaded(Grid.Cell cell) {
        return this.cellRegionMap_.containsKey(cell);
    }
    private Optional<Region> regionOfId(long id) {
        Region region = this.regionMap_.get(id);
        if (region != null) {
            return Optional.of(region);
        }
        region = this.identityRegistry_.get(id).orElse(null);
        if (region != null) {
            return Optional.of(region);
        }
        return this.delegate_.get(id).map(this::canonicalize);
    }
    private Stream<Region> regionsInCell(Grid.Cell cell) {
        Set<Long> regs = this.cellRegionMap_.get(cell);
        if (regs == null) {
            return Stream.empty();
        }
        return regs.stream()
                .map(this::regionOfId)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
    private List<Region> regionsInChunk(Chunk chunk) {
        Grid.Cell cell = this.cellOfChunk(chunk);
        LOGGER.trace("Resolving regions in chunk ({}, {}, world={}) via cell {}.",
                chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), cell);
        if (!this.isCellLoaded(cell)) {
            this.loadCell(cell);
        }
        Condition inChunk = Condition.inChunk(chunk);
        List<Region> regions = this.regionsInCell(cell).filter(inChunk).toList();
        LOGGER.trace("Resolved {} region(s) in chunk ({}, {}, world={}).",
                regions.size(), chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
        return regions;
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
    private Grid cellsIn(RegionSnapshot bounds, World world) {
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
            if (current instanceof Condition.Positional pos) {
                positionals.add(pos);
            }
        }

        if (positionals.isEmpty()) {
            LOGGER.trace("No positional filters detected. Using all warm regions (count={}).", this.regionMap_.size());
            return this.regionMap_.values();
        }

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
        LOGGER.trace("Staged {} region(s) from {} positional condition(s).", out.size(), positionals.size());
        return out;
    }
    private void insert(Region region) {
        RegionSnapshot bounds = new RegionSnapshot(region);
        World world = region.getWorld();
        Grid grid = cellsIn(bounds, world);
        for (Grid.Cell cell : grid) {
            this.linkToCell(cell, region);
        }
        int chunks = (int) chunksIn(bounds, region.getWorld()).cellStream()
                .filter(HotRegionRepository::isChunkLoaded)
                .count();
        this.chunksInRegionCount_.incrementBy(region.getId(), chunks);
        LOGGER.debug("Inserted region {} (loadedChunkCount={}).", region.getId(), chunks);
    }
    private void remove(Region region) {
        long id = region.getId();
        LOGGER.debug("Removing region {} from hot repository.", id);
        RegionSnapshot bounds = this.persistedStateOf(region);
        if (bounds == null) {
            bounds = new RegionSnapshot(region);
        }

        World world = region.getWorld();
        for (Grid.Cell cell : cellsIn(bounds, world)) {
            this.unlinkFromCell(cell, id);
        }
        this.regionMap_.remove(id);
        this.cache_.evict(id);
        this.chunksInRegionCount_.clear(id);
        LOGGER.debug("Removed region {} from hot repository state.", id);
    }
    private void repair(@Nullable RegionSnapshot old, Region region) {
        if (old == null) {
            LOGGER.debug("Repair called with null snapshot for region {}. Falling back to insert.", region.getId());
            this.insert(region);
            return;
        }
        LOGGER.debug("Repairing region {} links and hot counters.", region.getId());
        World world = region.getWorld();
        RegionSnapshot current = new RegionSnapshot(region);

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
        LOGGER.trace("Repaired region {} (newLoadedChunks={}, oldLoadedChunks={}, delta={}).",
                region.getId(), newChunksCount, oldChunksCount, newChunksCount - oldChunksCount);
    }
    private Region canonicalize(Region region) {
        return this.identityRegistry_.canonicalize(region);
    }
    private RegionSnapshot persistedStateOf(Region region) {
        Long id = region.getId();
        if (id == null) {
            return null;
        }

        RegionSnapshot snapshot = region.getRememberedState().orElse(null);
        if (snapshot != null) {
            this.cache_.put(snapshot);
            return snapshot;
        }

        snapshot = this.cache_.get(id).orElse(null);
        if (snapshot != null) {
            return snapshot;
        }

        Region loaded = this.delegate_.get(id).orElse(null);
        if (loaded == null) {
            return null;
        }

        snapshot = loaded.getRememberedState().orElseGet(() -> new RegionSnapshot(loaded));
        this.cache_.put(snapshot);
        return snapshot;
    }
    private void cacheRememberedState(Region region) {
        RegionSnapshot snapshot = region.getRememberedState().orElseGet(() -> new RegionSnapshot(region));
        this.cache_.put(snapshot);
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
    private static Grid chunksIn(RegionSnapshot bounds, World world) {
        return chunksIn(bounds.minX(), bounds.minZ(), bounds.maxX(), bounds.maxZ(), world);
    }
    private static boolean isChunkLoaded(Grid.Cell chunk) {
        return chunk.world().isChunkLoaded(chunk.x(), chunk.z());
    }
}
