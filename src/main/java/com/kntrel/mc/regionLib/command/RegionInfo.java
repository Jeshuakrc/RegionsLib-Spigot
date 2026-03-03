package com.kntrel.mc.regionLib.command;

import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record RegionInfo(
        long id,
        String name,
        BoundingBox area,
        World world,
        Map<Hierarchy.Group, List<String>> permissions,
        List<RuleValue<?>> rules
) {

    public static RegionInfo of(Region region) {

        Map<Hierarchy.Group, List<String>> permissions = region.getPermissions().stream()
            .collect(Collectors.groupingBy(
                    Permission::getGroup,
                    Collectors.mapping(
                        p -> p.getPlayer().map(Player::getName).orElse(p.getPlayerId().toString()),
                        Collectors.toList()
                    )
            ));

        return new RegionInfo(
                region.getId(),
                region.getName(),
                region.getBoundingBox(),
                region.getWorld(),
                permissions,
                region.getRuleValues()
        );
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Region: ").append(name).append(" (ID: ").append(id).append(") ===\n");
        sb.append("World: ").append(world.getName()).append("\n");
        
        // Area/Coordinates
        sb.append("Area Bounds:\n");
        sb.append("  Min: X=").append(String.format("%.2f", area.getMinX()))
                .append(", Y=").append(String.format("%.2f", area.getMinY()))
                .append(", Z=").append(String.format("%.2f", area.getMinZ())).append("\n");
        sb.append("  Max: X=").append(String.format("%.2f", area.getMaxX()))
                .append(", Y=").append(String.format("%.2f", area.getMaxY()))
                .append(", Z=").append(String.format("%.2f", area.getMaxZ())).append("\n");
        
        // Permissions
        if (!permissions.isEmpty()) {
            sb.append("Permissions:\n");
            for (Map.Entry<Hierarchy.Group, List<String>> perm : permissions.entrySet()) {
                sb.append("  ").append(perm.getKey().getName()).append(":\n");
                for (String player : perm.getValue()) {
                    sb.append("    ").append(player).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("Permissions: None\n");
        }
        
        // Rules
        if (!rules.isEmpty()) {
            sb.append("Rules:\n");
            for (RuleValue<?> rule : rules) {
                sb.append("  ").append(rule.getRule().name()).append(": ").append(rule.get()).append("\n");
            }
        } else {
            sb.append("Rules: None\n");
        }
        
        return sb.toString();
    }
}
