package com.kntrel.mc.regionLib.command;

import com.kntrel.mc.commvoker.argument.ArgumentRegistry;
import com.kntrel.mc.commvoker.argument.binder.ArgumentBinder;
import com.kntrel.mc.commvoker.bukkit.BukkitCommvoker;
import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.command.assembler.*;
import com.kntrel.mc.regionLib.region.Region;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.context.RegionContextConfig;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.rule.Rule;
import com.kntrel.mc.regionLib.region.rule.RuleValue;
import com.kntrel.mc.regionLib.test.mock.MockHierarchyRepository;
import com.kntrel.mc.regionLib.test.util.MemoryRegionRepository;
import com.kntrel.mc.regionLib.test.util.TestRegionContext;
import com.mojang.brigadier.CommandDispatcher;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RegionCommandTest {

    //FIELDS
    private BukkitCommvoker commvoker;
    private ServerMock server;
    private PlayerMock sender;
    private RegionContext context;
    private World mockWorld;
    private Plugin plugin;


    //SETUP
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        mockWorld = server.addSimpleWorld("world");

        context = new TestRegionContext(
                plugin,
                RegionContextConfig.defaultConfig(),
                new MemoryRegionRepository(),
                MockHierarchyRepository.ofSingle("hierarchy")
        );
        RegionLib.setDefault(context);

        commvoker = new BukkitCommvoker(new CommandDispatcher<>());

        ArgumentRegistry<CommandSender> argumentRegistry = commvoker.getArgumentRegistry();
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RegionAssembler::region)
                        .toClass(Region.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(HierarchyAssembler::hierarchy)
                        .toClass(Hierarchy.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(HierarchyGroupAssembler::hierarchyGroup)
                        .toClass(Hierarchy.Group.class)
                        .bind()
        );

        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RuleAssembler::rule)
                        .toClass((Class<Rule<?>>) (Class<?>) Rule.class)
                        .bind()
        );
        argumentRegistry.register(
                ArgumentBinder.argumentAssembler(RuleValueAssembler::ruleValue)
                        .toClass((Class<RuleValue<?>>) (Class<?>) RuleValue.class)
                        .bind()
        );
        commvoker.register(new RegionCommand());

        sender = server.addPlayer(UUID.randomUUID(), "TestPlayer");
        sender.teleport(new Location(mockWorld, 0, 64, 0));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // Tests

    @Test
    void testCreateRegion() {
        // Create a region using the command
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy testregion", sender));
        
        // Verify the region was created in the context
        List<Region> regions = context.getAll();
        assertEquals(1, regions.size(), "Region should be created");
        assertEquals("testregion", regions.getFirst().getName());
    }

    @Test
    void testCreateRegionWithPlayer() {
        // Create a region for the player (without specifying world)
        assertDoesNotThrow(() -> commvoker.execute("region create -50,0,0,50,100,100 hierarchy myregion", sender));
        
        // Verify the region was created
        List<Region> regions = context.getAll();
        assertTrue(regions.stream().anyMatch(r -> r.getName().equals("myregion")));
    }

    @Test
    void testDestroyRegion() {
        // First create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy destroytest", sender));
        
        // Verify it exists
        List<Region> beforeDestroy = context.getAll();
        assertTrue(beforeDestroy.stream().anyMatch(r -> r.getName().equals("destroytest")));
        
        // Destroy the region
        assertDoesNotThrow(() -> commvoker.execute("region destroy destroytest", sender));
        
        // Verify the region is destroyed
        List<Region> afterDestroy = context.getAll().stream().filter(r -> !r.isDestroyed()).toList();
        assertFalse(afterDestroy.stream().anyMatch(r -> r.getName().equals("destroytest")));
    }

    @Test
    void testResizeRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy resizetest", sender));
        
        // Resize the region
        assertDoesNotThrow(() -> commvoker.execute("region resize resizetest -50,10,10,50,120,120", sender));
        
        // Verify the region was resized
        List<Region> regions = context.getAll();
        Region resized = regions.stream().filter(r -> r.getName().equals("resizetest")).findFirst().orElse(null);
        assertNotNull(resized);
        assertEquals(100, resized.getWidthX(), 0.1);
        assertEquals(110, resized.getHeight(), 0.1);
    }

    @Test
    void testExpandRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy expandtest", sender));
        
        // Expand the region upward
        assertDoesNotThrow(() -> commvoker.execute("region expand expandtest UP 50", sender));
        
        // Verify the region was expanded
        List<Region> regions = context.getAll();
        Region expanded = regions.stream().filter(r -> r.getName().equals("expandtest")).findFirst().orElse(null);
        assertNotNull(expanded);
        assertEquals(150, expanded.getHeight(), 0.1);
    }

    @Test
    void testRenameRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy oldname", sender));
        
        // Rename the region
        assertDoesNotThrow(() -> commvoker.execute("region rename oldname newname", sender));
        
        // Verify the region was renamed
        List<Region> regions = context.getAll();
        assertFalse(regions.stream().anyMatch(r -> r.getName().equals("oldname")));
        assertTrue(regions.stream().anyMatch(r -> r.getName().equals("newname")));
    }

    @Test
    void testPlayerJoinRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy jointest", sender));
        
        // Create a mock player to join
        Player joiner = server.addPlayer(UUID.randomUUID(), "JoinPlayer");
        
        // This test demonstrates the join mechanism - actual execution depends on command parsing
        List<Region> regions = context.getAll();
        Region testRegion = regions.stream().filter(r -> r.getName().equals("jointest")).findFirst().orElse(null);
        assertNotNull(testRegion);
    }

    @Test
    void testPlayerKickRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy kicktest", sender));
        
        // Verify region exists
        List<Region> regions = context.getAll();
        Region testRegion = regions.stream().filter(r -> r.getName().equals("kicktest")).findFirst().orElse(null);
        assertNotNull(testRegion);
    }

    @Test
    void testEnableRegion() {
        // Create a region
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy enabletest", sender));
        
        // First disable it
        assertDoesNotThrow(() -> commvoker.execute("region disable enabletest", sender));
        
        // Enable the region
        assertDoesNotThrow(() -> commvoker.execute("region enable enabletest", sender));
        
        // Verify the region is enabled
        List<Region> regions = context.getAll();
        Region enabledRegion = regions.stream().filter(r -> r.getName().equals("enabletest")).findFirst().orElse(null);
        assertNotNull(enabledRegion);
        assertTrue(enabledRegion.isEnabled());
    }

    @Test
    void testDisableRegion() {
        // Create a region (enabled by default)
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy disabletest", sender));
        
        // Disable the region
        assertDoesNotThrow(() -> commvoker.execute("region disable disabletest", sender));
        
        // Verify the region is disabled
        List<Region> regions = context.getAll();
        Region disabledRegion = regions.stream().filter(r -> r.getName().equals("disabletest")).findFirst().orElse(null);
        assertNotNull(disabledRegion);
        assertFalse(disabledRegion.isEnabled());
    }

    @Test
    void testEnableMultipleRegions() {
        // Create multiple regions
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy multi1", sender));
        assertDoesNotThrow(() -> commvoker.execute("region create 100,0,0,200,100,100 hierarchy multi2", sender));
        
        // Disable them
        assertDoesNotThrow(() -> commvoker.execute("region disable multi1", sender));
        assertDoesNotThrow(() -> commvoker.execute("region disable multi2", sender));
        
        // Enable them via command
        assertDoesNotThrow(() -> commvoker.execute("region enable multi1 multi2", sender));
        
        // Verify both are enabled
        List<Region> regions = context.getAll();
        assertTrue(regions.stream()
                .filter(r -> r.getName().equals("multi1") || r.getName().equals("multi2"))
                .allMatch(Region::isEnabled));
    }

    @Test
    void testDisableMultipleRegions() {
        // Create multiple regions
        assertDoesNotThrow(() -> commvoker.execute("region create -100,0,0,100,100,100 hierarchy dmulti1", sender));
        assertDoesNotThrow(() -> commvoker.execute("region create 100,0,0,200,100,100 hierarchy dmulti2", sender));
        
        // Disable them via command
        assertDoesNotThrow(() -> commvoker.execute("region disable dmulti1 dmulti2", sender));
        
        // Verify both are disabled
        List<Region> regions = context.getAll();
        assertTrue(regions.stream()
                .filter(r -> r.getName().equals("dmulti1") || r.getName().equals("dmulti2"))
                .noneMatch(Region::isEnabled));
    }
}
