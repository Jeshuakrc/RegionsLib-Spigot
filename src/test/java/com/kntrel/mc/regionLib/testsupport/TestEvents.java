package com.kntrel.mc.regionLib.testsupport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class TestEvents {

    private TestEvents() {}

    public static class LocationEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Location location;

        public LocationEvent(Location location) {
            super(false);
            this.location = location;
        }

        public Location getLocation() {
            return this.location;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class SecondaryLocationEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Location location;

        public SecondaryLocationEvent(Location location) {
            super(false);
            this.location = location;
        }

        public Location getLocation() {
            return this.location;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class CancellableEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private boolean cancelled;

        public CancellableEvent() {
            super(false);
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class PlayerCarrierEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;

        public PlayerCarrierEvent(Player player) {
            super(false);
            this.player = player;
        }

        public Player getPlayer() {
            return this.player;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class SimplePlayerEvent extends PlayerEvent {
        private static final HandlerList HANDLERS = new HandlerList();

        public SimplePlayerEvent(Player who) {
            super(who);
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }

    public static class NoPlayerEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();

        public NoPlayerEvent() {
            super(false);
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
