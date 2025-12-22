package com.kntrel.mc.regionLib.persistence;

import com.kntrel.mc.regionLib.persistence.jpa.dto.PermissionDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RegionDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RegionDataDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RuleValueDTO;
import com.kntrel.mc.regionLib.persistence.jpa.ext.IdHolder;
import com.kntrel.mc.regionLib.persistence.jpa.ext.idHolderPermission;
import com.kntrel.mc.regionLib.persistence.jpa.mapper.JpaEntityMapper;
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
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;


public class SQLiteRegionRepository implements RegionRepository {

    // FIELDS
    private final RegionMapper regionMapper_;
    private final Connection conn_;


    // CONSTRUCTORS
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository) {
        this.conn_ = DataBaseInitializer.getConnection(plugin, database);
        this.regionMapper_ = new RegionMapper(plugin, context, hierarchyRepository, new RuleMapper(context), new RegionDataMapper());
    }


    // IMPLEMENTATION
    @Override
    public List<Region> getAll() {
        return this.fetchRegions("SELECT * FROM region", stmt -> {});
    }

    @Override
    public Optional<Region> get(Long id) {
        List<Region> regions = this.fetchRegions("SELECT * FROM region WHERE id = ?", stmt -> stmt.setLong(1, id));
        return regions.stream().findFirst();
    }

    @Override
    public List<Region> get(String name) {
        return this.fetchRegions("SELECT * FROM region WHERE name = ?", stmt -> stmt.setString(1, name));
    }

    @Override
    public List<Region> getAt(double x, double y, double z, World world) {
        return this.fetchRegions(
                "SELECT * FROM region WHERE ? BETWEEN min_x AND max_x AND ? BETWEEN min_y AND max_y AND ? BETWEEN min_z AND max_z AND world = ?",
                stmt -> {
                    stmt.setDouble(1, x);
                    stmt.setDouble(2, y);
                    stmt.setDouble(3, z);
                    stmt.setString(4, world.getName());
                }
        );
    }

    @Override
    public List<Region> getIn(World world) {
        return this.fetchRegions("SELECT * FROM region WHERE world = ?", stmt -> stmt.setString(1, world.getName()));
    }

    @Override
    public List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        double  minX = Math.min(x1, x2), maxX = Math.max(x1, x2),
                minY = Math.min(y1, y2), maxY = Math.max(y1, y2),
                minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return this.fetchRegions(
                "SELECT * FROM region WHERE min_x < ? AND min_y < ? AND min_z < ? AND max_x > ? AND max_y > ? AND max_z > ?",
                stmt -> {
                    stmt.setDouble(1, maxX);
                    stmt.setDouble(2, maxY);
                    stmt.setDouble(3, maxZ);
                    stmt.setDouble(4, minX);
                    stmt.setDouble(5, minY);
                    stmt.setDouble(6, minZ);
                }
        );
    }

    @Override
    public List<Region> getInChunk(int x, int z, World world) {
        int minX = x << 4, minZ = z << 4;
        return this.getIn(minX, world.getMinHeight(), minZ, minX + 15, world.getMaxHeight(), minZ + 15, world);
    }

    @Override
    public void save(Region region) {
        try {
            this.conn_.setAutoCommit(false);

            Long regionId = region.getId();
            if (regionId == null) {
                regionId = this.insertRegion(region);
                region.setId(regionId);
            } else {
                this.updateRegion(region);
                this.clearRegionChildren(regionId);
            }

            this.insertRules(regionId, this.getRuleValues(region));
            this.insertData(regionId, region.getDataContainer().getAll());
            this.insertPermissions(regionId, region.getPermissions());

            this.conn_.commit();
        } catch (SQLException e) {
            try { this.conn_.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Failed to save region", e);
        } finally {
            try { this.conn_.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public HierarchyRepository getHierarchyRepository() {
        return this.regionMapper_.hierarchyRepository_;
    }

    private List<Region> fetchRegions(String sql, Consumer<PreparedStatement> binder) {
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            binder.accept(stmt);

            List<RegionDTO> regions = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RegionDTO dto = new RegionDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setName(rs.getString("name"));
                    dto.setWorld(rs.getString("world"));
                    dto.setEnabled(rs.getBoolean("enabled"));
                    dto.setDestroyed(rs.getBoolean("destroyed"));
                    dto.setMinX(rs.getDouble("min_x"));
                    dto.setMinY(rs.getDouble("min_y"));
                    dto.setMinZ(rs.getDouble("min_z"));
                    dto.setMaxX(rs.getDouble("max_x"));
                    dto.setMaxY(rs.getDouble("max_y"));
                    dto.setMaxZ(rs.getDouble("max_z"));
                    dto.setHierarchyId(rs.getLong("hierarchy"));
                    regions.add(dto);
                }
            }

            this.loadRules(regions);
            this.loadData(regions);
            this.loadPermissions(regions);

            return regions.stream().map(this.regionMapper_::toModel).sorted().toList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query regions", e);
        }
    }

    private void loadRules(List<RegionDTO> regions) throws SQLException {
        if (regions.isEmpty()) { return; }

        Map<Long, List<RuleValueDTO>> ruleMap = new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(regions.size(), "?"));
        String sql = "SELECT id, region_id, key, value FROM regionRule WHERE region_id IN (" + placeholders + ")";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (int i = 0; i < regions.size(); i++) {
                stmt.setLong(i + 1, regions.get(i).getId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RuleValueDTO dto = new RuleValueDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setKey(rs.getString("key"));
                    dto.setValue(rs.getString("value"));
                    long regionId = rs.getLong("region_id");
                    ruleMap.computeIfAbsent(regionId, k -> new ArrayList<>()).add(dto);
                }
            }
        }

        regions.forEach(r -> r.setRules(ruleMap.getOrDefault(r.getId(), List.of())));
    }

    private void loadData(List<RegionDTO> regions) throws SQLException {
        if (regions.isEmpty()) { return; }

        Map<Long, List<RegionDataDTO>> dataMap = new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(regions.size(), "?"));
        String sql = "SELECT id, region_id, key, value FROM regionData WHERE region_id IN (" + placeholders + ")";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (int i = 0; i < regions.size(); i++) {
                stmt.setLong(i + 1, regions.get(i).getId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RegionDataDTO dto = new RegionDataDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setKey(rs.getString("key"));
                    dto.setValue(rs.getString("value"));
                    long regionId = rs.getLong("region_id");
                    dataMap.computeIfAbsent(regionId, k -> new ArrayList<>()).add(dto);
                }
            }
        }

        regions.forEach(r -> r.setDataContainer(dataMap.getOrDefault(r.getId(), List.of())));
    }

    private void loadPermissions(List<RegionDTO> regions) throws SQLException {
        if (regions.isEmpty()) { return; }

        Map<Long, List<PermissionDTO>> perms = new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(regions.size(), "?"));
        String sql = "SELECT id, region_id, player_uuid, level FROM regionPermission WHERE region_id IN (" + placeholders + ")";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (int i = 0; i < regions.size(); i++) {
                stmt.setLong(i + 1, regions.get(i).getId());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PermissionDTO dto = new PermissionDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setPlayerUUID(rs.getString("player_uuid"));
                    dto.setLevel(rs.getInt("level"));
                    long regionId = rs.getLong("region_id");
                    perms.computeIfAbsent(regionId, k -> new ArrayList<>()).add(dto);
                }
            }
        }

        regions.forEach(r -> r.setPermissions(perms.getOrDefault(r.getId(), List.of())));
    }

    private Long insertRegion(Region region) throws SQLException {
        String sql = "INSERT INTO region (name, world, enabled, hierarchy, min_x, min_y, min_z, max_x, max_y, max_z, destroyed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            this.bindRegion(stmt, region);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) { return keys.getLong(1); }
            }
        }
        throw new SQLException("Failed to retrieve generated region id");
    }

    private void updateRegion(Region region) throws SQLException {
        String sql = "UPDATE region SET name = ?, world = ?, enabled = ?, hierarchy = ?, min_x = ?, min_y = ?, min_z = ?, max_x = ?, max_y = ?, max_z = ?, destroyed = ? WHERE id = ?";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            this.bindRegion(stmt, region);
            stmt.setLong(12, region.getId());
            stmt.executeUpdate();
        }
    }

    private void bindRegion(PreparedStatement stmt, Region region) throws SQLException {
        stmt.setString(1, region.getName());
        stmt.setString(2, region.getWorld().getName());
        stmt.setBoolean(3, region.isEnabled());
        stmt.setLong(4, region.getHierarchy().getId());
        stmt.setDouble(5, region.getMinX());
        stmt.setDouble(6, region.getMinY());
        stmt.setDouble(7, region.getMinZ());
        stmt.setDouble(8, region.getMaxX());
        stmt.setDouble(9, region.getMaxY());
        stmt.setDouble(10, region.getMaxZ());
        stmt.setBoolean(11, region.isDestroyed());
    }

    private void clearRegionChildren(Long regionId) throws SQLException {
        for (String table : List.of("regionRule", "regionData", "regionPermission")) {
            try (PreparedStatement stmt = this.conn_.prepareStatement("DELETE FROM " + table + " WHERE region_id = ?")) {
                stmt.setLong(1, regionId);
                stmt.executeUpdate();
            }
        }
    }

    private Map<String, ValueHolder<?>> getRuleValues(Region region) {
        try {
            var field = Region.class.getDeclaredField("rulesValues_");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ValueHolder<?>> rules = (Map<String, ValueHolder<?>>) field.get(region);
            return rules;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access region rule values", e);
        }
    }

    private void insertRules(Long regionId, Map<String, ValueHolder<?>> rules) throws SQLException {
        if (rules.isEmpty()) { return; }

        String sql = "INSERT INTO regionRule (region_id, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (Map.Entry<String, ValueHolder<?>> entry : rules.entrySet()) {
                stmt.setLong(1, regionId);
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue().toString());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertData(Long regionId, List<RegionData> data) throws SQLException {
        if (data.isEmpty()) { return; }

        String sql = "INSERT INTO regionData (region_id, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (RegionData datum : data) {
                stmt.setLong(1, regionId);
                stmt.setString(2, datum.getKey());
                stmt.setString(3, datum.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void insertPermissions(Long regionId, List<Permission> permissions) throws SQLException {
        if (permissions.isEmpty()) { return; }

        String sql = "INSERT INTO regionPermission (region_id, player_uuid, level) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql)) {
            for (Permission permission : permissions) {
                stmt.setLong(1, regionId);
                stmt.setString(2, permission.getPlayerId().toString());
                stmt.setInt(3, permission.getGroup().getLevel());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }


    //INNER CLASSES
    private static class RegionMapper implements JpaEntityMapper<Region, RegionDTO> {

        //FIELDS
        private final Plugin plugin_;
        private final RegionContext context_;
        private final HierarchyRepository hierarchyRepository_;
        private final JpaEntityMapper<ValueHolder<?>, RuleValueDTO> ruleMapper_;
        private final JpaEntityMapper<RegionData, RegionDataDTO> dataMapper_;


        //CONSTRUCTORS
        public RegionMapper(
                Plugin plugin,
                RegionContext context,
                HierarchyRepository hierarchyRepository,
                JpaEntityMapper<ValueHolder<?>, RuleValueDTO> ruleMapper,
                JpaEntityMapper<RegionData, RegionDataDTO> dataMapper
        ) {
            this.plugin_ = plugin;
            this.context_ = context;
            this.hierarchyRepository_ = hierarchyRepository;
            this.ruleMapper_ = ruleMapper;
            this.dataMapper_ = dataMapper;
        }

        @Override public Class<Region> getModelClass() {
            return Region.class;
        }

        @Override public Class<RegionDTO> getEntityClass() {
            return RegionDTO.class;
        }

        @Override public Region toModel(RegionDTO src) {
            BoundingBox bb = new BoundingBox(src.getMinX(), src.getMinY(), src.getMinZ(), src.getMaxX(), src.getMaxY(), src.getMaxZ());
            World world = this.plugin_.getServer().getWorld(src.getWorld());
            Optional<Hierarchy> hierarchy = this.hierarchyRepository_.get(src.getHierarchyId());

            if (hierarchy.isEmpty()) {
                throw new RuntimeException("Hierarchy with ID " + src.getHierarchyId() + " not found.");
            }

            Region reg = new Region(this.context_, bb, world, src.getName(), hierarchy.get());
            reg.setId(src.getId());
            reg.enabled(src.isEnabled());
            if (src.isDestroyed()) {
                reg.destroy();
            }

            src.getPermissions().forEach(p -> reg.addPermission(new idHolderPermission(p.getId(), UUID.fromString(p.getPlayerUUID()), reg, p.getLevel())));

            src.getRules().stream()
                    .map(this.ruleMapper_::toModel)
                    .forEach(v -> reg.setRuleValue(v.toString(), v.get()));

            RegionDataContainer container = new RegionDataContainer();
            src.getDataContainer().stream()
                    .map(this.dataMapper_::toModel)
                    .forEach(container::add);
            reg.setDataContainer(container);

            return reg;
        }

        @Override public RegionDTO toEntity(Region src) {
            RegionDTO r = new RegionDTO();

            r.setId(src.getId());
            r.setName(src.getName());
            r.setWorld(src.getWorld().getName());
            r.setEnabled(src.isEnabled());
            r.setDestroyed(src.isDestroyed());
            r.setMinX(src.getMinX());
            r.setMinY(src.getMinY());
            r.setMinZ(src.getMinZ());
            r.setMaxX(src.getMaxX());
            r.setMaxY(src.getMaxY());
            r.setMaxZ(src.getMaxZ());
            r.setHierarchyId(src.getHierarchy().getId());

            r.setRules(
                    src.getRuleValues().stream().map(this.ruleMapper_::toEntity).toList()
            );
            r.setDataContainer(
                    src.getDataContainer().getAll().stream().map(this.dataMapper_::toEntity).toList()
            );
            r.setPermissions(
                    src.getPermissions().stream().map(p -> {
                        PermissionDTO dto = new PermissionDTO();
                        dto.setRegion(r);
                        dto.setPlayerUUID(p.getPlayerId().toString());
                        dto.setLevel(p.getGroup().getLevel());
                        if (p instanceof IdHolder h) { dto.setId(h.getId()); }
                        return dto;
                    }).toList()
            );

            return r;
        }
    }

    private static class RuleMapper implements JpaEntityMapper<ValueHolder<?>, RuleValueDTO> {

        private final RegionContext ctx_;

        private RuleMapper(RegionContext ctx) {
            this.ctx_ = ctx;
        }

        @Override @SuppressWarnings("unchecked")
        public Class<ValueHolder<?>> getModelClass() {
            return (Class<ValueHolder<?>>) (Class<?>) ValueHolder.class;
        }

        @Override
        public Class<RuleValueDTO> getEntityClass() {
            return RuleValueDTO.class;
        }

        @Override @SuppressWarnings({ "unchecked" , "rawtypes"})
        public ValueHolder<?> toModel(RuleValueDTO src) {
            ValueType<?> type = this.ctx_.getRuleRegistry().get(src.getKey())
                    .map(Rule::getValueType)
                    .orElse((ValueType) ValueType.STRING);

            return ValueHolder.of(src.getValue(), type);
        }
    }

    private static class RegionDataMapper implements JpaEntityMapper<RegionData, RegionDataDTO> {

        @Override
        public Class<RegionData> getModelClass() {
            return RegionData.class;
        }

        @Override
        public Class<RegionDataDTO> getEntityClass() {
            return RegionDataDTO.class;
        }

        @Override
        public RegionData toModel(RegionDataDTO src) {
            return new RegionData(src.getKey(), src.getValue());
        }
    }
}