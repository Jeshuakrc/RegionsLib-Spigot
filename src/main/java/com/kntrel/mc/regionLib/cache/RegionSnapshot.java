package com.kntrel.mc.regionLib.cache;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.dataContainer.RegionData;
import com.kntrel.mc.regionLib.region.dataContainer.RegionDataContainer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.util.Fingerprint64;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class RegionSnapshot {

    //ASSETS
    private static final long FP_SEED = 42L;
    private static final Gson GSON = new Gson();


    //SUBTYPES
    public record Permission(UUID playerUUID, int level) {}
    public record Entry(String key, String value) {}


    //FIELDS
    private final long id_;
    private final String name_;
    private final UUID world_;
    private final boolean enabled_, destroyed_;
    private final long hierarchy_;
    private final double minX_, minY_, minZ_, maxX_, maxY_, maxZ_;
    private final Permission[] permissions_;
    private final Entry[] rules_;
    private final Entry[] data_;
    private final long fingerprint_, regionFingerprint_, permsFingerprint_, rulesFingerprint_, dataFingerprint_;


    //CONSTRUCTORS
    private RegionSnapshot(
            long id,
            String name,
            UUID world,
            boolean enabled,
            long hierarchy,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            boolean destroyed,
            Permission[] permissions,
            Entry[] rules,
            Entry[] data
    ) {
        this.id_ = id;
        this.name_ = name;
        this.world_ = world;
        this.enabled_ = enabled;
        this.hierarchy_ = hierarchy;
        this.minX_ = minX;
        this.minY_ = minY;
        this.minZ_ = minZ;
        this.maxX_ = maxX;
        this.maxY_ = maxY;
        this.maxZ_ = maxZ;
        this.destroyed_ = destroyed;
        this.permissions_ = permissions;
        this.rules_ = rules;
        this.data_ = data;

        this.regionFingerprint_ = computeRegionFingerprint(this);
        this.permsFingerprint_ = computePermsFingerprint(this.permissions_);
        this.rulesFingerprint_ = computeEntriesFingerPrint(this.rules_);
        this.dataFingerprint_ = computeEntriesFingerPrint(this.data_);
        this.fingerprint_ = mixFingerprint(this.regionFingerprint_, this.permsFingerprint_, this.rulesFingerprint_, this.dataFingerprint_);
    }
    public RegionSnapshot(
            long id,
            String name,
            UUID world,
            boolean enabled,
            long hierarchy,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            boolean destroyed,
            Collection<Permission> permissions,
            Collection<Entry> rules,
            Collection<Entry> data
    ) {
        this(
                id,
                name,
                world,
                enabled,
                hierarchy,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                destroyed,
                permissions.toArray(new Permission[0]),
                rules.toArray(new Entry[0]),
                data.toArray(new Entry[0])
        );
    }
    public RegionSnapshot(Region source) {
        this(
                source.getId(),
                source.getName(),
                source.getWorld().getUID(),
                source.isEnabled(),
                source.getHierarchy().getId(),
                source.getMinX(),
                source.getMinY(),
                source.getMinZ(),
                source.getMaxX(),
                source.getMaxY(),
                source.getMaxZ(),
                source.isDestroyed(),
                source.getPermissions().stream().map(p -> new Permission(p.getPlayerId(), p.getGroup().getLevel())).toArray(Permission[]::new),
                source.getRuleValues().stream().map(r -> new Entry(r.getRule().name(), r.toString())).toArray(Entry[]::new),
                source.getDataContainer().getAll().stream().map(d -> new Entry(d.getKey(), GSON.toJson(d.getValue()))).toArray(Entry[]::new)
        );
    }


    //GETTERS
    public long id() {
        return this.id_;
    }
    public String name() {
        return this.name_;
    }
    public UUID world() {
        return this.world_;
    }
    public boolean enabled() {
        return this.enabled_;
    }
    public boolean destroyed() {
        return this.destroyed_;
    }
    public long hierarchy() {
        return this.hierarchy_;
    }
    public double minX() {
        return this.minX_;
    }
    public double minY() {
        return this.minY_;
    }
    public double minZ() {
        return this.minZ_;
    }
    public double maxX() {
        return this.maxX_;
    }
    public double maxY() {
        return this.maxY_;
    }
    public double maxZ() {
        return this.maxZ_;
    }
    public Set<Permission> permissions() {
        return Set.of(this.permissions_);
    }
    public Set<Entry> rules() {
        return Set.of(this.rules_);
    }
    public Set<Entry> data() {
        return Set.of(this.data_);
    }
    public long fingerPrint() {
        return fingerprint_;
    }
    public long regionFingerprint() {
        return regionFingerprint_;
    }
    public long permissionsFingerprint() {
        return permsFingerprint_;
    }
    public long rulesFingerprint() {
        return rulesFingerprint_;
    }
    public long dataFingerprint() {
        return dataFingerprint_;
    }
    public BoundingBox boundingBox() {
        return new BoundingBox(this.minX_, this.minY_, this.minZ_, this.maxX_, this.maxY_, this.maxZ_);
    }


    //UTILITY
    public Region toRegion(RegionContext context) {
        return toRegion(context, this);
    }


    //IMPLEMENTATION
    @Override public int hashCode() {
        return Long.hashCode(this.fingerprint_);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (!(obj instanceof RegionSnapshot other)) { return false; }

        return     this.id_ == other.id_
                && this.name_.equals(other.name_)
                && this.world_.equals(other.world_)
                && this.enabled_ == other.enabled_
                && this.hierarchy_ == other.hierarchy_
                && Double.compare(this.minX_, other.minX_) == 0
                && Double.compare(this.minY_, other.minY_) == 0
                && Double.compare(this.minZ_, other.minZ_) == 0
                && Double.compare(this.maxX_, other.maxX_) == 0
                && Double.compare(this.maxY_, other.maxY_) == 0
                && Double.compare(this.maxZ_, other.maxZ_) == 0
                && this.destroyed_ == other.destroyed_
                && Arrays.equals(this.permissions_, other.permissions_)
                && Arrays.equals(this.rules_, other.rules_)
                && Arrays.equals(this.data_, other.data_);
    }
    @Override public RegionSnapshot clone() {
        return new RegionSnapshot(
                this.id_,
                this.name_,
                this.world_,
                this.enabled_,
                this.hierarchy_,
                this.minX_,
                this.minY_,
                this.minZ_,
                this.maxX_,
                this.maxY_,
                this.maxZ_,
                this.destroyed_,
                this.permissions_,
                this.rules_,
                this.data_
        );
    }


    //PUBLIC HELPERS
    private Region toRegion(RegionContext context, RegionSnapshot snapshot) {
        World world = context.getServer().getWorld(snapshot.world());
        Hierarchy hierarchy = context.getHierarchyRepository().get(snapshot.hierarchy()).orElseThrow();
        Region r = new Region(context, snapshot.boundingBox(), world, snapshot.name(), hierarchy);

        r.setId(snapshot.id());

        if (!snapshot.enabled()) {
            r.enabled(false);
        }
        if (snapshot.destroyed()) {
            r.destroy();
        }

        for (RegionSnapshot.Entry rule : snapshot.rules()) {
            Rule<?> actualRule = context.getRuleRegistry().get(rule.key()).orElse(null);
            if (actualRule == null) { r.setRuleValue(rule.key(), rule.value()); }
            else { r.setRuleValue(new RuleValue<>(actualRule, rule.value)); }
        }
        RegionDataContainer dc = r.getDataContainer();
        for (RegionSnapshot.Entry data : snapshot.data()) {
            dc.add(new RegionData(data.key(), GSON.fromJson(data.value(), JsonElement.class)));
        }
        for (RegionSnapshot.Permission perm : snapshot.permissions()) {
            r.addPermission(perm.playerUUID(), perm.level());
        }

        return r;
    }


    //HELPERS
    private static long computePermsFingerprint(Permission[] perms) {
        Arrays.sort(perms, Comparator.comparing(Permission::playerUUID));
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (Permission perm : perms) {
            fp.addUUID(perm.playerUUID()).addInt(perm.level());
        }
        fp.addInt(perms.length);
        return fp.finish();
    }
    private static long computeEntriesFingerPrint(Entry[] rules) {
        Arrays.sort(rules, Comparator.comparing(Entry::key));
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (Entry rule : rules) {
            fp.addString(rule.key()).addString(rule.value());
        }
        fp.addInt(rules.length);
        return fp.finish();
    }

    private static long computeRegionFingerprint(RegionSnapshot region) {
        return new Fingerprint64(FP_SEED)
                .addLong(region.id())
                .addString(region.name())
                .addUUID(region.world())
                .addBool(region.enabled())
                .addLong(region.hierarchy())
                .addDouble(region.minX())
                .addDouble(region.minY())
                .addDouble(region.minZ())
                .addDouble(region.maxX())
                .addDouble(region.maxY())
                .addDouble(region.maxZ())
                .addBool(region.destroyed())
                .finish();
    }

    private static long mixFingerprint(long... fingerprints) {
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (long l : fingerprints) { fp.addLong(l); }
        return fp.finish();
    }
}
