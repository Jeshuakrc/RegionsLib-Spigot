package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.cache.RegionCache;
import com.kntrel.mc.regionLib.event.RegionLoadEvent;
import com.kntrel.mc.regionLib.event.RegionUnloadEvent;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.ability.AbilityRegistry;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.display.BlockDisplayAreaDisplayer;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.AttributedRegionRepository;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionReadRepository;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Holds configuration, repositories, and registries for a region namespace.
 */
public class RegionContext implements AttributedRegionRepository {

    private static class HotRegionRepositoryWrapper implements RegionRepository {
        private final HotRegionRepository inner_;
        HotRegionRepositoryWrapper(HotRegionRepository inner) { this.inner_ = inner; }
        @Override public List<Region> get(Query query) { return this.inner_.get(query); }
        @Override public void save(Region... regions) { this.inner_.save(regions); }
    }


    //FIELDS
    private final String namespace_;
    private final RegionContextConfig config_;
    private final Plugin plugin_;
    private final RegionRepository delegateRegionRepository_;
    private final HotRegionRepository hotRegionRepository_;
    private final MainRegionRepository regionRepository_;
    private final HierarchyRepository hierarchyRepository_;
    private final AbilityRegistry abilityRegistry_;
    private final RuleRegistry ruleRegistry_;
    private final DisplayController displayController_;
    private final RegionCache cache_;


    //CONSTRUCTORS
    public RegionContext(String namespace, RegionContextConfig config, Plugin plugin, Function<RegionContext, RegionRepository> regionRepFactory, Function<RegionContext, HierarchyRepository> hierarchyRepFactory) {
        this.namespace_ = namespace;
        this.config_ = config;
        this.plugin_ = plugin;
        this.hierarchyRepository_ = hierarchyRepFactory.apply(this);
        this.abilityRegistry_ = new AbilityRegistry(this, this.config_.permissionsOverlapMode);
        this.ruleRegistry_ = new RuleRegistry(this);
        this.displayController_ = new DisplayController(this, new BlockDisplayAreaDisplayer(this), this.config_.regionDisplayDurationSeconds);
        this.cache_ = new RegionCache(this.config_.cacheCapacity);

        this.delegateRegionRepository_ = regionRepFactory.apply(this);
        this.hotRegionRepository_ = new HotRegionRepository(this.cache_, this.delegateRegionRepository_, this.config_.cellSize);
        this.regionRepository_ = new MainRegionRepository(new HotRegionRepositoryWrapper(this.hotRegionRepository_), this.delegateRegionRepository_, this.plugin_.getServer().getPluginManager(), this.cache_);

        this.plugin_.getServer().getPluginManager().registerEvents(this.hotRegionRepository_, this.plugin_);

        this.hotRegionRepository_.onLoadedRegion((r, c) -> callEventTask(new RegionLoadEvent(r, c)));
        this.hotRegionRepository_.onUnloadedRegion((r, c) -> callEventTask(new RegionUnloadEvent(r, c)));
    }
    public RegionContext(RegionContextConfig config, Plugin plugin, Function<RegionContext, RegionRepository> regionRepFactory, Function<RegionContext, HierarchyRepository> hierarchyRepFactory) {
        this(plugin.getName(), config, plugin, regionRepFactory, hierarchyRepFactory);
    }
    public RegionContext(String namespace, RegionContextConfig config, Plugin plugin, RegionRepository regionRep, HierarchyRepository hierarchyRep) {
        this(namespace, config, plugin, rc -> regionRep, rc -> hierarchyRep);
    }
    public RegionContext(RegionContextConfig config, Plugin plugin, RegionRepository regionRep, HierarchyRepository hierarchyRep) {
        this(plugin.getName(), config, plugin, rc -> regionRep, rc -> hierarchyRep);
    }


    //GETTERS
    public String getNamespace() {
        return this.namespace_;
    }
    public RegionContextConfig getConfig() {
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
    public RegionReadRepository getHotRegionRepository() {
        return this.hotRegionRepository_;
    }
    public HierarchyRepository getHierarchyRepository() {
        return this.hierarchyRepository_;
    }
    public RegionCache getCache() {
        return this.cache_;
    }


    //API
    public void callEvent(Event event) {
        this.plugin_.getServer().getPluginManager().callEvent(event);
    }
    public void registerAbility(Ability ability) {
        this.abilityRegistry_.register(ability);
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
    public Region create(@Nullable Entity creator, BoundingBox initialBox, World world, String name, Hierarchy hierarchy) {
        Hierarchy existent = this.hierarchyRepository_.get(hierarchy.getId()).orElse(null);
        if (existent == null) {
            throw new IllegalArgumentException("Hierarchy '" + hierarchy.getName() + "', id: " + hierarchy.getId() + ", is nonexistent in this region context");
        }
        Region reg = new Region(this, initialBox, world, name, existent);
        this.save(creator, reg);
        return  reg;
    }
    public Region create(BoundingBox initialBox, World world, String name, Hierarchy hierarchy) {
        return this.create(null, initialBox, world, name, hierarchy);
    }


    //IMPLEMENTATION
    @Override public List<Region> get(Query query) {
        return this.regionRepository_.get(query);
    }
    @Override public void save(@Nullable Entity doer, Region... regions) {
        this.regionRepository_.save(doer, regions);
    }


    //HELPERS
    private void callEventTask(Event event) {
        this.getServer().getScheduler().runTaskLater(
                this.plugin_,
                () -> this.getServer().getPluginManager().callEvent(event),
                1);
    }
}
