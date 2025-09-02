package com.kntrel.mc.regionLib.io;

import org.bukkit.Material;
import org.bukkit.Particle;
import java.util.List;

public class Config {


    //ENUMS
    public enum OverlappingPermissionsMode {
        all, any, oldest, newest
    }

    //RECORDS
    public record ParticleData(Particle particle, int count, int[] delta) {}

    //FIELDS
    public String configPath = null;

    public int maxNameLength = 24;

    public int minNameLength = 4;

    public Config.OverlappingPermissionsMode overlappingPermissionsMode = Config.OverlappingPermissionsMode.all;

    public List<Material> plantableBlocks = List.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.KELP, Material.BAMBOO_SAPLING,
            Material.SUGAR_CANE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.NETHER_WART, Material.ACACIA_SAPLING,
            Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING, Material.OAK_SAPLING
    );

    public List<Material> breakableRedstoneBlocks = List.of(Material.REDSTONE_WIRE);

    public int regionDisplayDuration = 10;
}
