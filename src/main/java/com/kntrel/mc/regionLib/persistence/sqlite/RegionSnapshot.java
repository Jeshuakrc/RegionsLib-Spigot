package com.kntrel.mc.regionLib.persistence.sqlite;

import org.bukkit.util.BoundingBox;
import java.util.*;

record RegionSnapshot(DTO.Region region, DTO.Permission[] perms, DTO.Rule[] rules, DTO.Data[] data) {

    RegionSnapshot(DTO.Region region, DTO.Permission[] perms, DTO.Rule[] rules, DTO.Data[] data) {
        this.region = region;
        this.perms = perms;
        this.rules = rules;
        this.data = data;

        Arrays.sort(perms, Comparator.comparing(DTO.Permission::playerUUID));
        Arrays.sort(rules, Comparator.comparing(DTO.Rule::key));
        Arrays.sort(data, Comparator.comparing(DTO.Data::key));
    }

    RegionSnapshot(DTO.Region region, Collection<DTO.Permission> perms, Collection<DTO.Rule> rules, Collection<DTO.Data> data) {
        this(region, perms.toArray(new DTO.Permission[0]), rules.toArray(new DTO.Rule[0]), data.toArray(new DTO.Data[0]));
    }

    public BoundingBox boundingBox() {
        return new BoundingBox(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ());
    }
    public String worldName() {
        return region.world();
    }
    public String name() {
        return region.name();
    }

    @Override public int hashCode() {
        return Objects.hash(
                region,
                Arrays.hashCode(perms),
                Arrays.hashCode(rules),
                Arrays.hashCode(data)
        );
    }
}
