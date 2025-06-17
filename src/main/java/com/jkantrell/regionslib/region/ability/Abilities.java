package com.jkantrell.regionslib.region.ability;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.event.BlockRightClickedEvent;
import com.jkantrell.regionslib.event.CopperBlockInteractEvent;
import com.jkantrell.regionslib.event.LiquidRemoveEvent;
import com.jkantrell.regionslib.region.react.EnumBuilder;
import com.jkantrell.regionslib.util.Area;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.block.data.type.Lectern;
import java.util.Set;
import java.util.function.Function;

/**
 * This class provides a set of built-in abilities.
 */
public final class Abilities {

    //PRIVATE CONSTRUCTOR
    private Abilities() {
        throw new AssertionError("You cannot instantiate the Abilities class!");
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
            .by(BlockRightClickedEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock().getRelative(e.getBlockFace())))
            .withEnum(e -> e.getItem().getType());
    private static final EnumBuilder<BlockRightClickedEvent, Material, Ability> RIGHT_CLICKED_BLOCK = Ability.on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .in(e -> Area.ofBlock(e.getBlock()))
            .withEnum(e -> e.getBlock().getType());
    private static final EnumBuilder<LiquidRemoveEvent, LiquidRemoveEvent.Type, Ability> REMOVED_LIQUID = Ability.on(LiquidRemoveEvent.class)
            .by(LiquidRemoveEvent::getPlayer)
            .at(e -> e.getBlock().getLocation().add(.5,.5,.5))
            .withEnum(LiquidRemoveEvent::getType);
    private static final EnumBuilder<HangingBreakByEntityEvent, EntityType, Ability> HANGING_BREAK = Ability.on(HangingBreakByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getRemover()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());
    private static final EnumBuilder<EntityDamageByEntityEvent, EntityType, Ability> ENTITY_DAMAGED = Ability.on(EntityDamageByEntityEvent.class)
            .by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager()))
            .at(e -> e.getEntity().getLocation())
            .withEnum(e -> e.getEntity().getType());


    //BlocBreakEvent
    @DeclareAbility
    public final static Ability
    BREAK_BLOCKS = Ability.on(BlockBreakEvent.class).by(BlockBreakEvent::getPlayer).prioritize(-1).build(),
    BREAK_CROPS = Ability.on(BlockBreakEvent.class).when(e -> RegionsLib.CONFIG.plantableBlocks.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).build(),
    BREAK_REDSTONE = Ability.on(BlockBreakEvent.class).when(e -> RegionsLib.CONFIG.breakableRedstoneBlocks.contains(e.getBlock().getType())).by(BlockBreakEvent::getPlayer).build(),
    EXTINGUISH_FIRE = Ability.on(BlockBreakEvent.class).when(e -> e.getBlock().getType().equals(Material.FIRE)).by(BlockBreakEvent::getPlayer).build();

    //BlockPlaceEvent
    @DeclareAbility
    public final static Ability
    PLACE_BLOCKS = Ability.on(BlockPlaceEvent.class).by(BlockPlaceEvent::getPlayer).prioritize(-1).build(),
    PLANT = Ability.on(BlockPlaceEvent.class).when(e -> RegionsLib.CONFIG.plantableBlocks.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).build(),
    PLACE_REDSTONE = Ability.on(BlockPlaceEvent.class).when(e -> RegionsLib.CONFIG.breakableRedstoneBlocks.contains(e.getBlock().getType())).by(BlockPlaceEvent::getPlayer).build();


    //BlockRightClickedEvent
    @DeclareAbility
    public final static Ability RIGHT_CLICK_BLOCKS = Ability.on(BlockRightClickedEvent.class).by(BlockRightClickedEvent::getPlayer).prioritize(-1).build(),
    PLACE_ITEM_FRAMES = RIGHT_CLICKED_WITH_ITEM.build(Material.ITEM_FRAME),
    PLACE_GLOW_ITEM_FRAMES = RIGHT_CLICKED_WITH_ITEM.build(Material.GLOW_ITEM_FRAME),
    PLACE_PAINTINGS = RIGHT_CLICKED_WITH_ITEM.build(Material.PAINTING),
    PLACE_ARMOR_STANDS = RIGHT_CLICKED_WITH_ITEM.build(Material.ARMOR_STAND),
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
    OPEN_DOORS = RIGHT_CLICKED_BLOCK.build(m -> m.toString().contains("DOOR")),
    OPEN_TRAPDOORS = RIGHT_CLICKED_BLOCK.build(m -> m.toString().contains("TRAPDOOR")),
    OPEN_FENCE_GATES = RIGHT_CLICKED_BLOCK.build(m -> m.toString().contains("FENCE_GATE")),
    PUT_WATER = RIGHT_CLICKED_WITH_ITEM.build(Material.WATER_BUCKET),
    PUT_LAVA = RIGHT_CLICKED_WITH_ITEM.build(Material.LAVA_BUCKET),
    PRESS_BUTTONS = RIGHT_CLICKED_BLOCK.build(m -> m.toString().contains("BUTTON")),
    IGNITE = RIGHT_CLICKED_WITH_ITEM.build(Material.FLINT_AND_STEEL),
    IGNITE_TNT = Ability.on(BlockRightClickedEvent.class)
            .by(BlockRightClickedEvent::getPlayer)
            .extend(IGNITE)
            .when(e -> e.getBlock().getType().equals(Material.TNT))
            .in(e -> Area.ofBlock(e.getBlock()))
            .build(),
    USE_BEDS = RIGHT_CLICKED_BLOCK.build(m -> m.toString().contains("BED"));

    //LiquidRemoveEvent
    @DeclareAbility
    public final static Ability
    TAKE_LAVA = REMOVED_LIQUID.build(LiquidRemoveEvent.Type.LAVA),
    TAKE_WATER = REMOVED_LIQUID.build(LiquidRemoveEvent.Type.WATER),
    TAKE_INFINITE_WATER = REMOVED_LIQUID.build(LiquidRemoveEvent.Type.INFINITE_WATER);

    //PlayerTakeLecternBookEvent
    @DeclareAbility
    public final static Ability TAKE_BOOKS_FROM_LECTERNS = Ability.on(PlayerTakeLecternBookEvent.class).in(e -> Area.ofBlock(e.getLectern().getBlock())).build();

    //CooperBlockInteractEvent
    @DeclareAbility
    public final static Ability
    WAX_COPPER = Ability.on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.WAX)).build(),
    SCRAP_COPPER = Ability.on(CopperBlockInteractEvent.class).by(CopperBlockInteractEvent::getPlayer).when(e -> e.getAction().equals(CopperBlockInteractEvent.Action.SCRAP)).build();

    //HangingBreakByEntityEvent
    @DeclareAbility
    public final static Ability
    BREAK_PAINTINGS = HANGING_BREAK.build(EntityType.PAINTING),
    BREAK_ITEM_FRAMES = HANGING_BREAK.build(EntityType.ITEM_FRAME),
    BREAK_GLOW_ITEM_FRAMES = HANGING_BREAK.build(EntityType.GLOW_ITEM_FRAME);

    //EntityDamageByEntityEvent
    @DeclareAbility
    public final static Ability
    BREAK_ARMOR_STANDS = ENTITY_DAMAGED.build(EntityType.ARMOR_STAND),
    PICK_FROM_ITEM_FRAMES = ENTITY_DAMAGED.build(EntityType.ITEM_FRAME),
    PICK_FROM_GLOW_FRAMES = ENTITY_DAMAGED.build(EntityType.GLOW_ITEM_FRAME),
    DAMAGE_VILLAGERS = ENTITY_DAMAGED.build(EntityType.VILLAGER),
    DAMAGE_ANIMALS = Ability.on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Animals).build(),
    DAMAGE_MONSTERS = Ability.on(EntityDamageByEntityEvent.class).by(e -> DAMAGER_PLAYER_GETTER.apply(e.getDamager())).when(e -> e.getEntity() instanceof Monster).build();

    //PlayerInteractEntityEvent
    @DeclareAbility
    public final static Ability
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

    //PlayerInteractAtEntityEvent
    @DeclareAbility
    public final static Ability INTERACT_WITH_ARMOR_STANDS = Ability.on(PlayerInteractAtEntityEvent.class)
            .when(e -> e.getRightClicked().getType().equals(EntityType.ARMOR_STAND))
            .at(e -> e.getRightClicked().getLocation())
            .build();


    //PlayerTeleportEvent
    @DeclareAbility
    public final static Ability
    TELEPORT_IN = Ability.on(PlayerTeleportEvent.class).when(e -> VALID_TELEPORT_CAUSES.contains(e.getCause())).at(PlayerMoveEvent::getTo).build(),
    TELEPORT_OUT = Ability.on(PlayerTeleportEvent.class).extend(TELEPORT_IN).at(PlayerMoveEvent::getFrom).build();
}