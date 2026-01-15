package com.kntrel.mc.regionLib.region.ability;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.event.BlockRightClickedEvent;
import com.kntrel.mc.regionLib.event.CopperBlockInteractEvent;
import com.kntrel.mc.regionLib.region.react.EnumBuilder;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
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
    private static final EnumBuilder<BlockRightClickedEvent, Material, Ability> RIGHT_CLICKED_WITH_ITEM = Ability.on(BlockRightClickedEvent.class)
            .when(e -> e.getItem() != null)
            .by(BlockRightClickedEvent::getPlayer)
            .at(BlockRightClickedEvent::getClickedLocation)
            .withEnum(e -> e.getItem().getType());
    private static final EnumBuilder<BlockRightClickedEvent, Material, Ability> RIGHT_CLICKED_BLOCK = Ability.on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock()))
            .withEnum(e -> e.getBlock().getType());
    private static final EnumBuilder<HangingBreakByEntityEvent, EntityType, Ability> HANGING_BROKEN = Ability.on(HangingBreakByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getRemover()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());
    private static final EnumBuilder<EntityDamageByEntityEvent, EntityType, Ability> ENTITY_DAMAGED = Ability.on(EntityDamageByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());
    private static final EnumBuilder<PlayerBucketFillEvent, Material, Ability> BUCKET_FILLED = Ability.on(PlayerBucketFillEvent.class)
            .by(PlayerBucketFillEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock()))
            .withEnum(e -> e.getBlock().getType());
    private static final EnumBuilder<PlayerBucketEmptyEvent, Material, Ability> BUCKET_EMPTIED = Ability.on(PlayerBucketEmptyEvent.class)
            .by(PlayerBucketEmptyEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlockClicked().getRelative(e.getBlockFace())))
            .withEnum(PlayerBucketEvent::getBucket);
    private static final EnumBuilder<EntityPlaceEvent, EntityType, Ability> ENTITY_PLACED = Ability.on(EntityPlaceEvent.class)
            .by(EntityPlaceEvent::getPlayer)
            .in(e -> new Area(e.getEntity().getBoundingBox(), e.getEntity().getWorld()))
            .withEnum(e -> e.getEntity().getType());
    private static final EnumBuilder<HangingPlaceEvent, EntityType, Ability> HANGING_PLACED = Ability.on(HangingPlaceEvent.class)
            .by(HangingPlaceEvent::getPlayer)
            .in(e -> new Area(e.getEntity().getBoundingBox(), e.getEntity().getWorld()))
            .withEnum(e -> e.getEntity().getType());
    
    
    //BlocBreakEvent
    @DeclareAbility
    public static final Ability
    BREAK_BLOCKS = Ability.on(BlockBreakEvent.class).by(BlockBreakEvent::getPlayer).prioritize(-1).build(),
    BREAK_CROPS = Ability.on(BlockBreakEvent.class).when(e -> Constants.PLANTABLE_BLOCKS.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).build(),
    BREAK_REDSTONE = Ability.on(BlockBreakEvent.class).when(e -> Constants.BREACKABLE_REDSTONE_BLOCKS.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).build(),
    EXTINGUISH_FIRE = Ability.on(BlockBreakEvent.class).when(e -> e.getBlock().getType().equals(Material.FIRE)).by(BlockBreakEvent::getPlayer).build();


    //BlockPlaceEvent
    @DeclareAbility
    public static final Ability
    PLACE_BLOCKS = Ability.on(BlockPlaceEvent.class).by(BlockPlaceEvent::getPlayer).prioritize(-1).build(),
    PLANT = Ability.on(BlockPlaceEvent.class).when(e -> Constants.PLANTABLE_BLOCKS.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).build(),
    PLACE_REDSTONE = Ability.on(BlockPlaceEvent.class).when(e -> Constants.BREACKABLE_REDSTONE_BLOCKS.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).build();


    //BlockRightClickedEvent
    @DeclareAbility
    public static final Ability RIGHT_CLICK_BLOCKS = Ability.on(BlockRightClickedEvent.class).by(BlockRightClickedEvent::getPlayer).prioritize(-1).build(),
    ACCESS_FURNACES = RIGHT_CLICKED_BLOCK.build(Material.FURNACE),
    ACCESS_BLAST_FURNACES = RIGHT_CLICKED_BLOCK.build(Material.BLAST_FURNACE),
    ACCESS_SMOKERS = RIGHT_CLICKED_BLOCK.build(Material.SMOKER),
    ACCESS_CRAFTING_TABLES = RIGHT_CLICKED_BLOCK.build(Material.CRAFTING_TABLE),
    ACCESS_CARTOGRAPHY_TABLES = RIGHT_CLICKED_BLOCK.build(Material.CARTOGRAPHY_TABLE),
    ACCESS_SMITHING_TABLES = RIGHT_CLICKED_BLOCK.build(Material.SMITHING_TABLE),
    ACCESS_ENCHANTING_TABLES = RIGHT_CLICKED_BLOCK.build(Material.ENCHANTING_TABLE),
    ACCESS_FLETCHING_TABLES = RIGHT_CLICKED_BLOCK.build(Material.FLETCHING_TABLE),
    ACCESS_STONECUTTERS = RIGHT_CLICKED_BLOCK.build(Material.STONECUTTER),
    ACCESS_ANVILS = RIGHT_CLICKED_BLOCK.build(Material.ANVIL),
    ACCESS_GRINDSTONES = RIGHT_CLICKED_BLOCK.build(Material.GRINDSTONE),
    ACCESS_BREWING_STANDS = RIGHT_CLICKED_BLOCK.build(Material.BREWING_STAND),
    ACCESS_LOOMS = RIGHT_CLICKED_BLOCK.build(Material.LOOM),
    ACCESS_LECTERNS = RIGHT_CLICKED_BLOCK.build(Material.LECTERN),
    RING_BELLS = RIGHT_CLICKED_BLOCK.build(Material.BELL),
    USE_RESPAWN_ANCHORS = RIGHT_CLICKED_BLOCK.build(Material.RESPAWN_ANCHOR),
    PULL_LEVERS = RIGHT_CLICKED_BLOCK.build(Material.LEVER),
    CLICK_NOTE_BLOCKS = RIGHT_CLICKED_BLOCK.build(Material.NOTE_BLOCK),
    CLICK_JUKEBOXES = RIGHT_CLICKED_BLOCK.build(Material.JUKEBOX),
    OPEN_BARRELS = RIGHT_CLICKED_BLOCK.build(Material.BARREL),
    PUT_BOOKS_ON_LECTERNS =
            Ability.on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .when(e -> {
                if (!e.getItem().getType().toString().contains("BOOK")) { return false; }
                return !((Lectern) e.getBlock().getBlockData()).hasBook();
            })
            .prioritize(1)
            .extend(ACCESS_LECTERNS)
            .build(),
    OPEN_CHESTS = RIGHT_CLICKED_BLOCK.buildOr(Material.CHEST, Material.TRAPPED_CHEST),
    OPEN_DOORS = RIGHT_CLICKED_BLOCK.build(Tag.WOODEN_DOORS::isTagged),
    OPEN_TRAPDOORS = RIGHT_CLICKED_BLOCK.build(Tag.TRAPDOORS::isTagged),
    OPEN_FENCE_GATES = RIGHT_CLICKED_BLOCK.build(Tag.FENCE_GATES::isTagged),
    PRESS_BUTTONS = RIGHT_CLICKED_BLOCK.build(Tag.BUTTONS::isTagged),
    IGNITE = RIGHT_CLICKED_WITH_ITEM.build(Material.FLINT_AND_STEEL),
    IGNITE_TNT = Ability.on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .extend(IGNITE)
            .when(e -> e.getBlock().getType().equals(Material.TNT))
            .in(e -> Area.ofBlock(e.getBlock()))
            .build();


    //PlayerTakeLecternBookEvent
    @DeclareAbility
    public static final Ability TAKE_BOOKS_FROM_LECTERNS = Ability.on(PlayerTakeLecternBookEvent.class).in(e -> Area.ofBlock(e.getLectern().getBlock())).build();


    //CooperBlockInteractEvent
    @DeclareAbility
    public static final Ability
    WAX_COPPER = Ability.on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.WAX)).build(),
    SCRAP_COPPER = Ability.on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.SCRAP)).build();


    //EntityDamageByEntityEvent
    @DeclareAbility
    public static final Ability
    BREAK_ARMOR_STANDS = ENTITY_DAMAGED.build(EntityType.ARMOR_STAND),
    PICK_FROM_ITEM_FRAMES = ENTITY_DAMAGED.build(EntityType.ITEM_FRAME),
    PICK_FROM_GLOW_FRAMES = ENTITY_DAMAGED.build(EntityType.GLOW_ITEM_FRAME),
    DAMAGE_VILLAGERS = ENTITY_DAMAGED.build(EntityType.VILLAGER),
    DAMAGE_ANIMALS = Ability.on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Animals).build(),
    DAMAGE_MONSTERS = Ability.on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Monster).build();


    //PlayerInteractEntityEvent
    @DeclareAbility
    public static final Ability
    INTERACT_WITH_ITEM_FRAMES = Ability.on(PlayerInteractEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.ITEM_FRAME))
            .at(e -> e.getRightClicked().getLocation())
            .build(),
    INTERACT_WITH_GLOW_ITEM_FRAMES = Ability.on(PlayerInteractEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.GLOW_ITEM_FRAME))
            .at(e -> e.getRightClicked().getLocation())
            .build(),
    PUT_INTO_ITEM_FRAMES = Ability.on(PlayerInteractEntityEvent.class)
            .extend(Abilities.INTERACT_WITH_ITEM_FRAMES)
            .when(e -> ((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR))
            .at(e -> e.getRightClicked().getLocation())
            .build(),
    PUT_INTO_GLOW_ITEM_FRAMES = Ability.on(PlayerInteractEntityEvent.class)
            .extend(Abilities.INTERACT_WITH_GLOW_ITEM_FRAMES)
            .when(e -> ((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR))
            .at(e -> e.getRightClicked().getLocation())
            .build();


    //EntityPlaceEvent
    @DeclareAbility
    public static final Ability
    PLACE_ARMOR_STANDS = ENTITY_PLACED.build(EntityType.ARMOR_STAND),
    PLACE_BOATS = ENTITY_PLACED.build(t -> t.toString().endsWith("BOAT"));


    //HangingPlacedEvent
    @DeclareAbility
    public static final Ability
    PLACE_PAINTINGS = HANGING_PLACED.build(EntityType.PAINTING),
    PLACE_ITEM_FRAMES = HANGING_PLACED.build(EntityType.ITEM_FRAME),
    PLACE_GLOW_ITEM_FRAMES = HANGING_PLACED.build(EntityType.GLOW_ITEM_FRAME);


    //HangingBreakByEntityEvent
    @DeclareAbility
    public static final Ability
    BREAK_PAINTINGS = HANGING_BROKEN.build(EntityType.PAINTING),
    BREAK_ITEM_FRAMES = HANGING_BROKEN.build(EntityType.ITEM_FRAME),
    BREAK_GLOW_ITEM_FRAMES = HANGING_BROKEN.build(EntityType.GLOW_ITEM_FRAME);


    //PlayerBucketEmptyEvent
    @DeclareAbility
    public static final Ability
    PUT_WATER = BUCKET_EMPTIED.build(Material.WATER_BUCKET),
    PUT_LAVA = BUCKET_EMPTIED.build(Material.LAVA_BUCKET);


    //PlayerBucketFillEvent
    @DeclareAbility
    public static final Ability
    TAKE_LAVA = BUCKET_FILLED.build(Material.LAVA),
    TAKE_WATER = BUCKET_FILLED.build(Material.WATER),
    TAKE_INFINITE_WATER = Ability.on(PlayerBucketFillEvent.class)
            .extend(TAKE_WATER)
            .when(e -> Stream.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
                    .map(f -> e.getBlock().getRelative(f))
                    .filter(b -> b.getType().equals(Material.WATER))
                    .map(Block::getBlockData)
                    .filter(b -> b instanceof Levelled l && l.getLevel() == 0)
                    .count() > 1)
            .in(e -> Area.ofBlock(e.getBlockClicked().getRelative(e.getBlockFace())))
            .prioritize(1)
            .build();


    //PlayerInteractAtEntityEvent
    @DeclareAbility
    public static final Ability INTERACT_WITH_ARMOR_STANDS = Ability.on(PlayerInteractAtEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.ARMOR_STAND))
            .at(e -> e.getRightClicked().getLocation())
            .build();


    //PlayerBedEnterEvent
    @DeclareAbility
    public static final Ability USE_BEDS = Ability.on(PlayerBedEnterEvent.class)
            .in(e -> Area.ofBlock(e.getBed()))
            .build();


    //PlayerTeleportEvent
    @DeclareAbility
    public static final Ability
    TELEPORT_IN = Ability.on(PlayerTeleportEvent.class).when(e -> VALID_TELEPORT_CAUSES.contains(e.getCause())).at(PlayerMoveEvent::getTo).build(),
    TELEPORT_OUT = Ability.on(PlayerTeleportEvent.class).extend(TELEPORT_IN).at(PlayerMoveEvent::getFrom).build();
}