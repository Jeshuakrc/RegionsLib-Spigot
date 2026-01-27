package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.util.tuple.Pair;
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
import java.util.stream.Collectors;


public class SQLiteRegionRepository implements RegionRepository {

    // ASSETS
    private static final int SQLITE_STMT_MAX_LENGTH = 1_000_000;
    private static final int SQLITE_QUERY_MAX_DEPTH = 1000;

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
    private final ExecutorService writeExecutor_;
    private final AtomicLong idCount_;
    private final RegionCache cache_;


    // CONSTRUCTORS
    SQLiteRegionRepository(
            RegionContext context,
            DataBase database,
            QueryParser queryParser,
            ExecutorService writesExecutor
    ) {                                                 //Testing constructor
        this.context_ = context;
        this.dataBase_ = database;
        this.queryParser_ = queryParser;
        this.relationalTableCache_ = new HashMap<>();
        this.writeExecutor_ = writesExecutor;
        this.idCount_ = new AtomicLong(queryNextId(this.dataBase_.getConnection()));
        this.cache_ = this.context_.getCache();
    }
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database) {
        this(
                context,
                new DataBase(DataBaseInitializer.getConnection(plugin, database)),
                new QueryParser(context),
                Executors.newSingleThreadExecutor()
        );
    }



    @Override
    public List<Region> get(Query query) {
        List<RegionSnapshot> snapshots = this.getInner(query);
        return snapshots.stream().map(s -> s.toRegion(this.context_)).toList();
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
    private void saveInner(Region[] regions, Set<Long> newRegions) {
        Patch<Object> patch = new Patch<>();
        for (Region r : regions) {
            RegionSnapshot oldSnapshot = null;

            //Is a new region
            if (r.getId() == null) {
                r.setId(this.idCount_.getAndIncrement());
            } else if (!newRegions.contains(r.getId())) {
                oldSnapshot = this.cache_.get(r.getId()).orElse(null);
                if (oldSnapshot == null) {
                    List<RegionSnapshot> fetched = this.getInner(Query.builder(this).idIs(r.getId()).asQuery());
                    if (!fetched.isEmpty()) { oldSnapshot = fetched.getFirst(); }
                }
            }
            RegionSnapshot currentSnapshot = new RegionSnapshot(r);
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
        return regs.stream().map(r -> buildSnapshot(
                r,
                permMap.getOrDefault(r.id(), List.of()),
                ruleMap.getOrDefault(r.id(), List.of()),
                dataMap.getOrDefault(r.id(), List.of())
        )).toList();
    }



    //HELPERS
    static RegionSnapshot buildSnapshot(DTO.Region region, Collection<DTO.Permission> permissions, Collection<DTO.Rule> rules, Collection<DTO.Data> data) {
        List<RegionSnapshot.Permission> permsSnap = permissions.stream().map(p -> new RegionSnapshot.Permission(
                UUID.fromString(p.playerUUID()),
                p.level()
        )).toList();
        List<RegionSnapshot.Entry> rulesSnap = rules.stream().map(r -> new RegionSnapshot.Entry(
                r.key(),
                r.value()
        )).toList();
        List<RegionSnapshot.Entry> dataSnap = data.stream().map(d -> new RegionSnapshot.Entry(
                d.key(),
                d.value()
        )).toList();

        return new RegionSnapshot(
                region.id(),
                region.name(),
                UUID.fromString(region.world()),
                region.enabled(),
                region.hierarchy(),
                region.minX(),
                region.minY(),
                region.minZ(),
                region.maxX(),
                region.maxY(),
                region.maxZ(),
                region.destroyed(),
                permsSnap,
                rulesSnap,
                dataSnap
        );
    }
    static DTO.Region toRegionDTO(RegionSnapshot src) {
        return new DTO.Region(
                src.id(),
                src.name(),
                src.world().toString(),
                src.enabled(),
                src.hierarchy(),
                src.minX(),
                src.minY(),
                src.minZ(),
                src.maxX(),
                src.maxY(),
                src.maxZ(),
                src.destroyed()
        );
    }
    static List<DTO.Permission> toPermDTOs(Iterable<RegionSnapshot.Permission> src, long regionId) {
        List<DTO.Permission> out = new ArrayList<>();
        for (RegionSnapshot.Permission p : src) {
            out.add(new DTO.Permission(
                    regionId,
                    p.playerUUID().toString(),
                    p.level()
            ));
        }
        return out;
    }
    static List<DTO.Rule> toRuleDTOs(Iterable<RegionSnapshot.Entry> src, long regionId) {
        List<DTO.Rule> out = new ArrayList<>();
        for (RegionSnapshot.Entry r : src) {
            out.add(new DTO.Rule(
                    regionId,
                    r.key(),
                    r.value()
            ));
        }
        return out;
    }
    static List<DTO.Data> toDataDTOs(Iterable<RegionSnapshot.Entry> src, long regionId) {
        List<DTO.Data> out = new ArrayList<>();
        for (RegionSnapshot.Entry d : src) {
            out.add(new DTO.Data(
                    regionId,
                    d.key(),
                    d.value()
            ));
        }
        return out;
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
        long id = current.id();
        Patch<Object> out = new Patch<>(inserts, updates, deletes);
        if (old == null) {
            inserts.add(toRegionDTO(current));
            inserts.addAll(toPermDTOs(current.permissions(), id));
            inserts.addAll(toRuleDTOs(current.rules(), id));
            inserts.addAll(toDataDTOs(current.data(), id));
            return out;
        }

        if (old.fingerPrint() == current.fingerPrint()) {
            // NO OP
            return out;
        }

        if (old.regionFingerprint() != current.regionFingerprint()) {
            updates.add(toRegionDTO(current));
        }
        if (old.permissionsFingerprint() != current.permissionsFingerprint()) {
            List<DTO.Permission> o = toPermDTOs(old.permissions(), id), n = toPermDTOs(current.permissions(), id);
            Patch<DTO.Permission> p = computePatch(o, n, DTO.Permission::playerUUID);
            out.merge(p);
        }
        if (old.rulesFingerprint() != current.rulesFingerprint()) {
            List<DTO.Rule> o = toRuleDTOs(old.rules(), id), n = toRuleDTOs(current.rules(), id);
            Patch<DTO.Rule> p = computePatch(o, n, DTO.Rule::key);
            out.merge(p);
        }
        if (old.dataFingerprint() != current.dataFingerprint()) {
            List<DTO.Data> o = toDataDTOs(old.data(), id), n = toDataDTOs(current.data(), id);
            Patch<DTO.Data> p = computePatch(o, n, DTO.Data::key);
            out.merge(p);
        }

        return out;
    }

    private static <T, K> Patch<T> computePatch(Collection<T> old, Collection<T> current, Function<T, K> keyExtractor) {
        List<T> inserts = new ArrayList<>(), updates = new ArrayList<>(), deletes = new ArrayList<>();
        Patch<T> out = new Patch<>(inserts, updates, deletes);

        Map<K, T> oldMap = new HashMap<>(old.size() * 2);
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