package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.event.RegionCreateEvent;
import com.kntrel.mc.regionLib.event.RegionUpdatedEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.repository.AttributedRegionRepository;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MainRegionRepository implements AttributedRegionRepository {

    private static final int LOAD_BY_ID_BATCH_SIZE = 250;

    private record AliasSync(Region alias, Region canonical) {}
    private record SavePreparation(List<Region> regions, List<AliasSync> aliases) {}


    //FIELDS
    private final RegionRepository saveDelegate_;
    private final RegionReadRepository readDelegate_;
    private final PluginManager pluginManager_;
    private final RegionCache cache_;
    private final RegionIdentityRegistry identityRegistry_;


    //CONSTRUCTORS
    public MainRegionRepository(
            RegionRepository saveDelegate,
            RegionReadRepository readDelegate,
            PluginManager pluginManager,
            RegionCache cache,
            RegionIdentityRegistry identityRegistry
    ) {
        this.saveDelegate_ = saveDelegate;
        this.readDelegate_ = readDelegate;
        this.pluginManager_ = pluginManager;
        this.cache_ = cache;
        this.identityRegistry_ = identityRegistry;
    }
    public MainRegionRepository(RegionRepository delegate, PluginManager pluginManager, RegionCache cache) {
        this(delegate, delegate, pluginManager, cache, new RegionIdentityRegistry());
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        List<Long> ids = this.readDelegate_.get(query, RegionField.ID);
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Region> resolved = new HashMap<>(ids.size() * 2);
        List<Long> missing = new ArrayList<>();
        for (Long id : ids) {
            Region region = this.identityRegistry_.get(id).orElse(null);
            if (region == null) {
                missing.add(id);
                continue;
            }

            resolved.put(id, region);
            this.cacheRememberedState(region);
        }

        for (Region loaded : this.loadByIds(missing)) {
            Region canonical = this.identityRegistry_.canonicalize(loaded);
            resolved.put(canonical.getId(), canonical);
            this.cacheRememberedState(canonical);
        }

        List<Region> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Region region = resolved.get(id);
            if (region == null) {
                region = this.identityRegistry_.get(id).orElse(null);
            }
            if (region != null) {
                out.add(region);
            }
        }
        return out;
    }
    @Override public List<Object[]> get(Query query, RegionField<?>... fields) {
        if (fields.length < 1) {
            throw new IllegalArgumentException("Projected region queries must request at least one field.");
        }

        return this.get(query).stream()
                .map(region -> {
                    Object[] row = new Object[fields.length];
                    for (int i = 0; i < fields.length; i++) {
                        row[i] = fields[i].extract(region);
                    }
                    return row;
                })
                .toList();
    }
    @Override public void save(@Nullable Entity doer, Region... regions) {
        SavePreparation preparation = this.prepare(regions);
        Region[] inspected = preparation.regions().stream()
                .filter(region -> this.inspect(doer, region))
                .toArray(Region[]::new);
        if (inspected.length < 1) {
            return;
        }

        this.saveDelegate_.save(inspected);

        Map<Long, RegionSnapshot> persisted = new HashMap<>(inspected.length * 2);
        for (Region region : inspected) {
            Region canonical = this.identityRegistry_.canonicalize(region);
            RegionSnapshot snapshot = new RegionSnapshot(canonical);
            canonical.rememberState(snapshot);
            this.cache_.put(snapshot);
            persisted.put(canonical.getId(), snapshot);
        }

        for (AliasSync alias : preparation.aliases()) {
            Long id = alias.canonical().getId();
            RegionSnapshot snapshot = (id == null) ? null : persisted.get(id);
            if (snapshot == null) {
                continue;
            }
            RegionStateSync.overwrite(alias.alias(), alias.canonical());
            alias.alias().rememberState(snapshot);
        }
    }


    //PRIVATE
    private boolean inspect(@Nullable Entity doer, Region region) {
        if (region.getId() == null) {
            RegionCreateEvent event = new RegionCreateEvent(region, doer);
            this.pluginManager_.callEvent(event);
            return !event.isCancelled();
        }

        RegionSnapshot old = this.persistedStateOf(region);
        if (old == null) {
            return true;
        }

        RegionSnapshot curr = new RegionSnapshot(region);
        RegionUpdatedEvent event = new RegionUpdatedEvent(region, old, curr, doer);
        this.pluginManager_.callEvent(event);
        return !event.isCancelled();
    }
    private SavePreparation prepare(Region[] regions) {
        List<Region> prepared = new ArrayList<>(regions.length);
        List<AliasSync> aliases = new ArrayList<>();
        Set<Long> seenExisting = new HashSet<>();
        Set<Region> seenNew = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Region region : regions) {
            Region canonical = region;
            Long id = region.getId();
            if (id != null) {
                canonical = this.identityRegistry_.canonicalize(region);
                if (canonical != region) {
                    RegionStateSync.mergeIntoCanonical(canonical, region);
                    aliases.add(new AliasSync(region, canonical));
                }
                if (!seenExisting.add(canonical.getId())) {
                    continue;
                }
            } else if (!seenNew.add(canonical)) {
                continue;
            }

            prepared.add(canonical);
        }

        return new SavePreparation(prepared, aliases);
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

        Region loaded = this.readDelegate_.get(id).orElse(null);
        if (loaded == null) {
            return null;
        }

        snapshot = loaded.getRememberedState().orElseGet(() -> new RegionSnapshot(loaded));
        this.cache_.put(snapshot);
        return snapshot;
    }
    private List<Region> loadByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Region> out = new ArrayList<>(ids.size());
        for (int start = 0; start < ids.size(); start += LOAD_BY_ID_BATCH_SIZE) {
            int end = Math.min(start + LOAD_BY_ID_BATCH_SIZE, ids.size());
            out.addAll(this.readDelegate_.get(new Query(
                    Condition.isIn(RegionField.ID, ids.subList(start, end)),
                    true
            )));
        }
        return out;
    }
    private void cacheRememberedState(Region region) {
        RegionSnapshot snapshot = region.getRememberedState().orElseGet(() -> new RegionSnapshot(region));
        this.cache_.put(snapshot);
    }
}
