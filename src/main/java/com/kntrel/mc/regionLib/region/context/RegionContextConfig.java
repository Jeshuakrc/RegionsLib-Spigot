package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.util.Grid;

//SUBTYPES
public class RegionContextConfig {

    //CONSTANTS
    private static final RegionContextConfig DEFAULT = new RegionContextConfig(
        4,
        32,
        Permission.OverlapMode.OLDEST,
        10,
        Grid.CellSize.SIZE_32,
        1024
    );


    //FACTORY
    public static RegionContextConfig defaultConfig() {
        return DEFAULT;
    }
    public static Builder build() {
        return new Builder();
    }


    public final int minNameLength;
    public final int maxNameLength;
    public final Permission.OverlapMode permissionsOverlapMode;
    public final int regionDisplayDurationSeconds;
    public final Grid.CellSize cellSize;
    public final int cacheCapacity;

    public RegionContextConfig(int minNameLength, int maxNameLength, Permission.OverlapMode permissionsOverlapMode, int regionDisplayDurationSeconds, Grid.CellSize cellSize, int cacheCapacity) {
        this.minNameLength = minNameLength;
        this.maxNameLength = maxNameLength;
        this.permissionsOverlapMode = permissionsOverlapMode;
        this.regionDisplayDurationSeconds = regionDisplayDurationSeconds;
        this.cellSize = cellSize;
        this.cacheCapacity = cacheCapacity;
    }


    //SUBTYPES
    public static class Builder {
        private int minNameLength = DEFAULT.minNameLength;
        private int maxNameLength = DEFAULT.maxNameLength;
        private Permission.OverlapMode permissionsOverlapMode = DEFAULT.permissionsOverlapMode;
        private int regionDisplayDurationSeconds = DEFAULT.regionDisplayDurationSeconds;
        private Grid.CellSize cellSize = DEFAULT.cellSize;
        private int cacheCapacity = DEFAULT.cacheCapacity;

        private Builder() {}

        public Builder withMinNameLength(int minNameLength) {
            this.minNameLength = minNameLength;
            return this;
        }
        public Builder withMaxNameLength(int maxNameLength) {
            this.maxNameLength = maxNameLength;
            return this;
        }
        public Builder withPermissionsOverlapMode(Permission.OverlapMode permissionsOverlapMode) {
            this.permissionsOverlapMode = permissionsOverlapMode;
            return this;
        }
        public Builder withRegionDisplayDurationSeconds(int regionDisplayDurationSeconds) {
            this.regionDisplayDurationSeconds = regionDisplayDurationSeconds;
            return this;
        }
        public Builder withCellSize(Grid.CellSize cellSize) {
            this.cellSize = cellSize;
            return this;
        }
        public Builder withCacheCapacity(int cacheCapacity) {
            this.cacheCapacity = cacheCapacity;
            return this;
        }
        public RegionContextConfig end() {
            return new RegionContextConfig(
                this.minNameLength,
                this.maxNameLength,
                this.permissionsOverlapMode,
                this.regionDisplayDurationSeconds,
                this.cellSize,
                this.cacheCapacity
            );
        }
    }
}
