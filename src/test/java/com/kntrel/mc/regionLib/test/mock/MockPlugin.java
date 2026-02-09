package com.kntrel.mc.regionLib.test.mock;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

public class MockPlugin implements Plugin {

    private final String name_;
    private final Server server_;
    private final Logger logger_;


    public MockPlugin() {
        this.name_ = "MockPlugin";
        this.server_ = MockServer.mockServer();
        this.logger_ = Logger.getGlobal();
    }


    @NotNull @Override public File getDataFolder() {
        unimplemented();
        return null;
    }
    @NotNull @Override public PluginDescriptionFile getDescription() {
        unimplemented();
        return null;
    }

    @NotNull @Override public FileConfiguration getConfig() {
        unimplemented();
        return null;
    }

    @Nullable @Override public InputStream getResource(@NotNull String s) {
        unimplemented();
        return null;
    }

    @Override public void saveConfig() {}

    @Override public void saveDefaultConfig() {}

    @Override public void saveResource(@NotNull String s, boolean b) {}

    @Override public void reloadConfig() {}

    @NotNull @Override public PluginLoader getPluginLoader() {
        unimplemented();
        return null;
    }

    @NotNull @Override public Server getServer() { return this.server_; }

    @Override public boolean isEnabled() { return true; }

    @Override
    public void onDisable() {}

    @Override
    public void onLoad() {}

    @Override
    public void onEnable() {}

    @Override
    public boolean isNaggable() { return false; }

    @Override
    public void setNaggable(boolean b) {}

    @Nullable @Override public ChunkGenerator getDefaultWorldGenerator(@NotNull String s, @Nullable String s1) {
        unimplemented();
        return null;
    }

    @Nullable @Override public BiomeProvider getDefaultBiomeProvider(@NotNull String s, @Nullable String s1) {
        unimplemented();
        return null;
    }

    @NotNull @Override public Logger getLogger() { return this.logger_; }

    @NotNull @Override public String getName() { return this.name_; }

    @Override public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return false;
    }

    @Nullable @Override public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }


    //HELPERS
    private static void unimplemented() {
        throw new UnsupportedOperationException("This method is not implemented in MockPlugin");
    }
}
