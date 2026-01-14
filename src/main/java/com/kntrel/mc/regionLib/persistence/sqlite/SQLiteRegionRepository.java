package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.util.cache.ConcurrentRLUCache;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class SQLiteRegionRepository implements RegionRepository {

    // ASSETS
    private static final Gson GSON = new Gson();
    private record Patch<T>(List<T> inserts, List<T> updates, List<T> deletes) {

        Patch() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        void merge(Patch<? extends T> other) {
            this.inserts.addAll(other.inserts);
            this.updates.addAll(other.updates);
            this.deletes.addAll(other.deletes);
        }
        boolean isEmpty() {
            return this.inserts.isEmpty() && this.updates.isEmpty() && this.deletes.isEmpty();
        }
    }


    // FIELDS
    private final RegionContext context_;
    private final Server server_;
    private final DataBase dataBase_;
    private final QueryParser queryParser_;
    private final Map<Class<?>, String> relationalTableCache_;
    private final HierarchyRepository hierarchyRepository_;
    private final ConcurrentMap<Long, RegionSnapshot> snapshotCache_;
    private final ExecutorService writeExecutor_;
    private final AtomicLong idCount_;


    // CONSTRUCTORS
    SQLiteRegionRepository(
            Server server,
            RegionContext context,
            DataBase database,
            QueryParser queryParser,
            ExecutorService writesExecutor,
            HierarchyRepository hierarchyRepository,
            Supplier<? extends ConcurrentMap<Long, RegionSnapshot>> cacheFactory
    ) {                                                 //Testing constructor
        this.context_ = context;
        this.server_ = server;
        this.dataBase_ = database;
        this.queryParser_ = queryParser;
        this.relationalTableCache_ = new HashMap<>();
        this.hierarchyRepository_ = hierarchyRepository;
        this.snapshotCache_ = cacheFactory.get();
        this.writeExecutor_ = writesExecutor;
        this.idCount_ = new AtomicLong(queryNextId(this.dataBase_.getConnection()));
    }
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository, Supplier<? extends ConcurrentMap<Long, RegionSnapshot>> cacheFactory) {
        this(
                plugin.getServer(),
                context,
                new DataBase(DataBaseInitializer.getConnection(plugin, database)),
                new QueryParser(context),
                Executors.newSingleThreadExecutor(),
                hierarchyRepository,
                cacheFactory
        );
    }
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository) {
        this(
                plugin,
                context,
                database,
                hierarchyRepository,
                () -> new ConcurrentRLUCache<>(1024)
        );
    }



    @Override
    public List<Region> get(Query query) {
        List<RegionSnapshot> snapshots = this.getInner(query);
        snapshots.forEach(s -> this.snapshotCache_.put(s.region().id(), s));
        return snapshots.stream().map(this::buildRegion).toList();
    }

    @Override
    public void save(Region... regions) {
        this.writeExecutor_.execute(() -> this.saveInner(regions));
    }

    @Override
    public HierarchyRepository getHierarchyRepository() {
        return this.hierarchyRepository_;
    }


    //PRIVATE
    private <T> List<T> selectRelational(Collection<DTO.Region> regions, Class<T> dtoClass) throws SQLException {
        if (regions.isEmpty()) { return List.of(); }

        String tableName = this.relationalTableCache_.computeIfAbsent(dtoClass, cls -> {
            DTO.Table tableAnn = cls.getAnnotation(DTO.Table.class);
            return (tableAnn != null) ? tableAnn.value() : cls.getSimpleName().toLowerCase();
        });

        String sql = "SELECT "
                   + tableName
                   + ".* FROM "
                   + tableName
                   + " WHERE "
                   + tableName
                   + ".region_id IN ("
                   + String.join(", ", regions.stream().map(DTO.Region::id).map(String::valueOf).toList())
                   + ");";

        return this.dataBase_.query(sql, dtoClass);
    }
    private Region buildRegion(RegionSnapshot snapshot) {
        World world = this.server_.getWorld(snapshot.worldName());
        Hierarchy hierarchy = this.context_.getHierarchyRepository().get((long) snapshot.region().hierarchy()).orElseThrow();
        Region r = new Region(this.context_, snapshot.boundingBox(), world, snapshot.name(), hierarchy);

        r.setId(snapshot.region().id());

        if (!snapshot.region().enabled()) {
            r.enabled(false);
        }
        if (snapshot.region().destroyed()) {
            r.destroy();
        }

        for (DTO.Rule rule : snapshot.rules()) {
            r.setRuleValue(rule.key(), rule.value());
        }
        RegionDataContainer dc = r.getDataContainer();
        for (DTO.Data data : snapshot.data()) {
            dc.add(new RegionData(data.key(), GSON.fromJson(data.value(), JsonElement.class)));
        }
        for (DTO.Permission perm : snapshot.permissions()) {
            r.addPermission(UUID.fromString(perm.playerUUID()), perm.level());
        }

        return r;
    }
    private void saveInner(Region... regions) {
        Patch<Object> patch = new Patch<>();
        for (Region r : regions) {
            RegionSnapshot oldSnapshot = null;

            //Is a new region
            if (r.getId() == null) {
                r.setId(this.idCount_.getAndIncrement());
            } else {
                oldSnapshot = this.snapshotCache_.get(r.getId());
                if (oldSnapshot == null) {
                    List<RegionSnapshot> fetched = this.getInner(Query.builder(this).idIs(r.getId()).asQuery());
                    if (!fetched.isEmpty()) { oldSnapshot = fetched.getFirst(); }
                }
            }
            RegionSnapshot currentSnapshot = buildSnapshot(r);
            this.snapshotCache_.put(r.getId(), currentSnapshot);
            patch.merge(computePatch(oldSnapshot, currentSnapshot));
        }

        if (patch.isEmpty()) { return; }

        try {
            this.dataBase_.write(patch.inserts(), patch.updates(), patch.deletes());
        } catch (SQLException e) {
            throw new RegionSQLSaveException(regions[0], e);
        }
    }
    private List<RegionSnapshot> getInner(Query query) {
        String s1q = this.queryParser_.parse(query);

        List<DTO.Region> regs; List<DTO.Permission> perms; List<DTO.Rule> rules; List<DTO.Data> data;
        try {
            regs = this.dataBase_.query(s1q, DTO.Region.class);
            perms = this.selectRelational(regs, DTO.Permission.class);
            rules = this.selectRelational(regs, DTO.Rule.class);
            data = this.selectRelational(regs, DTO.Data.class);
        } catch (SQLException e) {
            throw new RegionSQLFetchException(s1q, e);
        }

        Map<Long, List<DTO.Permission>> permMap = perms.stream().collect(Collectors.groupingBy(DTO.Permission::regionId));
        Map<Long, List<DTO.Rule>> ruleMap = rules.stream().collect(Collectors.groupingBy(DTO.Rule::regionId));
        Map<Long, List<DTO.Data>> dataMap = data.stream().collect(Collectors.groupingBy(DTO.Data::regionId));
        return regs.stream().map(r -> new RegionSnapshot(
                r,
                permMap.getOrDefault(r.id(), List.of()),
                ruleMap.getOrDefault(r.id(), List.of()),
                dataMap.getOrDefault(r.id(), List.of())
        )).toList();
    }



    //HELPERS
    private static RegionSnapshot buildSnapshot(Region region) {
        DTO.Region dtoRegion = new DTO.Region(
                region.getId(),
                region.getName(),
                region.getWorld().getName(),
                region.isEnabled(),
                region.getHierarchy().getId().intValue(),
                region.getMinX(),
                region.getMinY(),
                region.getMinZ(),
                region.getMaxX(),
                region.getMaxY(),
                region.getMaxZ(),
                region.isDestroyed()
        );

        DTO.Permission[] permissions = region.getPermissions().stream()
                .map(perm -> new DTO.Permission(
                        region.getId(),
                        perm.getPlayerId().toString(),
                        perm.getGroup().getLevel()
                ))
                .toArray(DTO.Permission[]::new);

        DTO.Rule[] rules = region.getRuleValues().stream()
                .map(ruleValue -> new DTO.Rule(
                        region.getId(),
                        ruleValue.getRule().getName(),
                        ruleValue.toString()
                ))
                .toArray(DTO.Rule[]::new);

        RegionDataContainer dataContainer = region.getDataContainer();
        DTO.Data[] data = dataContainer.getAll().stream()
                .map(regionData -> new DTO.Data(
                        region.getId(),
                        regionData.getKey(),
                        GSON.toJson(regionData.getValue())
                ))
                .toArray(DTO.Data[]::new);

        return new RegionSnapshot(dtoRegion, permissions, rules, data);
    }
    private static long queryNextId(Connection conn) {
        String sql = "SELECT MAX(id) AS max_id FROM region;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("max_id") + 1;
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static Patch<?> computePatch(@Nullable RegionSnapshot old, @NotNull RegionSnapshot current) {
        List<Object> inserts = new ArrayList<>(), updates = new ArrayList<>(), deletes = new ArrayList<>();
        Patch<Object> out = new Patch<>(inserts, updates, deletes);
        if (old == null) {
            inserts.add(current.region());
            inserts.addAll(Arrays.asList(current.permissions()));
            inserts.addAll(Arrays.asList(current.rules()));
            inserts.addAll(Arrays.asList(current.data()));
            return out;
        }

        if (old.fingerPrint() == current.fingerPrint()) {
            // NO OP
            return out;
        }

        if (old.regionFingerprint() != current.regionFingerprint()) {
            updates.add(current.region());
        }
        if (old.permissionsFingerprint() != current.permissionsFingerprint()) {
            Patch<DTO.Permission> p = computePatch(old.permissions(), current.permissions(), DTO.Permission::playerUUID);
            out.merge(p);
        }
        if (old.rulesFingerprint() != current.rulesFingerprint()) {
            Patch<DTO.Rule> p = computePatch(old.rules(), current.rules(), DTO.Rule::key);
            out.merge(p);
        }
        if (old.dataFingerprint() != current.dataFingerprint()) {
            Patch<DTO.Data> p = computePatch(old.data(), current.data(), DTO.Data::key);
            out.merge(p);
        }

        return out;
    }

    private static <T, K> Patch<T> computePatch(T[] old, T[] current, Function<T, K> keyExtractor) {
        List<T> inserts = new ArrayList<>(), updates = new ArrayList<>(), deletes = new ArrayList<>();
        Patch<T> out = new Patch<>(inserts, updates, deletes);

        Map<K, T> oldMap = new HashMap<>(old.length * 2);
        for (T t : old) { oldMap.put(keyExtractor.apply(t), t); }

        for (T t : current) {
            K k = keyExtractor.apply(t);
            T o = oldMap.remove(k);
            if (o == null) { inserts.add(t); }
            else if (!o.equals(t)) { updates.add(t); }
        }
        deletes.addAll(oldMap.values());

        return out;
    }
}