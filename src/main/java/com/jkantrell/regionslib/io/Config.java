package com.jkantrell.regionslib.io;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.yamlizer.yaml.*;
import org.bukkit.Material;
import org.bukkit.Particle;
import java.io.FileNotFoundException;
import java.util.List;

public class Config extends AbstractYamlConfig {

    //CONSTRUCTOR
    public Config(String filePath) {
        super(filePath);
        Yamlizer yamlizer_ = this.yamlizer;
        yamlizer_.addSerializationRule(Config.ParticleData.class,
                (e,t) -> {
                    YamlMap map = e.get(YamlElementType.MAP);
                    return new ParticleData(
                            Particle.valueOf(map.get("type").get(YamlElementType.STRING)),
                            map.get("count").get(YamlElementType.INT),
                            yamlizer_.deserialize(map.get("delta"),int[].class)
                    );
                }
            );
    }

    @Override
    public void load() throws FileNotFoundException {
        super.load();
        this.configPath = "plugins/" + RegionsLib.getMain().getName();
    }

    //ENUMS
    public enum OverlappingPermissionsMode {
        all, any, oldest, newest
    }

    //RECORDS
    public record ParticleData(Particle particle, int count, int[] delta) {}

    //FIELDS
    public String configPath;

    @ConfigField
    public Config.OverlappingPermissionsMode overlappingPermissionsMode = Config.OverlappingPermissionsMode.all;

    @ConfigField
    public List<Material> plantableBlocks = List.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.KELP, Material.BAMBOO_SAPLING,
            Material.SUGAR_CANE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.NETHER_WART, Material.ACACIA_SAPLING,
            Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING, Material.OAK_SAPLING
    );

    @ConfigField
    public List<Material> breakableRedstoneBlocks = List.of(Material.REDSTONE_WIRE);

    @ConfigField(path = "regionBorderParticle.resolution")
    public int regionBorderResolution = 1;

    @ConfigField
    public ParticleData regionBorderParticle = new ParticleData(Particle.NAUTILUS, 1, new int[] {0,0,0});

}
