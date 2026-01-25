package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.ability.AbilityBuilder;
import com.kntrel.mc.regionLib.region.ability.AbilityRegistry;
import com.kntrel.mc.regionLib.region.ability.Permission;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.display.BlockDisplayAreaDisplayer;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import com.kntrel.mc.regionLib.util.Grid;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.function.Function;

public class RegionContext implements RegionRepository {

    //SUBTYPES
    public static class Config {
        public final int minNameLength;
        public final int maxNameLength;
        public final Permission.OverlapMode permissionsOverlapMode;
        public final int regionDisplayDurationSeconds;
        private final Grid.CellSize cellSize;

        public Config(int minNameLength, int maxNameLength, Permission.OverlapMode permissionsOverlapMode, int regionDisplayDurationSeconds, Grid.CellSize cellSize) {
            this.minNameLength = minNameLength;
            this.maxNameLength = maxNameLength;
            this.permissionsOverlapMode = permissionsOverlapMode;
            this.regionDisplayDurationSeconds = regionDisplayDurationSeconds;
            this.cellSize = cellSize;
        }

    }


    //FIELDS
    private final String namespace_;
    private final Config config_;
    private final Plugin plugin_;
    private final RegionRepository regionRepository_;
    private final HotRegionRepository hotRegionRepository_;
    private final HierarchyRepository hierarchyRepository_;
    private final AbilityRegistry abilityRegistry_;
    private final RuleRegistry ruleRegistry_;
    private final DisplayController displayController_;


    //CONSTRUCTORS
    public RegionContext(String namespace, Config config, Plugin plugin, Function<RegionContext, RegionRepository> regionRepFactory, Function<RegionContext, HierarchyRepository> hierarchyRepFactory) {
        this.namespace_ = namespace;
        this.config_ = config;
        this.plugin_ = plugin;
        this.regionRepository_ = regionRepFactory.apply(this);
        this.hierarchyRepository_ = hierarchyRepFactory.apply(this);
        this.abilityRegistry_ = new AbilityRegistry(this);
        this.ruleRegistry_ = new RuleRegistry(this);
        this.displayController_ = new DisplayController(this, new BlockDisplayAreaDisplayer(this), this.config_.regionDisplayDurationSeconds);
        this.hotRegionRepository_ = new HotRegionRepository(this.regionRepository_, this.config_.cellSize);
    }
    public RegionContext(Config config, Plugin plugin, Function<RegionContext, RegionRepository> regionRepFactory, Function<RegionContext, HierarchyRepository> hierarchyRepFactory) {
        this(plugin.getName(), config, plugin, regionRepFactory, hierarchyRepFactory);
    }
    public RegionContext(String namespace, Config config, Plugin plugin, RegionRepository regionRep, HierarchyRepository hierarchyRep) {
        this(namespace, config, plugin, rc -> regionRep, rc -> hierarchyRep);
    }
    public RegionContext(Config config, Plugin plugin, RegionRepository regionRep, HierarchyRepository hierarchyRep) {
        this(plugin.getName(), config, plugin, rc -> regionRep, rc -> hierarchyRep);
    }


    //GETTERS
    public String getNamespace() {
        return this.namespace_;
    }
    public Config getConfig() {
        return this.config_;
    }
    public RegionRepository getRegionRepository() {
        return this.regionRepository_;
    }
    public Plugin getPlugin() {
        return this.plugin_;
    }
    public Server getServer() {
        return this.plugin_.getServer();
    }
    public AbilityRegistry getAbilityRegistry() {
        return this.abilityRegistry_;
    }
    public RuleRegistry getRuleRegistry() {
        return this.ruleRegistry_;
    }
    public RegionRepository getHotRegionRepository() {
        return this.hotRegionRepository_;
    }
    public HierarchyRepository getHierarchyRepository() {
        return this.hierarchyRepository_;
    }


    //API
    public void callEvent(Event event) {
        this.plugin_.getServer().getPluginManager().callEvent(event);
    }
    public void registerAbility(Ability ability) {
        this.abilityRegistry_.register(ability);
    }
    public <E extends Event> AbilityBuilder<E> registerAbilityOn(Class<E> eventClass) {
        return this.abilityRegistry_.registerOn(eventClass);
    }
    public void displayRegion(Region region, AreaDisplayer displayer, long seconds, Player player) {
        this.displayController_.display(region, displayer, seconds, player);
    }
    public void displayRegion(Region region, AreaDisplayer displayer, Player player) {
        this.displayController_.display(region, displayer, player);
    }
    public void displayRegion(Region region, long second, Player player) {
        this.displayController_.display(region, second, player);
    }
    public void displayRegion(Region region, Player player) {
        this.displayController_.display(region, player);
    }
    public void stopDisplayRegion(Region region) {
        this.displayController_.stopDisplay(region);
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        return this.regionRepository_.get(query);
    }
    @Override public void save(Region... regions) {
        this.regionRepository_.save(regions);
    }
}
