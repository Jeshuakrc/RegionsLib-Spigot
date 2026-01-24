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
import com.kntrel.util.cache.ConcurrentRLUCache;
import com.kntrel.util.tuple.Pair;
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
    private static final int SQLITE_STMT_MAX_LENGTH = 1_000_000;
    private static final int SQLITE_QUERY_MAX_DEPTH = 1000;
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
    private record IdGrouping(List<Pair<Long, Long>> ranges, List<Long> singles) {}


    // FIELDS
    private final RegionContext context_;
    private final DataBase dataBase_;
    private final QueryParser queryParser_;
    private final Map<Class<?>, String> relationalTableCache_;
    private final ConcurrentMap<Long, RegionSnapshot> snapshotCache_;
    private final ExecutorService writeExecutor_;
    private final AtomicLong idCount_;


    // CONSTRUCTORS
    SQLiteRegionRepository(
            RegionContext context,
            DataBase database,
            QueryParser queryParser,
            ExecutorService writesExecutor,
            Supplier<? extends ConcurrentMap<Long, RegionSnapshot>> cacheFactory
    ) {                                                 //Testing constructor
        this.context_ = context;
        this.dataBase_ = database;
        this.queryParser_ = queryParser;
        this.relationalTableCache_ = new HashMap<>();
        this.snapshotCache_ = cacheFactory.get();
        this.writeExecutor_ = writesExecutor;
        this.idCount_ = new AtomicLong(queryNextId(this.dataBase_.getConnection()));
    }
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, Supplier<? extends ConcurrentMap<Long, RegionSnapshot>> cacheFactory) {
        this(
                context,
                new DataBase(DataBaseInitializer.getConnection(plugin, database)),
                new QueryParser(context),
                Executors.newSingleThreadExecutor(),
                cacheFactory
        );
    }
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database) {
        this(
                plugin,
                context,
                database,
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
        Set<Long> newRegions = new HashSet<>();
        for (Region r : regions) {
            if (r.getId() != null) { continue; }
            long id = this.idCount_.getAndIncrement();
            r.setId(id);
            newRegions.add(id);
        }
        this.writeExecutor_.execute(() -> this.saveInner(regions, newRegions));
    }


    //PRIVATE
    private <T> List<T> selectRelational(Collection<DTO.Region> regions, Class<T> dtoClass) {
        if (regions.isEmpty()) { return List.of(); }

        String tableName = this.relationalTableCache_.computeIfAbsent(dtoClass, cls -> {
            DTO.Table tableAnn = cls.getAnnotation(DTO.Table.class);
            return (tableAnn != null) ? tableAnn.value() : cls.getSimpleName().toLowerCase();
        });

        List<String> wheres = buildRelationalWheres(
                tableName,
                regions.stream().map(DTO.Region::id).collect(Collectors.toSet())
        );

        List<T> out = new ArrayList<>();
        for (String where : wheres) {
            String sql = "SELECT "
                       + tableName
                       + ".* FROM "
                       + tableName
                       + " WHERE "
                       + where
                       + ";";
            try {
                List<T> partial = this.dataBase_.query(sql, dtoClass);
                out.addAll(partial);
            } catch (SQLException e) {
                throw new RegionSQLFetchException(sql, e);
            }
        }
        return out;
    }
    private Region buildRegion(RegionSnapshot snapshot) {
        World world = this.context_.getServer().getWorld(snapshot.worldName());
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
    private void saveInner(Region[] regions, Set<Long> newRegions) {
        Patch<Object> patch = new Patch<>();
        for (Region r : regions) {
            RegionSnapshot oldSnapshot = null;

            //Is a new region
            if (r.getId() == null) {
                r.setId(this.idCount_.getAndIncrement());
            } else if (!newRegions.contains(r.getId())) {
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

        List<DTO.Region> regs;
        try {
            regs = this.dataBase_.query(s1q, DTO.Region.class);
        } catch (SQLException e) {
            throw new RegionSQLFetchException(s1q, e);
        }
        List<DTO.Permission> perms = this.selectRelational(regs, DTO.Permission.class);
        List<DTO.Rule> rules = this.selectRelational(regs, DTO.Rule.class);
        List<DTO.Data> data = this.selectRelational(regs, DTO.Data.class);

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
    private static IdGrouping groupIds(Collection<Long> ids) {
        List<Long> sorted = ids.stream().sorted().toList();
        List<Pair<Long, Long>> ranges = new ArrayList<>();
        List<Long> singles = new ArrayList<>();

        if (sorted.isEmpty()) {
            return new IdGrouping(ranges, singles);
        }

        Iterator<Long> i = sorted.iterator();
        long rangeStart = i.next();
        long previous = rangeStart;

        while (i.hasNext()) {
            long current = i.next();
            if (current != previous + 1) {
                if (rangeStart == previous) {
                    singles.add(rangeStart);
                } else {
                    ranges.add(Pair.of(rangeStart, previous));
                }
                rangeStart = current;
            }
            previous = current;
        }

        if (rangeStart == previous) {
            singles.add(rangeStart);
        } else {
            ranges.add(Pair.of(rangeStart, previous));
        }

        return new IdGrouping(ranges, singles);
    }
    private static List<String> buildRelationalWheres(String tableName, Collection<Long> ids) {
        int threshold = SQLITE_STMT_MAX_LENGTH - 100; // buffer for the rest of the statement

        IdGrouping grouping = groupIds(ids);
        List<String> out = new ArrayList<>();
        TrackedStringJoiner joiner = new TrackedStringJoiner(") OR (", "(", ")");

        for (Pair<Long, Long> range : grouping.ranges()) {
            String clause = tableName + ".region_id BETWEEN " + range.first() + " AND " + range.second();
            int preLen = joiner.predictedLength(clause);
            if (preLen >= threshold || joiner.count() >= SQLITE_QUERY_MAX_DEPTH) {
                out.add(joiner.toString());
                joiner.clear();
            }
            joiner.add(clause);
        }

        TrackedStringJoiner inJoiner = new TrackedStringJoiner(", ", tableName + ".region_id IN (", ")");

        for (Long single : grouping.singles()) {
            String id = String.valueOf(single);
            int preInLen = inJoiner.predictedLength(id);
            int preLen = joiner.predictedLength(preInLen);
            if (preLen >= threshold || joiner.count() >= SQLITE_QUERY_MAX_DEPTH) {
                if (inJoiner.count() > 0) {
                    joiner.add(inJoiner.toString());
                    inJoiner.clear();
                }
                out.add(joiner.toString());
                joiner.clear();
            }
            inJoiner.add(id);
        }

        if (inJoiner.count() > 0) {
            joiner.add(inJoiner.toString());
        }

        if (joiner.count() > 0) {
            out.add(joiner.toString());
        }

        return out;
    }
    private static class TrackedStringJoiner {

        private final String delimiter_, prefix_, suffix_;
        private StringJoiner joiner_;
        private int count_;

        TrackedStringJoiner(String delimiter, String prefix, String suffix) {
            this.joiner_ = new StringJoiner(delimiter, prefix, suffix);
            this.delimiter_ = delimiter;
            this.prefix_ = prefix;
            this.suffix_ = suffix;
            this.count_ = 0;
        }
        TrackedStringJoiner(String delimiter) {
            this(delimiter, "", "");
        }

        void add(String element) {
            this.joiner_.add(element);
            this.count_++;
        }
        int count() {
            return this.count_;
        }
        void clear() {
            this.joiner_ = new StringJoiner(this.delimiter_, this.prefix_, this.suffix_);
            this.count_ = 0;
        }
        int predictedLength(int elementLength) {
            if (this.count_ < 1) {
                return this.joiner_.length() + elementLength;
            }
            return this.joiner_.length() + this.delimiter_.length() + elementLength;
        }
        int predictedLength(String element) {
            return predictedLength(element.length());
        }
        @Override public String toString() {
            return this.joiner_.toString();
        }
    }
}