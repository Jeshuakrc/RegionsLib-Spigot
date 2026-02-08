package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.event.CopperBlockInteractEvent;
import com.kntrel.mc.regionLib.trigger.build.EnumTriggerBuilder;
import com.kntrel.mc.regionLib.util.Area;
import com.kntrel.util.Priority;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.block.data.type.Lectern;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.kntrel.mc.regionLib.region.ability.Ability.*;

/**
 * This class provides a set of built-in abilities.
 */
public final class Abilities {

    //PRIVATE CONSTRUCTOR
    private Abilities() {
        throw new AssertionError("You cannot instantiate the Abilities class!");
    }


    //CONSTANTS
    public static class Constants {

        private static Set<Material> PLANTABLE_BLOCKS = Set.of(
                Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.KELP, Material.BAMBOO_SAPLING,
                Material.SUGAR_CANE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.NETHER_WART, Material.ACACIA_SAPLING,
                Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.DARK_OAK_SAPLING, Material.JUNGLE_SAPLING, Material.OAK_SAPLING
        );
        private static Set<Material> BREACKABLE_REDSTONE_BLOCKS = Set.of(Material.REDSTONE_WIRE);


        public static void setPlantableBlocks(List<Material> materials) {
            PLANTABLE_BLOCKS = Set.copyOf(materials);
        }
        public static Set<Material> getPlantableBlocks() {
            return PLANTABLE_BLOCKS;
        }
    }


    //HELPERS
    private static final Function<Entity, Player> DAMAGER_PLAYER_GETTER = e -> {
        if (e instanceof Player player) {
            return player;
        }
        if (e instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    };
    private final static Set<PlayerTeleportEvent.TeleportCause> VALID_TELEPORT_CAUSES = Set.of(
            PlayerTeleportEvent.TeleportCause.COMMAND, PlayerTeleportEvent.TeleportCause.END_PORTAL, PlayerTeleportEvent.TeleportCause.END_GATEWAY,
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, PlayerTeleportEvent.TeleportCause.PLUGIN
    );
    private static final EnumTriggerBuilder<?, Material, AbilityBuilder<BlockRightClickedEvent>> RIGHT_CLICKED_WITH_ITEM = on(BlockRightClickedEvent.class)
            .when(e -> e.getItem() != null)
            .by(BlockRightClickedEvent::getPlayer)
            .at(BlockRightClickedEvent::getClickedLocation)
            .withEnum(e -> e.getItem().getType());
    private static final EnumTriggerBuilder<?, Material, AbilityBuilder<BlockRightClickedEvent>> RIGHT_CLICKED_BLOCK = on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock()))
            .withEnum(e -> e.getBlock().getType());
    private static final EnumTriggerBuilder<?, EntityType, AbilityBuilder<HangingBreakByEntityEvent>> HANGING_BROKEN = on(HangingBreakByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getRemover()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());
    private static final EnumTriggerBuilder<?, EntityType, AbilityBuilder<EntityDamageByEntityEvent>> ENTITY_DAMAGED = on(EntityDamageByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());
    private static final EnumTriggerBuilder<?, Material, AbilityBuilder<PlayerBucketFillEvent>> BUCKET_FILLED = on(PlayerBucketFillEvent.class)
            .by(PlayerBucketFillEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock()))
            .withEnum(e -> e.getBlock().getType());
    private static final EnumTriggerBuilder<?, Material, AbilityBuilder<PlayerBucketEmptyEvent>> BUCKET_EMPTIED = on(PlayerBucketEmptyEvent.class)
            .by(PlayerBucketEmptyEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlockClicked().getRelative(e.getBlockFace())))
            .withEnum(PlayerBucketEvent::getBucket);
    private static final EnumTriggerBuilder<?, EntityType, AbilityBuilder<EntityPlaceEvent>> ENTITY_PLACED = on(EntityPlaceEvent.class)
            .by(EntityPlaceEvent::getPlayer)
            .in(e -> new Area(e.getEntity().getBoundingBox(), e.getEntity().getWorld()))
            .withEnum(e -> e.getEntity().getType());
    private static final EnumTriggerBuilder<?, EntityType, AbilityBuilder<HangingPlaceEvent>> HANGING_PLACED = on(HangingPlaceEvent.class)
            .by(HangingPlaceEvent::getPlayer)
            .in(e -> new Area(e.getEntity().getBoundingBox(), e.getEntity().getWorld()))
            .withEnum(e -> e.getEntity().getType());
    
    
    //BlocBreakEvent
    @DeclareAbility
    public static final Ability BREAK_BLOCKS =
        on(BlockBreakEvent.class)
            .by(BlockBreakEvent::getPlayer)
            .prioritize(Priority.LOWEST)
        .alsoOn(BlockDamageEvent.class)
            .by(BlockDamageEvent::getPlayer)
            .prioritize(Priority.LOWEST)
        .done();

    @DeclareAbility
    public static final Ability
    BREAK_CROPS = on(BlockBreakEvent.class).when(e -> Constants.PLANTABLE_BLOCKS.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).done(),
    BREAK_REDSTONE = on(BlockBreakEvent.class).when(e -> Constants.BREACKABLE_REDSTONE_BLOCKS.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).done(),
    EXTINGUISH_FIRE = on(BlockBreakEvent.class).when(e -> e.getBlock().getType().equals(Material.FIRE)).by(BlockBreakEvent::getPlayer).done();


    //BlockPlaceEvent
    @DeclareAbility
    public static final Ability
    PLACE_BLOCKS = on(BlockPlaceEvent.class).by(BlockPlaceEvent::getPlayer).prioritize(Priority.LOWEST).done(),
    PLANT = on(BlockPlaceEvent.class).when(e -> Constants.PLANTABLE_BLOCKS.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).done(),
    PLACE_REDSTONE = on(BlockPlaceEvent.class).when(e -> Constants.BREACKABLE_REDSTONE_BLOCKS.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).done();


    //BlockRightClickedEvent
    @DeclareAbility
    public static final Ability RIGHT_CLICK_BLOCKS = on(BlockRightClickedEvent.class).by(BlockRightClickedEvent::getPlayer).prioritize(Priority.LOWEST).done(),
    ACCESS_FURNACES = RIGHT_CLICKED_BLOCK.whenIs(Material.FURNACE).done(),
    ACCESS_BLAST_FURNACES = RIGHT_CLICKED_BLOCK.whenIs(Material.BLAST_FURNACE).done(),
    ACCESS_SMOKERS = RIGHT_CLICKED_BLOCK.whenIs(Material.SMOKER).done(),
    ACCESS_CRAFTING_TABLES = RIGHT_CLICKED_BLOCK.whenIs(Material.CRAFTING_TABLE).done(),
    ACCESS_CARTOGRAPHY_TABLES = RIGHT_CLICKED_BLOCK.whenIs(Material.CARTOGRAPHY_TABLE).done(),
    ACCESS_SMITHING_TABLES = RIGHT_CLICKED_BLOCK.whenIs(Material.SMITHING_TABLE).done(),
    ACCESS_ENCHANTING_TABLES = RIGHT_CLICKED_BLOCK.whenIs(Material.ENCHANTING_TABLE).done(),
    ACCESS_FLETCHING_TABLES = RIGHT_CLICKED_BLOCK.whenIs(Material.FLETCHING_TABLE).done(),
    ACCESS_STONECUTTERS = RIGHT_CLICKED_BLOCK.whenIs(Material.STONECUTTER).done(),
    ACCESS_ANVILS = RIGHT_CLICKED_BLOCK.whenIs(Material.ANVIL).done(),
    ACCESS_GRINDSTONES = RIGHT_CLICKED_BLOCK.whenIs(Material.GRINDSTONE).done(),
    ACCESS_BREWING_STANDS = RIGHT_CLICKED_BLOCK.whenIs(Material.BREWING_STAND).done(),
    ACCESS_LOOMS = RIGHT_CLICKED_BLOCK.whenIs(Material.LOOM).done(),
    ACCESS_LECTERNS = RIGHT_CLICKED_BLOCK.whenIs(Material.LECTERN).done(),
    RING_BELLS = RIGHT_CLICKED_BLOCK.whenIs(Material.BELL).done(),
    USE_RESPAWN_ANCHORS = RIGHT_CLICKED_BLOCK.whenIs(Material.RESPAWN_ANCHOR).done(),
    PULL_LEVERS = RIGHT_CLICKED_BLOCK.whenIs(Material.LEVER).done(),
    CLICK_NOTE_BLOCKS = RIGHT_CLICKED_BLOCK.whenIs(Material.NOTE_BLOCK).done(),
    CLICK_JUKEBOXES = RIGHT_CLICKED_BLOCK.whenIs(Material.JUKEBOX).done(),
    OPEN_BARRELS = RIGHT_CLICKED_BLOCK.whenIs(Material.BARREL).done(),
    PUT_BOOKS_ON_LECTERNS =
            on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .when(e -> {
                if (!e.getItem().getType().toString().contains("BOOK")) { return false; }
                return !((Lectern) e.getBlock().getBlockData()).hasBook();
            })
            .prioritize(1)
            .extend(ACCESS_LECTERNS)
            .done(),
    OPEN_CHESTS = RIGHT_CLICKED_BLOCK.whenIsAnyOf(Material.CHEST, Material.TRAPPED_CHEST).done(),
    OPEN_DOORS = RIGHT_CLICKED_BLOCK.when(Tag.WOODEN_DOORS::isTagged).done(),
    OPEN_TRAPDOORS = RIGHT_CLICKED_BLOCK.when(Tag.TRAPDOORS::isTagged).done(),
    OPEN_FENCE_GATES = RIGHT_CLICKED_BLOCK.when(Tag.FENCE_GATES::isTagged).done(),
    PRESS_BUTTONS = RIGHT_CLICKED_BLOCK.when(Tag.BUTTONS::isTagged).done(),
    IGNITE = RIGHT_CLICKED_WITH_ITEM.whenIs(Material.FLINT_AND_STEEL).done(),
    IGNITE_TNT = on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .extend(IGNITE)
            .when(e -> e.getBlock().getType().equals(Material.TNT))
            .in(e -> Area.ofBlock(e.getBlock()))
            .done();


    //PlayerTakeLecternBookEvent
    @DeclareAbility
    public static final Ability TAKE_BOOKS_FROM_LECTERNS = on(PlayerTakeLecternBookEvent.class).in(e -> Area.ofBlock(e.getLectern().getBlock())).done();


    //CooperBlockInteractEvent
    @DeclareAbility
    public static final Ability
    WAX_COPPER = on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.WAX)).done(),
    SCRAP_COPPER = on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.SCRAP)).done();


    //EntityDamageByEntityEvent
    @DeclareAbility
    public static final Ability
    BREAK_ARMOR_STANDS = ENTITY_DAMAGED.whenIs(EntityType.ARMOR_STAND).done(),
    PICK_FROM_ITEM_FRAMES = ENTITY_DAMAGED.whenIs(EntityType.ITEM_FRAME).done(),
    PICK_FROM_GLOW_FRAMES = ENTITY_DAMAGED.whenIs(EntityType.GLOW_ITEM_FRAME).done(),
    DAMAGE_VILLAGERS = ENTITY_DAMAGED.whenIs(EntityType.VILLAGER).done(),
    DAMAGE_ANIMALS = on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Animals).done(),
    DAMAGE_MONSTERS = on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Monster).done();


    //PlayerInteractEntityEvent
    @DeclareAbility
    public static final Ability
    INTERACT_WITH_ITEM_FRAMES = on(PlayerInteractEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.ITEM_FRAME))
            .at(e -> e.getRightClicked().getLocation())
            .done(),
    INTERACT_WITH_GLOW_ITEM_FRAMES = on(PlayerInteractEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.GLOW_ITEM_FRAME))
            .at(e -> e.getRightClicked().getLocation())
            .done(),
    PUT_INTO_ITEM_FRAMES = on(PlayerInteractEntityEvent.class)
            .extend(Abilities.INTERACT_WITH_ITEM_FRAMES)
            .when(e -> ((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR))
            .at(e -> e.getRightClicked().getLocation())
            .done(),
    PUT_INTO_GLOW_ITEM_FRAMES = on(PlayerInteractEntityEvent.class)
            .extend(Abilities.INTERACT_WITH_GLOW_ITEM_FRAMES)
            .when(e -> ((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR))
            .at(e -> e.getRightClicked().getLocation())
            .done();


    //EntityPlaceEvent
    @DeclareAbility
    public static final Ability
    PLACE_ARMOR_STANDS = ENTITY_PLACED.whenIs(EntityType.ARMOR_STAND).done(),
    PLACE_BOATS = ENTITY_PLACED.when(t -> t.toString().endsWith("BOAT")).done();


    //HangingPlacedEvent
    @DeclareAbility
    public static final Ability
    PLACE_PAINTINGS = HANGING_PLACED.whenIs(EntityType.PAINTING).done(),
    PLACE_ITEM_FRAMES = HANGING_PLACED.whenIs(EntityType.ITEM_FRAME).done(),
    PLACE_GLOW_ITEM_FRAMES = HANGING_PLACED.whenIs(EntityType.GLOW_ITEM_FRAME).done();


    //HangingBreakByEntityEvent
    @DeclareAbility
    public static final Ability
    BREAK_PAINTINGS = HANGING_BROKEN.whenIs(EntityType.PAINTING).done(),
    BREAK_ITEM_FRAMES = HANGING_BROKEN.whenIs(EntityType.ITEM_FRAME).done(),
    BREAK_GLOW_ITEM_FRAMES = HANGING_BROKEN.whenIs(EntityType.GLOW_ITEM_FRAME).done();


    //PlayerBucketEmptyEvent
    @DeclareAbility
    public static final Ability
    PUT_WATER = BUCKET_EMPTIED.whenIs(Material.WATER_BUCKET).done(),
    PUT_LAVA = BUCKET_EMPTIED.whenIs(Material.LAVA_BUCKET).done();


    //PlayerBucketFillEvent
    @DeclareAbility
    public static final Ability
    TAKE_LAVA = BUCKET_FILLED.whenIs(Material.LAVA).done(),
    TAKE_WATER = BUCKET_FILLED.whenIs(Material.WATER).done(),
    TAKE_INFINITE_WATER = on(PlayerBucketFillEvent.class)
            .extend(TAKE_WATER)
            .when(e -> Stream.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
                    .map(f -> e.getBlock().getRelative(f))
                    .filter(b -> b.getType().equals(Material.WATER))
                    .map(Block::getBlockData)
                    .filter(b -> b instanceof Levelled l && l.getLevel() == 0)
                    .count() > 1)
            .in(e -> Area.ofBlock(e.getBlockClicked().getRelative(e.getBlockFace())))
            .prioritize(1)
            .done();


    //PlayerInteractAtEntityEvent
    @DeclareAbility
    public static final Ability INTERACT_WITH_ARMOR_STANDS = on(PlayerInteractAtEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.ARMOR_STAND))
            .at(e -> e.getRightClicked().getLocation())
            .done();


    //PlayerBedEnterEvent
    @DeclareAbility
    public static final Ability USE_BEDS = on(PlayerBedEnterEvent.class)
            .in(e -> Area.ofBlock(e.getBed()))
            .done();


    //PlayerTeleportEvent
    @DeclareAbility
    public static final Ability
    TELEPORT_IN = on(PlayerTeleportEvent.class).when(e -> VALID_TELEPORT_CAUSES.contains(e.getCause())).at(PlayerMoveEvent::getTo).done(),
    TELEPORT_OUT = on(PlayerTeleportEvent.class).extend(TELEPORT_IN).at(PlayerMoveEvent::getFrom).done();
}