package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.persistence.RegionSQLFetchException;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class SQLiteRegionRepository implements RegionRepository {

    // ASSETS
    private static final Gson GSON = new Gson();


    // FIELDS
    private final RegionContext context_;
    private Server server_;
    private final DataBase dataBase_;
    private final QueryParser queryParser_;
    private final Map<Class<?>, String> relationalTableCache_;


    // CONSTRUCTORS
    public SQLiteRegionRepository(Plugin plugin, RegionContext context, URI database, HierarchyRepository hierarchyRepository) {
        Connection conn = DataBaseInitializer.getConnection(plugin, database);
        this.context_ = context;
        this.server_ = plugin.getServer();
        this.dataBase_ = new DataBase(conn);
        this.queryParser_ = new QueryParser(context);
        this.relationalTableCache_ = new HashMap<>();
    }


    @Override
    public List<Region> get(Query query) {
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
        List<RegionSnapshot> snapshots = regs.stream().map(r -> new RegionSnapshot(
                r,
                permMap.getOrDefault(r.id(), List.of()),
                ruleMap.getOrDefault(r.id(), List.of()),
                dataMap.getOrDefault(r.id(), List.of())
        )).toList();
        return snapshots.stream().map(this::buildRegion).toList();
    }

    @Override
    public void save(Region region) {

    }

    @Override
    public HierarchyRepository getHierarchyRepository() {
        return null;
    }


    //HELPER
    private <T> List<T> selectRelational(Collection<DTO.Region> regions, Class<T> dtoClass) throws SQLException {
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

        for (DTO.Rule rule : snapshot.rules()) {
            r.setRuleValue(rule.key(), rule.value());
        }
        RegionDataContainer dc = r.getDataContainer();
        for (DTO.Data data : snapshot.data()) {
            dc.add(new RegionData(data.key(), GSON.fromJson(data.value(), JsonElement.class)));
        }
        for (DTO.Permission perm : snapshot.perms()) {
            r.addPermission(UUID.fromString(perm.playerUUID()), perm.level());
        }

        return r;
    }
}