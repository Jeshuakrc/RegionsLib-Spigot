package com.jkantrell.regionslib.io;

import com.jkantrell.regionslib.RegionsLib;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ConfigManager {

    private static Plugin main_ = RegionsLib.getMain();
    private static FileConfiguration configFile_ = new YamlConfiguration();

    public static void initialize() {
        File configFile = new File(main_.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            main_.saveResource("config.yml", false);
        }

        try {
            configFile_.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }


    }

    protected static String getConfigPath() {
        return configFile_.getString("config_folder");
    }
    public static ConfigManager.overlappingPermissionsMode getOverlappingRegionMode() {
        String val = configFile_.getString("overlapping_permissions_mode");
        return ConfigManager.overlappingPermissionsMode.valueOf(val);
    }
    public static List<Material> getPlantableBlocks() {

        List<String> sList = configFile_.getStringList("plantable_blocks");
        List<Material> mList = new java.util.ArrayList<>(Collections.emptyList());
        for (String i : sList) {
            mList.add(Material.valueOf(i));
        }
        return mList;
    }
    public static List<Material> getEnforcedButtons() {

        List<String> sList = configFile_.getStringList("enforced_buttons");
        List<Material> mList = new java.util.ArrayList<>(Collections.emptyList());
        for (String i : sList) {
            mList.add(Material.valueOf(i));
        }
        return mList;
    }
    public static List<Material> getLeverLockerBlocks() {

        List<String> sList = configFile_.getStringList("lever_locker_blocks");
        List<Material> mList = new java.util.ArrayList<>(Collections.emptyList());
        for (String i : sList) {
            mList.add(Material.valueOf(i));
        }
        return mList;
    }
    public static List<Material> getBreakableRedstoneBlocks() {

        List<String> sList = configFile_.getStringList("breakable_redstone_blocks");
        List<Material> mList = new java.util.ArrayList<>(Collections.emptyList());
        for (String i : sList) {
            mList.add(Material.valueOf(i));
        }
        return mList;
    }
    public static ConfigManager.endCrystalOnAnyBlock getEndCystalOnAnyBlock(){
        String val = configFile_.getString("end_crystal_on_any_block");
        return ConfigManager.endCrystalOnAnyBlock.valueOf(val);
    }
    public static Material getTotemUpgradeItem(){
        return Material.valueOf(configFile_.getString("upgrade_totem_item"));
    }
    public static Material getTotemDowngradeItem(){
        return Material.valueOf(configFile_.getString("downgrade_totem_item"));
    }
    public static boolean getTotemUpgradeItemConsume(){
        return configFile_.getBoolean("upgrade_totem_item_consume");
    }
    public static boolean getTotemDowngradeItemConsume(){
        return configFile_.getBoolean("Downgrade_totem_item_consume");
    }
    public static int getTotemInteractCooldown(){
        return configFile_.getInt("totem_interact_cooldown");
    }
    public static double getTotemDropBackRate(){
        return configFile_.getDouble("totem_drop_back_rate");
    }
    public static int getTotemBorderResolution() {
        int r = configFile_.getInt("totem_border_resolution");
        if (r < 0) { return 1; } else { return r; }
    }
    public static String getDefaultLanguageCode() {
        return configFile_.getString("default_language");
    }
    public static Material getScriptureExchangeItem() {
        return Material.valueOf(configFile_.getString("scriptures_exchange_item"));
    }
    public static int getDeedsPlayersPerPage(){
        return configFile_.getInt("deeds_players_per_page");
    }
    public static int getDeedsPlayersForNewPage(){
        return configFile_.getInt("deeds_players_for_new_page");
    }
    public static int getRegionMaxNameLength() {
        return configFile_.getInt("maximum_region_name_length");
    }
    public static int getRegionMinNameLength() {
        return configFile_.getInt("minimum_region_name_length");
    }
    public static ParticleData getRegionBorderParticle() {
        Particle particle = Particle.valueOf(configFile_.getString("region_border_particle_type"));
        int count = configFile_.getInt("region_border_particle_count");
        List<Integer> deltatList = configFile_.getIntegerList("region_border_particle_delta");
        int[] delta = new int[deltatList.size()];
        for (int i = 0; i < delta.length; i++) {
            delta[i] = deltatList.get(i);
        }

        return new ParticleData(particle,count,delta);
    }
    public static int getRegionBorderRefreshRate(){
        return configFile_.getInt("region_border_refresh_rate");
    }
    public static int getRegionBorderPersistenceBell(){
        return configFile_.getInt("region_border_persistence_bell");
    }
    public static int getRegionBorderPersistencePlaced(){
        return configFile_.getInt("region_border_persistence_placed");
    }
    public static ParticleData getTotemPlaceParticle() {
        Particle particle = Particle.valueOf(configFile_.getString("totem_place_particle_type"));
        int count = configFile_.getInt("totem_place_particle_count");
        List<Integer> deltatList = configFile_.getIntegerList("totem_place_particle_delta");
        int[] delta = new int[deltatList.size()];
        for (int i = 0; i < delta.length; i++) {
            delta[i] = deltatList.get(i);
        }

        return new ParticleData(particle,count,delta);
    }
    public static int[] getTotemPlaceParticlePos() {
        List<Integer> posList = configFile_.getIntegerList("totem_place_particle_position");
        int[] pos = new int[posList.size()];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = posList.get(i);
        }
        return pos;
    }
    public static groupLevelReach getTotemResizeMessageLevelReach() {
        String path = "region_messages_maxLevelReach_resize";
        groupLevelReach r;

        if (configFile_.isInt(path)) {
            r = groupLevelReach.lvl;
            r.setLevel(configFile_.getInt(path));
            return r;
        }

        String val = configFile_.getString("region_messages_maxLevelReach_resize");

        try {
            r = groupLevelReach.valueOf(val);
        } catch (Exception e) {
            r = groupLevelReach.noOne;
        }
        return r;
    }
    public static List<PotionType> getTotemDestroyArrowEffects() {
        List<String> sList = configFile_.getStringList("totem_destroy_arrow_effects");
        List<PotionType> eList = new ArrayList<>();
        for (String i : sList) {
            eList.add(PotionType.valueOf(i));
        }
        return eList;
    }
    public static TitleData getRegionTitleDisplayData() {
        String mainPath = "region_names_title_display.";
        return new TitleData(
                configFile_.getInt(mainPath+"fadeIn"),
                configFile_.getInt(mainPath+"stay"),
                configFile_.getInt(mainPath+"fadeOut")
        );
    }
    public static boolean getRegionTitleDisplayEnabled() {
        return configFile_.getBoolean("region_names_title_display.enabled");
    }
    public static int getRegionTitleDisplayRefreshRate() {
        return configFile_.getInt("region_names_title_display.refreshRate");
    }
    public static int getDefaultGroupLevel() {
        return configFile_.getInt("default_group_level");
    }

    public record ParticleData(Particle particle, int count, int[] delta) {}
    public record TitleData(int fadeIn, int stay, int fadeOut) {}
    public enum overlappingPermissionsMode {
        all, any, oldest, newest
    }
    public enum endCrystalOnAnyBlock {
        never,
        always,
        on_totem
    }
    public enum groupLevelReach {
        noOne, all, responsible, members, lvl;
        private int level_ = -1;

        public void setLevel(int level) {
            this.level_ = level;
        }
        public int getLevel() {
            return this.level_;
        }
    }
}
