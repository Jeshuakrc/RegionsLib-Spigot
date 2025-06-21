package com.kntrel.mc.regionLib.persistence;

import com.kntrel.mc.regionLib.persistence.jpa.dto.PermissionDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RegionDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RegionDataDTO;
import com.kntrel.mc.regionLib.persistence.jpa.dto.RuleValueDTO;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.platform.sqlite.SQLitePlatform;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DataBaseInitializer {

    private static final String RESOURCE_PREFIX = "schema/";
    private static final Pattern FILE_PATTERN = Pattern.compile("v(\\d+)\\.sql", Pattern.CASE_INSENSITIVE);


    private static SortedMap<Integer, String> loadMigrations() throws IOException, URISyntaxException {

        SortedMap<Integer, String> out = new TreeMap<>();

        try (JarFile jar = new JarFile(
            new java.io.File(DataBaseInitializer.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
        ))) {

            Enumeration<JarEntry> it = jar.entries();
            while (it.hasMoreElements()) {
                JarEntry je = it.nextElement();
                String name = je.getName();
                if (!name.startsWith(RESOURCE_PREFIX)) { continue; }

                String simple = name.substring(RESOURCE_PREFIX.length());
                Matcher m = FILE_PATTERN.matcher(simple);
                if (!m.matches()) { continue; }
                int ver = Integer.parseInt(m.group(1));

                out.put(ver, name);
            }
        }
        return out;
    }

    private static void migrate(Plugin plugin, Database db) {

        int curr = 0;
        SqlRow row = db.sqlQuery("PRAGMA user_version").findOne();
        if (row != null) {
            curr = row.getInteger("user_version");
        }

        SortedMap<Integer, String> migrations;
        try {
            migrations = DataBaseInitializer.loadMigrations();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        migrations = migrations.tailMap(curr + 1);
        if (migrations.isEmpty()) { return; }

        ClassLoader cl = plugin.getClass().getClassLoader();
        plugin.getServer().getLogger().log(Level.INFO, "Database schema is behind. Migrating.");
        try (Transaction txn = db.beginTransaction()) {
            int latest = curr;
            for (Map.Entry<Integer, String> entry : migrations.entrySet()) {
                latest = entry.getKey();
                plugin.getServer().getLogger().log(Level.INFO, "Migrating RegionLib database to v" + latest);
                URL script = cl.getResource(entry.getValue());
                db.script().run(script);
            }
            db.execute(db.sqlUpdate("PRAGMA user_version = " + latest));
            txn.commit();
        }
    }

    public static Database getDatabase(Plugin plugin, URI location) {
        DataSourceConfig dsConfig = new DataSourceConfig();
        dsConfig.setDriver("org.sqlite.JDBC");
        dsConfig.setUrl("jdbc:sqlite:" + location.getPath());
        dsConfig.setUsername("");
        dsConfig.setPassword("");

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setName("SQLite");
        dbConfig.setDatabasePlatform(new SQLitePlatform());
        dbConfig.setDataSourceConfig(dsConfig);
        dbConfig.setDdlGenerate(false);
        dbConfig.setDdlRun(false);

        // Register your @Entity classes
        dbConfig.addClass(RegionDTO.class);
        dbConfig.addClass(RegionDataDTO.class);
        dbConfig.addClass(PermissionDTO.class);
        dbConfig.addClass(RuleValueDTO.class);

        ClassLoader pluginLoader = plugin.getClass().getClassLoader();
        Database db = DatabaseFactory.createWithContextClassLoader(dbConfig, pluginLoader);

        DataBaseInitializer.migrate(plugin, db);

        return db;
    }
}
