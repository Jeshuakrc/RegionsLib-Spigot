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
import io.ebean.Database;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class SQLiteRegionRepository implements RegionRepository {

    // FIELDS
    private final RegionMapper regionMapper_;
    private final Database db_;


    // CONSTRUCTORS
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository) {
        this.db_ = DataBaseInitializer.getDatabase(plugin, database);
        this.regionMapper_ = new RegionMapper(plugin, context, hierarchyRepository, new RuleMapper(context), new RegionDataMapper());
    }


    // IMPLEMENTATION
    @Override
    public List<Region> getAll() {
        return this.db_.find(RegionDTO.class)
                .findStream()
                .map(this.regionMapper_::toModel)
                .toList();
    }

    @Override
    public Optional<Region> get(Long id) {
        return Optional.ofNullable(this.db_.find(RegionDTO.class, id)).map(this.regionMapper_::toModel);
    }

    @Override
    public List<Region> get(String name) {
        return this.db_.find(RegionDTO.class).where().eq("name", name).findList().stream().map(this.regionMapper_::toModel).toList();
    }

    @Override
    public List<Region> getAt(double x, double y, double z, World world) {
        return this.db_.find(RegionDTO.class)
                .where()
                .raw("? BETWEEN minX AND maxX", x)
                .raw("? BETWEEN minY AND maxY", y)
                .raw("? BETWEEN minZ AND maxZ", z)
                .eq("world", world.getName())
                .findList()
                .stream()
                .map(this.regionMapper_::toModel)
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getIn(World world) {
        return this.db_.find(RegionDTO.class)
                .where()
                .eq("world", world.getName())
                .findList()
                .stream()
                .map(this.regionMapper_::toModel)
                .sorted()
                .toList();
    }

    @Override
    public List<Region> getIn(double x1, double y1, double z1, double x2, double y2, double z2, World world) {
        double  minX = Math.min(x1, x2), maxX = Math.max(x1, x2),
                minY = Math.min(y1, y2), maxY = Math.max(y1, y2),
                minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return this.db_.find(RegionDTO.class)
                .where()
                .lt("minX", maxX)
                .lt("minY", maxY)
                .lt("minZ", maxZ)
                .gt("maxX", minX)
                .gt("maxY", minY)
                .gt("maxZ", minZ)
                .findList()
                .stream()
                .map(this.regionMapper_::toModel)
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
        RegionDTO dto = this.regionMapper_.toEntity(region);
        if (region.getId() == null) {
            this.db_.save(dto);
        } else {
            this.db_.update(dto);
        }
    }

    @Override
    public HierarchyRepository getHierarchyRepository() {
        return this.regionMapper_.hierarchyRepository_;
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