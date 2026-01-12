package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.ability.AbilityBuilder;
import com.kntrel.mc.regionLib.region.ability.AbilityRegistry;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.display.BlockDisplayAreaDisplayer;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.region.repository.RegionRepository;
import com.kntrel.mc.regionLib.region.rule.RuleRegistry;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class RegionContext implements RegionRepository {

    //FIELDS
    private final RegionRepository regionRepository_;
    private final Plugin plugin_;
    private final AbilityRegistry abilityRegistry_;
    private final RuleRegistry ruleRegistry_;
    private final DisplayController displayController_;


    //CONSTRUCTORS
    public RegionContext(Plugin plugin, Function<RegionContext, RegionRepository> repositoryFactory) {
        this.regionRepository_ = repositoryFactory.apply(this);
        this.plugin_ = plugin;
        this.abilityRegistry_ = new AbilityRegistry(this);
        this.ruleRegistry_ = new RuleRegistry(this);
        this.displayController_ = new DisplayController(this, new BlockDisplayAreaDisplayer(this));
    }


    //GETTERS
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

    //UTILITIES
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
    @Override public HierarchyRepository getHierarchyRepository() {
        return this.regionRepository_.getHierarchyRepository();
    }

}
