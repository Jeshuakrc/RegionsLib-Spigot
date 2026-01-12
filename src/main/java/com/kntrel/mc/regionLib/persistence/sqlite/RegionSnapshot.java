package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.util.Fingerprint64;
import org.bukkit.util.BoundingBox;
import java.util.*;

class RegionSnapshot {

    //ASSETS
    private static final long FP_SEED = 42L;

    //FIELDS
    private final DTO.Region region_;
    private final DTO.Permission[] perms_;
    private final DTO.Rule[] rules_;
    private final DTO.Data[] data_;
    private final long fingerprint_, regionFingerprint_, permsFingerprint_, rulesFingerprint_, dataFingerprint_;


    //CONSTRUCTORS
    RegionSnapshot(DTO.Region region, DTO.Permission[] perms, DTO.Rule[] rules, DTO.Data[] data) {
        this.region_ = region;
        this.perms_ = perms;
        this.rules_ = rules;
        this.data_ = data;
        this.regionFingerprint_ = computeRegionFingerprint(this.region_);
        this.permsFingerprint_ = computePermsFingerprint(this.perms_);
        this.rulesFingerprint_ = computeRulesFingerPrint(this.rules_);
        this.dataFingerprint_ = computeDataFingerprint(this.data_);
        this.fingerprint_ = mixFingerprint(this.regionFingerprint_, this.permsFingerprint_, this.rulesFingerprint_, this.dataFingerprint_);
    }

    RegionSnapshot(DTO.Region region, Collection<DTO.Permission> perms, Collection<DTO.Rule> rules, Collection<DTO.Data> data) {
        this(region, perms.toArray(new DTO.Permission[0]), rules.toArray(new DTO.Rule[0]), data.toArray(new DTO.Data[0]));
    }

    //GETTERS
    public DTO.Region region() {
        return region_;
    }
    public DTO.Permission[] permissions() {
        return perms_;
    }
    public DTO.Rule[] rules() {
        return rules_;
    }

    public DTO.Data[] data() {
        return data_;
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


    //ACCESSORS
    public BoundingBox boundingBox() {
        return new BoundingBox(this.region_.minX(), this.region_.minY(), this.region_.minZ(), this.region_.maxX(), this.region_.maxY(), this.region_.maxZ());
    }
    public String worldName() {
        return this.region_.world();
    }
    public String name() {
        return this.region_.name();
    }


    //IMPLEMENTATION
    @Override public int hashCode() {
        return Long.hashCode(this.fingerprint_);
    }
    @Override public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (!(obj instanceof RegionSnapshot other)) { return false; }

        return this.region_.equals(other.region_)
                && Arrays.equals(this.perms_, other.perms_)
                && Arrays.equals(this.rules_, other.rules_)
                && Arrays.equals(this.data_, other.data_);
    }
    @Override public RegionSnapshot clone() {
        return new RegionSnapshot(
                this.region_,
                Arrays.copyOf(this.perms_, this.perms_.length),
                Arrays.copyOf(this.rules_, this.rules_.length),
                Arrays.copyOf(this.data_, this.data_.length)
        );
    }


    //HELPERS
    private static long computePermsFingerprint(DTO.Permission[] perms) {
        Arrays.sort(perms, Comparator.comparing(DTO.Permission::playerUUID));
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (DTO.Permission perm : perms) {
                fp.addString(perm.playerUUID()).addInt(perm.level());
        }
        fp.addInt(perms.length);
        return fp.finish();
    }
    private static long computeRulesFingerPrint(DTO.Rule[] rules) {
        Arrays.sort(rules, Comparator.comparing(DTO.Rule::key));
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (DTO.Rule rule : rules) {
            fp.addString(rule.key()).addString(rule.value());
        }
        fp.addInt(rules.length);
        return fp.finish();
    }
    private static long computeDataFingerprint(DTO.Data[] data) {
        Arrays.sort(data, Comparator.comparing(DTO.Data::key));
        Fingerprint64 fp = new Fingerprint64(FP_SEED);
        for (DTO.Data d : data) {
            fp.addString(d.key()).addString(d.value());
        }
        fp.addInt(data.length);
        return fp.finish();
    }

    private static long computeRegionFingerprint(DTO.Region region) {
        return new Fingerprint64(FP_SEED)
                .addLong(region.id())
                .addString(region.name())
                .addString(region.world())
                .addBool(region.enabled())
                .addInt(region.hierarchy())
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
