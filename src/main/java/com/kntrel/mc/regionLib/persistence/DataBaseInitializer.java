package com.kntrel.mc.regionLib.persistence;

import org.bukkit.plugin.Plugin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class DataBaseInitializer {

    private static final String RESOURCE_PREFIX = "schema/";
    private static final Pattern FILE_PATTERN = Pattern.compile("v(\\d+)\\.sql", Pattern.CASE_INSENSITIVE);


    private static SortedMap<Integer, String> loadMigrations() throws IOException {

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

    private static void migrate(Plugin plugin, Connection connection) {
        int curr = getUserVersion(connection);

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

        boolean oldAutoCommit;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        int latest = curr;
        try {
            for (Map.Entry<Integer, String> entry : migrations.entrySet()) {
                latest = entry.getKey();
                plugin.getServer().getLogger().log(Level.INFO, "Migrating RegionLib database to v" + latest);
                URL script = cl.getResource(entry.getValue());
                DataBaseInitializer.runScript(connection, script);
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA user_version = " + latest);
            }
            connection.commit();
        } catch (SQLException | IOException e) {
            try { connection.rollback(); } catch (SQLException ignored) { }
            throw new RuntimeException(e);
        } finally {
            try { connection.setAutoCommit(oldAutoCommit); } catch (SQLException ignored) { }
        }
    }

    private static void runScript(Connection connection, URL script) throws IOException, SQLException {
        if (script == null) { return; }
        try (InputStream stream = script.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            LinkedList<String> statements = new LinkedList<>();
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--")) { continue; }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(current.toString());
                    current.setLength(0);
                }
            }

            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    stmt.execute(sql);
                }
            }
        }
    }

    private static int getUserVersion(Connection connection) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection(Plugin plugin, URI location) {
        Path dbPath = Paths.get(location);
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            DataBaseInitializer.migrate(plugin, connection);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
