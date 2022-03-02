package com.jkantrell.regionslib.regions.abilities;

import com.jkantrell.regionslib.events.BlockRightClickedEvent;
import com.jkantrell.regionslib.events.CopperBlockInteractEvent;
import com.jkantrell.regionslib.events.LiquidRemoveEvent;
import com.jkantrell.regionslib.io.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.block.data.type.Lectern;
import org.yaml.snakeyaml.constructor.ConstructorException;

/**
 * This class provides a set of built-in abilities.
 */
public final class Abilities {

    //PRIVATE CONSTRUCTOR
    private Abilities() {
        throw new AssertionError("You cannot instantiate the Abilities class!");
    }

    //BlocBreakEvent
    @AbilityRegistration
    public final static Ability<BlockBreakEvent> BREAK_BLOCKS = new Ability<>(BlockBreakEvent.class, e -> true).setPriority(-1);
    @AbilityRegistration
    public final static Ability<BlockBreakEvent> BREAK_CROPS = new Ability<>(
            BlockBreakEvent.class,
            e -> ConfigManager.getPlantableBlocks().contains(e.getBlock().getType())
    );
    @AbilityRegistration
    public final static Ability<BlockBreakEvent> BREAK_REDSTONE = new Ability<>(
            BlockBreakEvent.class,
            e -> ConfigManager.getBreakableRedstoneBlocks().contains(e.getBlock().getType())
    );
    @AbilityRegistration
    public final static Ability<BlockBreakEvent> EXTINGUISH_FIRE = new Ability<>(
            BlockBreakEvent.class,
            e -> e.getBlock().getType().equals(Material.FIRE)
    );

    //BlockPlaceEvent
    @AbilityRegistration
    public final static Ability<BlockPlaceEvent> PLACE_BLOCKS = new Ability<>(
            BlockPlaceEvent.class,
            e -> true
    ).setPriority(-1);
    @AbilityRegistration
    public final static Ability<BlockPlaceEvent> PLANT = new Ability<>(
            BlockPlaceEvent.class,
            e -> ConfigManager.getPlantableBlocks().contains(e.getBlock().getType())
    );
    @AbilityRegistration
    public final static Ability<BlockPlaceEvent> PLACE_REDSTONE = new Ability<>(
            BlockPlaceEvent.class,
            e -> ConfigManager.getBreakableRedstoneBlocks().contains(e.getBlock().getType())
    );

    //BlockRightClickedEvent
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> RIGHT_CLICK_BLOCS = new Ability<>(BlockRightClickedEvent.class,e -> true).setPriority(-1);
    private static final AbilityEnumBuilder<BlockRightClickedEvent,Material> placeEntityBuilder = new AbilityEnumBuilder<>(
            BlockRightClickedEvent.class,
            (e,m) -> e.getItem().getType().equals(m),
            BlockRightClickedEvent::getPlayer,
            e -> e.getBlock().getRelative(e.getBlockFace()).getLocation().add(.5,.5,.5)
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PLACE_ITEM_FRAMES = Abilities.placeEntityBuilder.build(Material.ITEM_FRAME);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PLACE_GLOW_ITEM_FRAMES = Abilities.placeEntityBuilder.build(Material.GLOW_ITEM_FRAME);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PLACE_PAINTINGS = Abilities.placeEntityBuilder.build(Material.PAINTING);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PLACE_ARMOR_STANDS = Abilities.placeEntityBuilder.build(Material.ARMOR_STAND);

    private static final AbilityEnumBuilder<BlockRightClickedEvent,Material> blockAccessBuilder = new AbilityEnumBuilder<>(
            BlockRightClickedEvent.class,
            (e,m) -> e.getBlock().getType().equals(m),
            BlockRightClickedEvent::getPlayer,
            e -> e.getBlock().getLocation().add(.5,.5,.5)
    );
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_FURNACES = Abilities.blockAccessBuilder.build(Material.FURNACE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_BLAST_FURNACES = Abilities.blockAccessBuilder.build(Material.BLAST_FURNACE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_SMOKERS = Abilities.blockAccessBuilder.build(Material.SMOKER);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_CRAFTING_TABLES = Abilities.blockAccessBuilder.build(Material.CRAFTING_TABLE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_CARTOGRAPHY_TABLES = Abilities.blockAccessBuilder.build(Material.CARTOGRAPHY_TABLE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_SMITHING_TABLES = Abilities.blockAccessBuilder.build(Material.SMITHING_TABLE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_ENCHANTING_TABLES = Abilities.blockAccessBuilder.build(Material.ENCHANTING_TABLE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_FLETCHING_TABLES = Abilities.blockAccessBuilder.build(Material.FLETCHING_TABLE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_STONECUTTERS = Abilities.blockAccessBuilder.build(Material.STONECUTTER);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_ANVILS = Abilities.blockAccessBuilder.build(Material.ANVIL);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_GRINDSTONES = Abilities.blockAccessBuilder.build(Material.GRINDSTONE);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_BREWING_STANDS = Abilities.blockAccessBuilder.build(Material.BREWING_STAND);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_LOOMS = Abilities.blockAccessBuilder.build(Material.LOOM);
    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent> ACCESS_LECTERNS = Abilities.blockAccessBuilder.build(Material.LECTERN);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> RING_BELLS = Abilities.blockAccessBuilder.build(Material.BELL);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> USE_RESPAWN_ANCHORS = Abilities.blockAccessBuilder.build(Material.RESPAWN_ANCHOR);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PULL_LEVERS = Abilities.blockAccessBuilder.build(Material.LEVER);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> CLICK_NOTE_BLOCKS = Abilities.blockAccessBuilder.build(Material.NOTE_BLOCK);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> CLICK_JUKEBOXES = Abilities.blockAccessBuilder.build(Material.JUKEBOX);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> OPEN_BARRELS = Abilities.blockAccessBuilder.build(Material.BARREL);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PUT_BOOKS_ON_LECTERNS = new Ability<>(
            BlockRightClickedEvent.class,
            e -> {
                if (!e.getItem().getType().toString().contains("BOOK")) { return false; }
                return !((Lectern) e.getBlock().getBlockData()).hasBook();
            }
    ).setPriority(1).invalidates(Abilities.PLACE_BLOCKS).extend(Abilities.ACCESS_LECTERNS);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> OPEN_CHESTS = new Ability<>(
            BlockRightClickedEvent.class,
            e ->
                    (e.getClickedBlock().getType().equals(Material.CHEST) || e.getClickedBlock().getType().equals(Material.TRAPPED_CHEST))
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> OPEN_DOORS = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getClickedBlock().toString().contains("DOOR")
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> OPEN_TRAPDOORS = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getClickedBlock().toString().contains("TRAPDOOR")
    ).extend(Abilities.OPEN_DOORS);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> OPEN_FENCE_GATES = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getClickedBlock().toString().contains("FENCE_GATE")
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PUT_WATER = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getItem().getType().equals(Material.WATER_BUCKET),
            BlockRightClickedEvent::getPlayer,
            e -> e.getBlock().getRelative(e.getBlockFace()).getLocation().add(.5,.5,.5)
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PUT_LAVA = new Ability<>(
            Abilities.PUT_WATER,
            e -> e.getItem().getType().equals(Material.LAVA_BUCKET)
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> PRESS_BUTTONS = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getBlock().getType().toString().contains("BUTTON")
    );
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> IGNITE = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getItem().getType().equals(Material.FLINT_AND_STEEL)
    ).invalidates(Abilities.PLACE_BLOCKS);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> IGNITE_TNT = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getBlock().getType().equals(Material.TNT)
    ).extend(Abilities.IGNITE);
    @AbilityRegistration
    public final static Ability<BlockRightClickedEvent> USE_BEDS = new Ability<>(
            BlockRightClickedEvent.class,
            e -> e.getBlock().getType().toString().contains("BED")
    );

    //LiquidRemoveEvent
    @AbilityRegistration
    public final static Ability<LiquidRemoveEvent> TAKE_LAVA = new Ability<>(
            LiquidRemoveEvent.class,
            e -> e.getType().equals(LiquidRemoveEvent.Type.LAVA)
    );
    @AbilityRegistration
    public final static Ability<LiquidRemoveEvent> TAKE_WATER = new Ability<>(
            LiquidRemoveEvent.class,
            e -> e.getType().equals(LiquidRemoveEvent.Type.WATER)
    );
    @AbilityRegistration
    public final static Ability<LiquidRemoveEvent> TAKE_INFINITE_WATER = new Ability<>(
            LiquidRemoveEvent.class,
            e -> e.getType().equals(LiquidRemoveEvent.Type.INFINITE_WATER)
    );

    //PlayerTakeLecternBookEvent
    @AbilityRegistration
    public final static Ability<PlayerTakeLecternBookEvent> TAKE_BOOKS_FROM_LECTERNS = new Ability<>(
            PlayerTakeLecternBookEvent.class,
            e -> true,
            e -> e.getLectern().getLocation().add(.5,.5,.5)
    );

    //CooperBlockInteractEvent
    @AbilityRegistration
    public final static Ability<CopperBlockInteractEvent> WAX_COPPER = new Ability<>(
            CopperBlockInteractEvent.class,
            e -> e.getAction().equals(CopperBlockInteractEvent.Action.WAX)
    ).invalidates(Abilities.PLACE_BLOCKS);
    @AbilityRegistration
    public final static Ability<CopperBlockInteractEvent> SCRAP_COPPER = new Ability<>(
            CopperBlockInteractEvent.class,
            e -> e.getAction().equals(CopperBlockInteractEvent.Action.SCRAP)
    ).invalidates(Abilities.PLACE_BLOCKS);

    //HangingBreakByEntityEvent
    private final static AbilityEnumBuilder<HangingBreakByEntityEvent,EntityType> breakHangingBuilder = new AbilityEnumBuilder<>(
            HangingBreakByEntityEvent.class,
            (e,t) -> e.getEntity().getType().equals(t),
            e -> {
                Player player = null;
                Entity remover = e.getRemover();
                if (remover instanceof Player player_) {
                    player = player_;
                }
                if (remover instanceof Projectile projectile) {
                    if (projectile.getShooter() instanceof Player player_) {
                        player = player_;
                    }
                }
                return player;
            },
            e -> e.getEntity().getLocation()
    );
    @AbilityRegistration
    public final static Ability<HangingBreakByEntityEvent> BREAK_PAINTINGS = Abilities.breakHangingBuilder.build(EntityType.PAINTING);
    @AbilityRegistration
    public final static Ability<HangingBreakByEntityEvent> BREAK_ITEM_FRAMES = Abilities.breakHangingBuilder.build(EntityType.ITEM_FRAME);
    @AbilityRegistration
    public final static Ability<HangingBreakByEntityEvent> BREAK_GLOW_ITEM_FRAMES = Abilities.breakHangingBuilder.build(EntityType.GLOW_ITEM_FRAME);

    //EntityDamageByEntityEvent
    private final static AbilityEnumBuilder<EntityDamageByEntityEvent,EntityType> entityDamageBuilder = new AbilityEnumBuilder<>(
            EntityDamageByEntityEvent.class,
            (e,t) -> e.getEntity().getType().equals(t),
            e -> {
                Player player = null;
                Entity remover = e.getDamager();
                if (remover instanceof Player player_) {
                    player = player_;
                }
                if (remover instanceof Projectile projectile) {
                    if (projectile.getShooter() instanceof Player player_) {
                        player = player_;
                    }
                }
                return player;
            },
            e -> e.getEntity().getLocation()
    );
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> BREAK_ARMOR_STANDS = Abilities.entityDamageBuilder.build("break_armor_stands",EntityType.ARMOR_STAND);
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> PICK_FROM_ITEM_FRAMES = Abilities.entityDamageBuilder.build("pick_from_item_frames",EntityType.ITEM_FRAME);
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> PICK_FROM_GLOW_FRAMES = Abilities.entityDamageBuilder.build("pick_from_glow_item_frames",EntityType.GLOW_ITEM_FRAME);
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> DAMAGE_VILLAGERS = Abilities.entityDamageBuilder.build("damage_villagers",EntityType.VILLAGER);
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> DAMAGE_ANIMALS = new Ability<>(
            Abilities.BREAK_ARMOR_STANDS,
            e -> e.getEntity() instanceof Animals
    );
    @AbilityRegistration
    public final static Ability<EntityDamageByEntityEvent> DAMAGE_MONSTERS = new Ability<>(
            Abilities.BREAK_ARMOR_STANDS,
            e -> e.getEntity() instanceof Monster
    );

    //PlayerInteractEntityEvent
    @AbilityRegistration
    public final static Ability<PlayerInteractEntityEvent> INTERACT_WITH_ITEM_FRAMES = new Ability<>(
            PlayerInteractEntityEvent.class,
            e -> e.getRightClicked().getType().equals(EntityType.ITEM_FRAME),
            PlayerEvent::getPlayer,
            e -> e.getRightClicked().getLocation()
    );
    @AbilityRegistration
    public final static Ability<PlayerInteractEntityEvent> INTERACT_WITH_GLOW_ITEM_FRAMES = new Ability<>(
            Abilities.INTERACT_WITH_ITEM_FRAMES,
            e -> e.getRightClicked().getType().equals(EntityType.GLOW_ITEM_FRAME)
    );
    @AbilityRegistration
    public final static Ability<PlayerInteractEntityEvent> PUT_INTO_ITEM_FRAMES = new Ability<>(
            Abilities.INTERACT_WITH_ITEM_FRAMES,
            e -> ((ItemFrame) e.getRightClicked()).getItem().getType().equals(Material.AIR)
    ).extend(Abilities.INTERACT_WITH_ITEM_FRAMES);
    @AbilityRegistration
    public final static Ability<PlayerInteractEntityEvent> PUT_INTO_GLOW_ITEM_FRAMES = new Ability<>(
            Abilities.PUT_INTO_ITEM_FRAMES,
            Abilities.PUT_INTO_ITEM_FRAMES.validator
    ).extend(Abilities.INTERACT_WITH_GLOW_ITEM_FRAMES);

    //PlayerInteractAtEntityEvent
    @AbilityRegistration
    public final static Ability<PlayerInteractAtEntityEvent> INTERACT_WITH_ARMOR_STANDS = new Ability<>(
            PlayerInteractAtEntityEvent.class,
            e -> e.getRightClicked().getType().equals(EntityType.ARMOR_STAND),
            e -> e.getRightClicked().getLocation()
    );

    //PlayerTeleportEvent
    private final static PlayerTeleportEvent.TeleportCause[] validTeleportCauses = {
            PlayerTeleportEvent.TeleportCause.COMMAND, PlayerTeleportEvent.TeleportCause.END_PORTAL, PlayerTeleportEvent.TeleportCause.END_GATEWAY,
            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, PlayerTeleportEvent.TeleportCause.PLUGIN
    };
    @AbilityRegistration
    public final static Ability<PlayerTeleportEvent> TELEPORT_IN = new Ability<>(
            PlayerTeleportEvent.class,
            e -> {
                for (PlayerTeleportEvent.TeleportCause cause : validTeleportCauses) {
                    if (cause.equals(e.getCause())) { return false; }
                }
                return true;
            },
            PlayerTeleportEvent::getPlayer,
            PlayerTeleportEvent::getTo
    );
    @AbilityRegistration
    public final static Ability<PlayerTeleportEvent> TELEPORT_OUT = new Ability<>(
            PlayerTeleportEvent.class,
            Abilities.TELEPORT_IN::isAllowed,
            PlayerTeleportEvent::getPlayer,
            PlayerTeleportEvent::getFrom
    ).extend(Abilities.TELEPORT_IN);
}