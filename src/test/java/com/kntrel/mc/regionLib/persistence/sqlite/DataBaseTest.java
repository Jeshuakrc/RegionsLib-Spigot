package com.kntrel.mc.regionLib.persistence.sqlite;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DataBaseTest {

    @DTO.Table("test_table")
    record TestDTO(@DTO.Column("id") int id, @DTO.Column("name") String name) {}

    private Connection conn_;
    private DataBase dataBase_;

    public DataBaseTest() {
        try {
            this.conn_ = DriverManager.getConnection("jdbc:sqlite::memory:");
            DataBaseInitializer.runScript(this.conn_, getClass().getClassLoader().getResource("schema/v1.sql"));
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

        this.dataBase_ = new DataBase(this.conn_);
    }

    @Test
    void testConnection() {
        assertDoesNotThrow(() -> {
            try (PreparedStatement stmt = conn_.prepareStatement("SELECT 1")) {
                var rs = stmt.executeQuery();
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        });
    }

    @Test
    void testQuery() {
        assertDoesNotThrow(() -> {
            String sql = "CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT);";
            try (PreparedStatement stmt = conn_.prepareStatement(sql)) {
                stmt.execute();
            }

            sql = "INSERT INTO test_table (name) VALUES ('Test Name');";
            try (PreparedStatement stmt = conn_.prepareStatement(sql)) {
                stmt.executeUpdate();
            }

            sql = "SELECT id, name FROM test_table;";
            var results = dataBase_.query(sql, TestDTO.class);
            assertEquals(1, results.size());
            assertEquals("Test Name", results.getFirst().name);
        });
    }

    @Test
    void testInserts() {
        List<RegionSnapshot> snapshots = snapshots(20);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(RegionSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.perms()))
                    .toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.rules()))
                    .toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.data()))
                    .toList());
        });
        assertDoesNotThrow(() -> {
            String sql = "SELECT COUNT(*) FROM region;";
            try (PreparedStatement stmt = conn_.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(20, rs.getInt(1));
            }
            sql = "SELECT COUNT(*) FROM regionPermission;";
            int count = (int) snapshots.stream().flatMap(s -> Arrays.stream(s.perms())).count();
            try (PreparedStatement stmt = conn_.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(count, rs.getInt(1));
            }
            sql = "SELECT COUNT(*) FROM regionRule;";
            count = (int) snapshots.stream().flatMap(s -> Arrays.stream(s.rules())).count();
            try (PreparedStatement stmt = conn_.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(count, rs.getInt(1));
            }
            sql = "SELECT COUNT(*) FROM regionData;";
            count = (int) snapshots.stream().flatMap(s -> Arrays.stream(s.data())).count();
            try (PreparedStatement stmt = conn_.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(count, rs.getInt(1));
            }
        });
    }


    // HELPERS
    private static List<RegionSnapshot> snapshots(int count) {
        List<RegionSnapshot> regionSnapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {

            double x1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                   y1 = ThreadLocalRandom.current().nextDouble(-64, 320),
                   z1 = ThreadLocalRandom.current().nextDouble(-400, 400),
                   x2 = ThreadLocalRandom.current().nextDouble(-400, 400),
                   y2 = ThreadLocalRandom.current().nextDouble(-64, 320),
                   z2 =  ThreadLocalRandom.current().nextDouble(-400, 400);

            DTO.Region region = new DTO.Region(
                    i,
                    "Region" + i,
                    "world",
                    true,
                    0,
                    Math.min(x1, x2),
                    Math.min(y1, y2),
                    Math.min(z1, z2),
                    Math.max(x1, x2),
                    Math.max(y1, y2),
                    Math.max(z1, z2),
                    false
            );
            List<DTO.Permission> permissions = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                permissions.add(new DTO.Permission(
                        i,
                        UUID.randomUUID().toString(),
                        ThreadLocalRandom.current().nextInt(0, 10)
                ));
            }
            List<DTO.Rule> rules = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                rules.add(new DTO.Rule(
                        i,
                        "rule_key_" + j,
                        "rule_value_" + j
                ));
            }
            List<DTO.Data> data = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                data.add(new DTO.Data(
                        i,
                        "data_key_" + j,
                        "data_value_" + j
                ));
            }
            regionSnapshots.add(new RegionSnapshot(region, permissions, rules, data));
        }
        return regionSnapshots;
    }

}
