package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.util.Grid;

//SUBTYPES
/**
 * Configuration options for a {@link RegionContext}.
 */
public class RegionContextConfig {

    //CONSTANTS
    private static final long DEFAULT_PLAYER_SAMPLING_PERIOD_TICKS = 1L;
    private static final double DEFAULT_PLAYER_MOVEMENT_TOLERANCE = 0D;
    private static final RegionContextConfig DEFAULT = new RegionContextConfig(
        4,
        32,
        Permission.OverlapMode.OLDEST,
        10,
        Grid.CellSize.SIZE_32,
        1024,
        DEFAULT_PLAYER_SAMPLING_PERIOD_TICKS,
        DEFAULT_PLAYER_MOVEMENT_TOLERANCE
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
    public final long playerSamplingPeriodTicks;
    public final double playerMovementTolerance;

    public RegionContextConfig(int minNameLength, int maxNameLength, Permission.OverlapMode permissionsOverlapMode, int regionDisplayDurationSeconds, Grid.CellSize cellSize, int cacheCapacity) {
        this(minNameLength, maxNameLength, permissionsOverlapMode, regionDisplayDurationSeconds, cellSize, cacheCapacity, DEFAULT_PLAYER_SAMPLING_PERIOD_TICKS, DEFAULT_PLAYER_MOVEMENT_TOLERANCE);
    }
    public RegionContextConfig(int minNameLength, int maxNameLength, Permission.OverlapMode permissionsOverlapMode, int regionDisplayDurationSeconds, Grid.CellSize cellSize, int cacheCapacity, long playerSamplingPeriodTicks) {
        this(minNameLength, maxNameLength, permissionsOverlapMode, regionDisplayDurationSeconds, cellSize, cacheCapacity, playerSamplingPeriodTicks, DEFAULT_PLAYER_MOVEMENT_TOLERANCE);
    }
    public RegionContextConfig(int minNameLength, int maxNameLength, Permission.OverlapMode permissionsOverlapMode, int regionDisplayDurationSeconds, Grid.CellSize cellSize, int cacheCapacity, long playerSamplingPeriodTicks, double playerMovementTolerance) {
        if (playerSamplingPeriodTicks < 1L) {
            throw new IllegalArgumentException("Player sampling period must be at least 1 tick.");
        }
        if (playerMovementTolerance < 0D) {
            throw new IllegalArgumentException("Player movement tolerance cannot be negative.");
        }
        this.minNameLength = minNameLength;
        this.maxNameLength = maxNameLength;
        this.permissionsOverlapMode = permissionsOverlapMode;
        this.regionDisplayDurationSeconds = regionDisplayDurationSeconds;
        this.cellSize = cellSize;
        this.cacheCapacity = cacheCapacity;
        this.playerSamplingPeriodTicks = playerSamplingPeriodTicks;
        this.playerMovementTolerance = playerMovementTolerance;
    }


    //SUBTYPES
    public static class Builder {
        private int minNameLength = DEFAULT.minNameLength;
        private int maxNameLength = DEFAULT.maxNameLength;
        private Permission.OverlapMode permissionsOverlapMode = DEFAULT.permissionsOverlapMode;
        private int regionDisplayDurationSeconds = DEFAULT.regionDisplayDurationSeconds;
        private Grid.CellSize cellSize = DEFAULT.cellSize;
        private int cacheCapacity = DEFAULT.cacheCapacity;
        private long playerSamplingPeriodTicks = DEFAULT.playerSamplingPeriodTicks;
        private double playerMovementTolerance = DEFAULT.playerMovementTolerance;

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
        public Builder withPlayerSamplingPeriodTicks(long playerSamplingPeriodTicks) {
            this.playerSamplingPeriodTicks = playerSamplingPeriodTicks;
            return this;
        }
        public Builder withPlayerMovementTolerance(double playerMovementTolerance) {
            this.playerMovementTolerance = playerMovementTolerance;
            return this;
        }
        public RegionContextConfig end() {
            return new RegionContextConfig(
                this.minNameLength,
                this.maxNameLength,
                this.permissionsOverlapMode,
                this.regionDisplayDurationSeconds,
                this.cellSize,
                this.cacheCapacity,
                this.playerSamplingPeriodTicks,
                this.playerMovementTolerance
            );
        }
    }
}
