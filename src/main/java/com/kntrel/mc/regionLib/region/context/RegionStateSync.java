package com.kntrel.mc.regionLib.region.context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class RegionStateSync {

    private static final Gson GSON = new Gson();


    private RegionStateSync() {}


    //API
    public static void mergeIntoCanonical(Region canonical, Region incoming) {
        if (canonical == incoming) { return; }
        validateCompatibility(canonical, incoming);

        RegionSnapshot base = incoming.getRememberedState().orElse(null);
        if (base == null) {
            overwrite(canonical, incoming);
            return;
        }

        RegionSnapshot current = new RegionSnapshot(canonical);
        RegionSnapshot next = new RegionSnapshot(incoming);

        if (current.destroyed() && !next.destroyed()) {
            return;
        }

        String finalName = current.name();
        if (fieldChanged(base.name(), next.name())) {
            finalName = incoming.getName();
        }

        boolean finalEnabled = current.enabled();
        if (fieldChanged(base.enabled(), next.enabled())) {
            finalEnabled = incoming.isEnabled();
        }

        Hierarchy finalHierarchy = canonical.getHierarchy();
        if (fieldChanged(base.hierarchy(), next.hierarchy())) {
            finalHierarchy = incoming.getHierarchy();
        }

        boolean applyBounds = boundsChanged(base, next);
        boolean destroy = shouldDestroy(current, base, next);

        Map<UUID, Integer> permissions = permissionLevels(current);
        mergeEntries(permissions, permissionLevels(base), permissionLevels(next));

        Map<String, String> rules = entryValues(current.rules());
        mergeEntries(rules, entryValues(base.rules()), entryValues(next.rules()));

        Map<String, String> data = entryValues(current.data());
        mergeEntries(data, entryValues(base.data()), entryValues(next.data()));

        canonical.setName(finalName);
        canonical.setHierarchy(finalHierarchy);
        canonical.enabled(finalEnabled);
        if (applyBounds) {
            canonical.resize(incoming.getBoundingBox());
        }

        canonical.clearPermissions();
        permissions.forEach(canonical::addPermission);

        canonical.clearRules();
        rules.forEach(canonical::setRuleValue);

        canonical.getDataContainer().clear();
        data.forEach((key, value) -> canonical.getDataContainer().add(
                new RegionData(key, GSON.fromJson(value, JsonElement.class))
        ));

        if (destroy) {
            canonical.destroy();
        }
    }
    public static void overwrite(Region target, Region source) {
        if (target == source) { return; }
        validateCompatibility(target, source);

        if (target.isDestroyed() && !source.isDestroyed()) {
            return;
        }

        target.setName(source.getName());
        target.setHierarchy(source.getHierarchy());
        target.enabled(source.isEnabled());
        target.resize(source.getBoundingBox());

        target.clearPermissions();
        source.getPermissions().forEach(p -> target.addPermission(p.getPlayerId(), p.getGroup().getLevel()));

        target.clearRules();
        for (RuleValue<?> ruleValue : source.getRuleValues()) {
            target.setRuleValue(ruleValue);
        }

        target.getDataContainer().clear();
        for (RegionData data : source.getDataContainer().getAll()) {
            target.getDataContainer().add(new RegionData(data.getKey(), data.getValue().deepCopy()));
        }

        if (source.isDestroyed()) {
            target.destroy();
        }
    }


    //PRIVATE
    private static void validateCompatibility(Region target, Region source) {
        Long targetId = target.getId();
        Long sourceId = source.getId();
        if (targetId != null && sourceId != null && !targetId.equals(sourceId)) {
            throw new IllegalArgumentException("Cannot sync regions with different IDs.");
        }
        if (target.getContext() != source.getContext()) {
            throw new IllegalArgumentException("Cannot sync regions from different contexts.");
        }
        if (!target.getWorld().equals(source.getWorld())) {
            throw new IllegalArgumentException("Cannot sync regions from different worlds.");
        }
    }
    private static boolean fieldChanged(@Nullable Object base, Object next) {
        return base == null || !Objects.equals(base, next);
    }
    private static boolean boundsChanged(@Nullable RegionSnapshot base, RegionSnapshot next) {
        return base == null
                || Double.compare(base.minX(), next.minX()) != 0
                || Double.compare(base.minY(), next.minY()) != 0
                || Double.compare(base.minZ(), next.minZ()) != 0
                || Double.compare(base.maxX(), next.maxX()) != 0
                || Double.compare(base.maxY(), next.maxY()) != 0
                || Double.compare(base.maxZ(), next.maxZ()) != 0;
    }
    private static boolean shouldDestroy(RegionSnapshot current, RegionSnapshot base, RegionSnapshot next) {
        if (current.destroyed()) { return true; }
        return (base == null || base.destroyed() != next.destroyed()) && next.destroyed();
    }
    private static Map<UUID, Integer> permissionLevels(RegionSnapshot snapshot) {
        Map<UUID, Integer> out = new HashMap<>();
        for (RegionSnapshot.Permission permission : snapshot.permissions()) {
            out.put(permission.playerUUID(), permission.level());
        }
        return out;
    }
    private static Map<String, String> entryValues(Iterable<RegionSnapshot.Entry> entries) {
        Map<String, String> out = new HashMap<>();
        for (RegionSnapshot.Entry entry : entries) {
            out.put(entry.key(), entry.value());
        }
        return out;
    }
    private static <K, V> void mergeEntries(Map<K, V> current, Map<K, V> base, Map<K, V> next) {
        Set<K> keys = new HashSet<>(base.keySet());
        keys.addAll(next.keySet());
        for (K key : keys) {
            if (Objects.equals(base.get(key), next.get(key))) {
                continue;
            }
            if (next.containsKey(key)) {
                current.put(key, next.get(key));
            } else {
                current.remove(key);
            }
        }
    }
}
