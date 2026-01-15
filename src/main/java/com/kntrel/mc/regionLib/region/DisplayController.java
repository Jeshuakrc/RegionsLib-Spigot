package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.display.AreaDisplayer;
import com.kntrel.mc.regionLib.region.display.DisplayToken;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;

class DisplayController {

    //FIELDS
    private final AreaDisplayer displayer_;
    private final RegionContext ctx_;
    private final Map<Region, DisplayContext> taskMap_;
    private final int displayDuration_;


    //CONSTRUCTORS
    DisplayController(RegionContext regionContext, AreaDisplayer displayer, int displayDurationSeconds) {
        this.displayer_ = displayer;
        this.ctx_ = regionContext;
        this.taskMap_ = new HashMap<>();
        this.displayDuration_ = displayDurationSeconds;
    }


    void display(Region region, AreaDisplayer displayer, long seconds, Player player) {
        if (this.taskMap_.containsKey(region)) {
            this.stopDisplay(region);
        }

        DisplayToken token = displayer.display(Area.ofRegion(region), player);
        BukkitTask task = this.ctx_.getServer().getScheduler().runTaskLater(
                this.ctx_.getPlugin(),
                () -> this.stopDisplay(region),
                seconds * 20L
        );

        this.taskMap_.put(region, new DisplayContext(displayer, task, token));
    }
    void display(Region region, long second, Player player) {
        this.display(region, this.displayer_, second, player);
    }
    void display(Region region, AreaDisplayer displayer, Player player) {
        this.display(region, displayer, this.displayDuration_, player);
    }
    void display(Region region, Player player) {
        this.display(region, this.displayer_ , this.displayDuration_, player);
    }




    void stopDisplay(Region region) {
        DisplayContext ctx = this.taskMap_.get(region);
        if (ctx == null) { return; }

        ctx.task().cancel();
        ctx.displayer().stop(ctx.token());
        this.taskMap_.remove(region);
    }


    private record DisplayContext(AreaDisplayer displayer, BukkitTask task, DisplayToken token) {}

}
