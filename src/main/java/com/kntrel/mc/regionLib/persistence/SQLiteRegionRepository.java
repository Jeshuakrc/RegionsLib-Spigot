package com.kntrel.mc.regionLib.persistence;

import com.kntrel.mc.regionLib.region.Permission;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.RegionRepository;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.util.valueType.ValueHolder;
import com.kntrel.mc.regionLib.util.valueType.ValueType;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import java.lang.reflect.Field;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.StringJoiner;

public class SQLiteRegionRepository implements RegionRepository {

    // FIELDS
    private final Plugin plugin_;
    private final RegionContext context_;
    private final HierarchyRepository hierarchyRepository_;
    private final Connection connection_;


    // CONSTRUCTORS
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository) {
        this.plugin_ = plugin;
        this.context_ = context;
        this.hierarchyRepository_ = hierarchyRepository;
        this.connection_ = DataBaseInitializer.getConnection(plugin, database);
    }


    // IMPLEMENTATION
    @Override
    public List<Region> getAll() {
        return this.queryRegions("", stmt -> {})
                .sorted()
                .toList();
    }

    @Override
    public Optional<Region> get(Long id) {
        return this.queryRegions("WHERE id = ?", stmt -> set(stmt, 1, id)).findFirst();
    }

    @Override
    public List<Region> get(String name) {
        return this.queryRegions("WHERE name = ?", stmt -> set(stmt, 1, name))
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getAt(double x, double y, double z, World world) {
        return this.queryRegions(
                        "WHERE ? BETWEEN min_x AND max_x AND ? BETWEEN min_y AND max_y AND ? BETWEEN min_z AND max_z AND world = ?",
                        stmt -> {
                            set(stmt, 1, x);
                            set(stmt, 2, y);
                            set(stmt, 3, z);
                            set(stmt, 4, world.getName());
                        })
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getIn(World world) {
        return this.queryRegions("WHERE world = ?", stmt -> set(stmt, 1, world.getName()))
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        double  minX = Math.min(x1, x2), maxX = Math.max(x1, x2),
                minY = Math.min(y1, y2), maxY = Math.max(y1, y2),
                minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return this.queryRegions(
                        "WHERE min_x < ? AND min_y < ? AND min_z < ? AND max_x > ? AND max_y > ? AND max_z > ?",
                        stmt -> {
                            set(stmt, 1, maxX);
                            set(stmt, 2, maxY);
                            set(stmt, 3, maxZ);
                            set(stmt, 4, minX);
                            set(stmt, 5, minY);
                            set(stmt, 6, minZ);
                        })
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getInChunk(int x, int z, World world) {
        int minX = x << 4, minZ = z << 4;
        return this.getIn(minX, world.getMinHeight(), minZ, minX + 15, world.getMaxHeight(), minZ + 15, world);
    }

    @Override
    public void save(Region region) {
        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = this.connection_.getAutoCommit();
            this.connection_.setAutoCommit(false);

            if (region.getId() == null) {
                insertRegion(region);
            } else {
                updateRegion(region);
            }

            deleteAssociations(region.getId());
            insertRules(region);
            insertData(region);
            insertPermissions(region);

            this.connection_.commit();
        } catch (SQLException e) {
            try { this.connection_.rollback(); } catch (SQLException ignored) { }
            throw new RuntimeException(e);
        } finally {
            try { this.connection_.setAutoCommit(oldAutoCommit); } catch (SQLException ignored) { }
        }
    }

    @Override
    public HierarchyRepository getHierarchyRepository() {
        return this.hierarchyRepository_;
    }

    private void insertRegion(Region region) throws SQLException {
        String sql = "INSERT INTO region (name, world, enabled, hierarchy, min_x, min_y, min_z, max_x, max_y, max_z, destroyed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setRegionFields(region, stmt, false);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    region.setId(id);
                }
            }
        }
    }

    private void updateRegion(Region region) throws SQLException {
        String sql = "UPDATE region SET name = ?, world = ?, enabled = ?, hierarchy = ?, min_x = ?, min_y = ?, min_z = ?, max_x = ?, max_y = ?, max_z = ?, destroyed = ? WHERE id = ?";
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            setRegionFields(region, stmt, true);
            stmt.executeUpdate();
        }
    }

    private void setRegionFields(Region region, PreparedStatement stmt, boolean includeId) throws SQLException {
        set(stmt, 1, region.getName());
        set(stmt, 2, region.getWorld().getName());
        set(stmt, 3, region.isEnabled());
        set(stmt, 4, region.getHierarchy().getId());
        set(stmt, 5, region.getMinX());
        set(stmt, 6, region.getMinY());
        set(stmt, 7, region.getMinZ());
        set(stmt, 8, region.getMaxX());
        set(stmt, 9, region.getMaxY());
        set(stmt, 10, region.getMaxZ());
        set(stmt, 11, region.isDestroyed());
        if (includeId) { set(stmt, 12, region.getId()); }
    }

    private void deleteAssociations(Long regionId) throws SQLException {
        if (regionId == null) { return; }
        try (PreparedStatement rule = this.connection_.prepareStatement("DELETE FROM regionRule WHERE region_id = ?");
             PreparedStatement data = this.connection_.prepareStatement("DELETE FROM regionData WHERE region_id = ?");
             PreparedStatement perm = this.connection_.prepareStatement("DELETE FROM regionPermission WHERE region_id = ?")) {
            set(rule, 1, regionId);
            set(data, 1, regionId);
            set(perm, 1, regionId);
            rule.executeUpdate();
            data.executeUpdate();
            perm.executeUpdate();
        }
    }

    private void insertRules(Region region) throws SQLException {
        Map<String, ValueHolder<?>> rules = extractRuleValues(region);
        if (rules.isEmpty()) { return; }

        String sql = "INSERT INTO regionRule (region_id, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            for (Map.Entry<String, ValueHolder<?>> entry : rules.entrySet()) {
                String key = entry.getKey();
                ValueHolder<?> holder = entry.getValue();
                ValueType<?> type = this.context_.getRuleRegistry().get(key)
                        .map(Rule::getValueType)
                        .orElse(holder.getType());
                set(stmt, 1, region.getId());
                set(stmt, 2, key);
                set(stmt, 3, type.toString(holder.get()));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertData(Region region) throws SQLException {
        RegionDataContainer container = region.getDataContainer();
        if (container == null || container.isEmpty()) { return; }

        String sql = "INSERT INTO regionData (region_id, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            for (RegionData data : container.getAll()) {
                set(stmt, 1, region.getId());
                set(stmt, 2, data.getKey());
                set(stmt, 3, data.getValue().toString());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertPermissions(Region region) throws SQLException {
        List<Permission> permissions = region.getPermissions();
        if (permissions == null || permissions.isEmpty()) { return; }

        String sql = "INSERT INTO regionPermission (region_id, player_uuid, level) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            for (Permission perm : permissions) {
                set(stmt, 1, region.getId());
                set(stmt, 2, perm.getPlayerId());
                set(stmt, 3, perm.getGroup().getLevel());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private Stream<Region> queryRegions(String whereClause, Consumer<PreparedStatement> setter) {
        String sql = "SELECT id, name, world, enabled, destroyed, min_x, min_y, min_z, max_x, max_y, max_z, hierarchy FROM region " + whereClause;
        Map<Long, RegionRow> regions = new LinkedHashMap<>();

        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            setter.accept(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RegionRow row = new RegionRow(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("world"),
                            rs.getInt("enabled") != 0,
                            rs.getInt("destroyed") != 0,
                            rs.getDouble("min_x"),
                            rs.getDouble("min_y"),
                            rs.getDouble("min_z"),
                            rs.getDouble("max_x"),
                            rs.getDouble("max_y"),
                            rs.getDouble("max_z"),
                            rs.getLong("hierarchy")
                    );
                    regions.put(row.id(), row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (regions.isEmpty()) { return Stream.empty(); }

        List<Long> ids = new ArrayList<>(regions.keySet());
        Map<Long, List<RuleRow>> rules = loadRules(ids);
        Map<Long, List<DataRow>> data = loadData(ids);
        Map<Long, List<PermissionRow>> perms = loadPermissions(ids);

        return regions.values().stream().map(row -> toRegion(
                row,
                rules.getOrDefault(row.id(), List.of()),
                data.getOrDefault(row.id(), List.of()),
                perms.getOrDefault(row.id(), List.of())
        ));
    }

    private Map<Long, List<RuleRow>> loadRules(List<Long> ids) {
        if (ids.isEmpty()) { return Map.of(); }
        String sql = "SELECT region_id, key, value FROM regionRule WHERE region_id IN (" + placeholders(ids.size()) + ")";
        Map<Long, List<RuleRow>> grouped = new HashMap<>();
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            setIds(stmt, ids);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RuleRow row = new RuleRow(rs.getString("key"), rs.getString("value"));
                    long regionId = rs.getLong("region_id");
                    grouped.computeIfAbsent(regionId, __ -> new ArrayList<>()).add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return grouped;
    }

    private Map<Long, List<DataRow>> loadData(List<Long> ids) {
        if (ids.isEmpty()) { return Map.of(); }
        String sql = "SELECT region_id, key, value FROM regionData WHERE region_id IN (" + placeholders(ids.size()) + ")";
        Map<Long, List<DataRow>> grouped = new HashMap<>();
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            setIds(stmt, ids);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DataRow row = new DataRow(rs.getString("key"), rs.getString("value"));
                    long regionId = rs.getLong("region_id");
                    grouped.computeIfAbsent(regionId, __ -> new ArrayList<>()).add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return grouped;
    }

    private Map<Long, List<PermissionRow>> loadPermissions(List<Long> ids) {
        if (ids.isEmpty()) { return Map.of(); }
        String sql = "SELECT region_id, player_uuid, level FROM regionPermission WHERE region_id IN (" + placeholders(ids.size()) + ")";
        Map<Long, List<PermissionRow>> grouped = new HashMap<>();
        try (PreparedStatement stmt = this.connection_.prepareStatement(sql)) {
            setIds(stmt, ids);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PermissionRow row = new PermissionRow(rs.getString("player_uuid"), rs.getInt("level"));
                    long regionId = rs.getLong("region_id");
                    grouped.computeIfAbsent(regionId, __ -> new ArrayList<>()).add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return grouped;
    }

    private Region toRegion(RegionRow row, List<RuleRow> rules, List<DataRow> data, List<PermissionRow> permissions) {
        BoundingBox bb = new BoundingBox(row.minX(), row.minY(), row.minZ(), row.maxX(), row.maxY(), row.maxZ());
        World world = this.plugin_.getServer().getWorld(row.world());
        Optional<Hierarchy> hierarchy = this.hierarchyRepository_.get(row.hierarchyId());

        if (hierarchy.isEmpty()) {
            throw new RuntimeException("Hierarchy with ID " + row.hierarchyId() + " not found.");
        }

        Region reg = new Region(this.context_, bb, world, row.name(), hierarchy.get());
        reg.setId(row.id());
        reg.enabled(row.enabled());
        if (row.destroyed()) {
            reg.destroy();
        }

        permissions.forEach(p -> reg.addPermission(new Permission(UUID.fromString(p.playerUuid()), reg, p.level())));

        rules.forEach(rule -> {
            ValueType<?> type = this.context_.getRuleRegistry().get(rule.key())
                    .map(Rule::getValueType)
                    .orElse(ValueType.STRING);
            ValueHolder<?> holder = ValueHolder.of(rule.value(), type);
            reg.setRuleValue(rule.key(), holder.get());
        });

        RegionDataContainer container = new RegionDataContainer();
        data.stream()
                .map(d -> new RegionData(d.key(), d.value()))
                .forEach(container::add);
        reg.setDataContainer(container);

        return reg;
    }

    private void setIds(PreparedStatement stmt, List<Long> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            set(stmt, i + 1, ids.get(i));
        }
    }

    private String placeholders(int count) {
        if (count <= 0) { throw new IllegalArgumentException("count must be positive"); }
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < count; i++) { joiner.add("?"); }
        return joiner.toString();
    }

    private static Map<String, ValueHolder<?>> extractRuleValues(Region region) {
        try {
            Field field = Region.class.getDeclaredField("rulesValues_");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ValueHolder<?>> map = (Map<String, ValueHolder<?>>) field.get(region);
            return Map.copyOf(map);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Unable to extract rule values", e);
        }
    }

    private static void set(PreparedStatement stmt, int idx, Object value) {
        try {
            stmt.setObject(idx, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    //INNER RECORDS
    private record RegionRow(Long id, String name, String world, boolean enabled, boolean destroyed,
                             double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                             Long hierarchyId) {}

    private record RuleRow(String key, String value) {}

    private record DataRow(String key, String value) {}

    private record PermissionRow(String playerUuid, int level) {}
}
