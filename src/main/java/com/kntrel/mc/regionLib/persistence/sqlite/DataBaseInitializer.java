package com.kntrel.mc.regionLib.persistence.sqlite;

import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    static void runScript(Connection conn, URL script) throws IOException, SQLException {
        StringBuilder cleaned = new StringBuilder();
        for (String line : new String(script.openStream().readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) { continue; }
            cleaned.append(line).append('\n');
        }

        for (String statement : cleaned.toString().split(";")) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) { continue; }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(trimmed);
            }
        }
    }

    private static int getUserVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            if (rs.next()) { return rs.getInt(1); }
        }
        return 0;
    }

    private static void setUserVersion(Connection conn, int version) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA user_version = " + version);
        }
    }

    private static void migrate(Plugin plugin, Connection conn) {

        int curr;
        try {
            curr = DataBaseInitializer.getUserVersion(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed reading database version", e);
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
        int latest = curr;
        try {
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, String> entry : migrations.entrySet()) {
                latest = entry.getKey();
                plugin.getServer().getLogger().log(Level.INFO, "Migrating RegionLib database to v" + latest);
                URL script = cl.getResource(entry.getValue());
                if (script == null) { throw new IOException("Missing migration script " + entry.getValue()); }
                DataBaseInitializer.runScript(conn, script);
            }
            DataBaseInitializer.setUserVersion(conn, latest);
            conn.commit();
        } catch (SQLException | IOException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Failed migrating database", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static Connection getConnection(Plugin plugin, URI location) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }

        Path dbPath = Paths.get(location);

        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            DataBaseInitializer.migrate(plugin, conn);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite connection", e);
        }
    }
}
