package com.kntrel.mc.regionLib.region.context;

import com.kntrel.mc.regionLib.event.PlayerEnterRegionEvent;
import com.kntrel.mc.regionLib.event.PlayerLeaveRegionEvent;
import com.kntrel.mc.regionLib.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class PlayerSampler implements Listener {

    //FIELDS
    private final RegionContext context_;
    private final long samplingPeriodTicks_;
    private final double movementTolerance_;
    private final double movementToleranceSquared_;
    private final BukkitTask task_;
    private final Map<UUID, Map<Long, Region>> playerRegions_;
    private final Map<UUID, Location> sampledLocations_;
    private final Map<UUID, Player> dirtyPlayers_;
    private final Map<Long, Set<UUID>> regionPlayers_;


    //CONSTRUCTORS
    PlayerSampler(RegionContext context, long samplingPeriodTicks, double movementTolerance) {
        if (samplingPeriodTicks < 1L) {
            throw new IllegalArgumentException("Player sampling period must be at least 1 tick.");
        }
        if (movementTolerance < 0D) {
            throw new IllegalArgumentException("Player movement tolerance cannot be negative.");
        }

        this.context_ = context;
        this.samplingPeriodTicks_ = samplingPeriodTicks;
        this.movementTolerance_ = movementTolerance;
        this.movementToleranceSquared_ = movementTolerance * movementTolerance;
        this.playerRegions_ = new HashMap<>();
        this.sampledLocations_ = new HashMap<>();
        this.dirtyPlayers_ = new HashMap<>();
        this.regionPlayers_ = new HashMap<>();
        this.task_ = this.context_.getServer().getScheduler().runTaskTimer(
                this.context_.getPlugin(),
                (Runnable) this::sample,
                this.samplingPeriodTicks_,
                this.samplingPeriodTicks_
        );

        this.context_.getServer().getPluginManager().registerEvents(this, this.context_.getPlugin());
        this.context_.getServer().getOnlinePlayers().forEach(player -> this.markDirty(player));
    }


    //GETTERS
    long getSamplingPeriodTicks() {
        return this.samplingPeriodTicks_;
    }
    double getMovementTolerance() {
        return this.movementTolerance_;
    }


    //API
    List<Player> getPlayersWithin(long regionId) {
        Set<UUID> playerIds = this.regionPlayers_.get(regionId);
        if (playerIds == null || playerIds.isEmpty()) {
            return List.of();
        }

        return this.context_.getServer().getOnlinePlayers().stream()
                .<Player>map(player -> player)
                .filter(player -> playerIds.contains(player.getUniqueId()))
                .toList();
    }
    void sample() {
        List<Player> queued = new ArrayList<>(this.dirtyPlayers_.values());
        this.dirtyPlayers_.clear();
        for (Player player : queued) {
            this.sample(player);
        }
    }
    void sample(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Long, Region> previous = this.playerRegions_.getOrDefault(playerId, Map.of());
        Map<Long, Region> current = this.regionsAt(player);

        for (Region region : previous.values()) {
            long regionId = region.getId();
            if (current.containsKey(regionId)) {
                continue;
            }

            this.unlink(playerId, regionId);
            this.context_.callEvent(new PlayerLeaveRegionEvent(player, region));
        }

        for (Region region : current.values()) {
            long regionId = region.getId();
            if (previous.containsKey(regionId)) {
                continue;
            }

            this.link(playerId, regionId);
            this.context_.callEvent(new PlayerEnterRegionEvent(player, region));
        }

        if (current.isEmpty()) {
            this.playerRegions_.remove(playerId);
        } else {
            this.playerRegions_.put(playerId, current);
        }
        this.sampledLocations_.put(playerId, player.getLocation().clone());
    }
    void markDirty(Player player) {
        this.dirtyPlayers_.put(player.getUniqueId(), player);
    }


    //LISTENERS
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        this.markDirty(event.getPlayer());
    }
    @EventHandler
    void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (!this.shouldQueue(player.getUniqueId(), to)) {
            return;
        }
        this.markDirty(player);
    }
    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        this.clearPlayer(event.getPlayer());
    }
    @EventHandler
    void onPlayerRespawn(PlayerRespawnEvent event) {
        this.markDirty(event.getPlayer());
    }
    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != this.context_.getPlugin()) {
            return;
        }

        this.task_.cancel();
        this.playerRegions_.clear();
        this.sampledLocations_.clear();
        this.dirtyPlayers_.clear();
        this.regionPlayers_.clear();
    }


    //PRIVATE
    private void clearPlayer(UUID playerId) {
        Map<Long, Region> previous = this.playerRegions_.remove(playerId);
        this.sampledLocations_.remove(playerId);
        this.dirtyPlayers_.remove(playerId);
        if (previous == null || previous.isEmpty()) {
            return;
        }

        for (long regionId : previous.keySet()) {
            this.unlink(playerId, regionId);
        }
    }
    private void clearPlayer(Player player) {
        Map<Long, Region> previous = this.playerRegions_.remove(player.getUniqueId());
        this.sampledLocations_.remove(player.getUniqueId());
        this.dirtyPlayers_.remove(player.getUniqueId());
        if (previous == null || previous.isEmpty()) {
            return;
        }

        for (Region region : previous.values()) {
            this.unlink(player.getUniqueId(), region.getId());
            this.context_.callEvent(new PlayerLeaveRegionEvent(player, region));
        }
    }
    private boolean shouldQueue(UUID playerId, Location currentLocation) {
        Location sampled = this.sampledLocations_.get(playerId);
        if (sampled == null) {
            return true;
        }
        if (!Objects.equals(sampled.getWorld(), currentLocation.getWorld())) {
            return true;
        }
        return sampled.distanceSquared(currentLocation) >= this.movementToleranceSquared_;
    }
    private void link(UUID playerId, long regionId) {
        this.regionPlayers_.computeIfAbsent(regionId, ignored -> new HashSet<>()).add(playerId);
    }
    private void unlink(UUID playerId, long regionId) {
        Set<UUID> playerIds = this.regionPlayers_.get(regionId);
        if (playerIds == null) {
            return;
        }

        playerIds.remove(playerId);
        if (playerIds.isEmpty()) {
            this.regionPlayers_.remove(regionId);
        }
    }
    private Map<Long, Region> regionsAt(Player player) {
        return this.context_.getHotRegionRepository().getAt(player.getLocation()).stream()
                .filter(region -> region.getId() != null)
                .collect(Collectors.toMap(Region::getId, Function.identity(), (left, right) -> left, HashMap::new));
    }
}
