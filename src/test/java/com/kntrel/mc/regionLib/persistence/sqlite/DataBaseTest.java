package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.cache.RegionSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static com.kntrel.mc.regionLib.cache.RegionSnapshotTest.snapshots;
import static com.kntrel.mc.regionLib.persistence.sqlite.SQLiteRegionRepository.*;

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

    @BeforeEach
    void setUp() throws SQLException {
        try (
            PreparedStatement dropPermissions = this.conn_.prepareStatement("DELETE FROM regionPermission");
            PreparedStatement dropData = this.conn_.prepareStatement("DELETE FROM regionData");
            PreparedStatement dropRules = this.conn_.prepareStatement("DELETE FROM regionRule");
        ) {
            this.conn_.setAutoCommit(false);
            dropPermissions.execute();
            dropData.execute();
            dropRules.execute();

            this.conn_.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            this.conn_.setAutoCommit(true);
        }
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
        List<DTOSnapshot> snapshots = DTOSnapshots(20);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.permissions()))
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
            int count = (int) snapshots.stream().flatMap(s -> Arrays.stream(s.permissions())).count();
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

    @Test
    void testQueryDTOs() {
        assertDoesNotThrow(() -> {
            try (PreparedStatement stmt = this.conn_.prepareStatement("SELECT * FROM region;")) {
                ResultSet rs = stmt.executeQuery();
                assertFalse(rs.next());
            }
        });
        int len = 30;
        List<DTOSnapshot> snapshots = DTOSnapshots(len);

        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
        });

        List<DTO.Region> result = assertDoesNotThrow(() -> this.dataBase_.query("SELECT * FROM region ORDER BY id ASC;", DTO.Region.class));
        assertEquals(snapshots.size(), result.size());

        DTO.Region prevReg = null;
        for (int i = 0; i < len; i++) {
            DTO.Region reg = result.get(i);
            assertEquals(snapshots.get(i).region(), reg);
            if (prevReg != null) {
                assertTrue(prevReg.id() < reg.id());
            }
            prevReg = reg;
        }
    }

    @Test
    void testUpdateRegions() {
        List<DTOSnapshot> snapshots = DTOSnapshots(5);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
        });

        // Verify initial insert
        List<DTO.Region> initial = assertDoesNotThrow(() -> 
            this.dataBase_.query("SELECT * FROM region WHERE id = 0;", DTO.Region.class));
        assertEquals(1, initial.size());
        assertEquals("Region0", initial.getFirst().name());
        assertTrue(initial.getFirst().enabled());

        // Update the region
        DTO.Region updatedRegion = new DTO.Region(
                0,
                "UpdatedRegion0",
                "world",
                false,
                0,
                -100.0,
                0.0,
                -100.0,
                100.0,
                100.0,
                100.0,
                false
        );

        assertDoesNotThrow(() -> {
            this.dataBase_.update(List.of(updatedRegion));
        });

        // Verify update
        List<DTO.Region> updated = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM region WHERE id = 0;", DTO.Region.class));
        assertEquals(1, updated.size());
        assertEquals("UpdatedRegion0", updated.getFirst().name());
        assertFalse(updated.getFirst().enabled());
        assertEquals(-100.0, updated.getFirst().minX());
    }

    @Test
    void testUpdatePermissions() {
        List<DTOSnapshot> snapshots = DTOSnapshots(3);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.permissions()))
                    .toList());
        });

        // Query initial permissions
        List<DTO.Permission> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission WHERE region_id = 0;", DTO.Permission.class));
        assertEquals(3, initial.size());
        
        // Update all permissions for region 0 with new level
        List<DTO.Permission> updatedPerms = initial.stream()
                .map(p -> new DTO.Permission(p.regionId(), p.playerUUID(), 9))
                .toList();

        assertDoesNotThrow(() -> {
            this.dataBase_.update(updatedPerms);
        });

        // Verify all permissions were updated
        List<DTO.Permission> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission WHERE region_id = 0;", DTO.Permission.class));
        assertEquals(3, result.size());
        for (DTO.Permission perm : result) {
            assertEquals(9, perm.level());
        }
    }

    @Test
    void testUpdateRules() {
        List<DTOSnapshot> snapshots = DTOSnapshots(2);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.rules()))
                    .toList());
        });

        // Query initial rules
        List<DTO.Rule> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionRule WHERE region_id = 0;", DTO.Rule.class));
        assertEquals(3, initial.size());

        // Update specific rule
        DTO.Rule updatedRule = new DTO.Rule(0, "rule_key_0", "updated_value_0");
        assertDoesNotThrow(() -> {
            this.dataBase_.update(List.of(updatedRule));
        });

        // Verify update
        List<DTO.Rule> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionRule WHERE region_id = 0 AND key = 'rule_key_0';", DTO.Rule.class));
        assertEquals(1, result.size());
        assertEquals("updated_value_0", result.getFirst().value());
    }

    @Test
    void testUpdateData() {
        List<DTOSnapshot> snapshots = DTOSnapshots(2);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.data()))
                    .toList());
        });

        // Query initial data
        List<DTO.Data> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionData WHERE region_id = 1;", DTO.Data.class));
        assertEquals(3, initial.size());

        // Update all data
        List<DTO.Data> updatedData = initial.stream()
                .map(d -> new DTO.Data(d.regionId(), d.key(), "updated_" + d.value()))
                .toList();

        assertDoesNotThrow(() -> {
            this.dataBase_.update(updatedData);
        });

        // Verify update
        List<DTO.Data> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionData WHERE region_id = 1;", DTO.Data.class));
        assertEquals(3, result.size());
        for (DTO.Data data : result) {
            assertTrue(data.value().startsWith("updated_"));
        }
    }

    @Test
    void testDeleteRegions() {
        List<DTOSnapshot> snapshots = DTOSnapshots(10);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
        });

        // Verify initial insert
        record Count(int count) {}
        Optional<Count> count = assertDoesNotThrow(() -> this.dataBase_.queryOne("SELECT COUNT(*) as count FROM region;", Count.class));
        assertTrue(count.isPresent());
        assertEquals(10, count.get().count());

        // Delete specific regions
        List<DTO.Region> toDelete = snapshots.stream()
                .limit(3)
                .map(DTOSnapshot::region)
                .toList();

        assertDoesNotThrow(() -> {
            this.dataBase_.delete(toDelete);
        });

        // Verify deletion
        List<DTO.Region> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM region;", DTO.Region.class));
        assertEquals(7, result.size());
        
        // Ensure deleted IDs are not present
        Set<Long> remainingIds = result.stream().map(DTO.Region::id).collect(Collectors.toSet());
        assertTrue(remainingIds.contains(3L));
        assertTrue(remainingIds.contains(9L));
        assertFalse(remainingIds.contains(0L));
        assertFalse(remainingIds.contains(1L));
        assertFalse(remainingIds.contains(2L));
    }

    @Test
    void testDeletePermissions() {
        List<DTOSnapshot> snapshots = DTOSnapshots(4);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.permissions()))
                    .toList());
        });

        // Query permissions for region 1
        List<DTO.Permission> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission WHERE region_id = 1;", DTO.Permission.class));
        assertEquals(3, initial.size());

        // Delete first permission
        assertDoesNotThrow(() -> {
            this.dataBase_.delete(List.of(initial.get(0)));
        });

        // Verify deletion
        List<DTO.Permission> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission WHERE region_id = 1;", DTO.Permission.class));
        assertEquals(2, result.size());
        
        // Verify the deleted permission UUID is not present
        Set<String> remainingUUIDs = result.stream().map(DTO.Permission::playerUUID).collect(Collectors.toSet());
        assertFalse(remainingUUIDs.contains(initial.getFirst().playerUUID()));
    }

    @Test
    void testDeleteRules() {
        List<DTOSnapshot> snapshots = DTOSnapshots(3);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.rules()))
                    .toList());
        });

        // Query initial rules for region 2
        List<DTO.Rule> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionRule WHERE region_id = 2;", DTO.Rule.class));
        assertEquals(3, initial.size());

        // Delete rules with specific key
        List<DTO.Rule> toDelete = initial.stream()
                .filter(r -> r.key().equals("rule_key_0") || r.key().equals("rule_key_1"))
                .toList();

        assertDoesNotThrow(() -> {
            this.dataBase_.delete(toDelete);
        });

        // Verify deletion
        List<DTO.Rule> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionRule WHERE region_id = 2;", DTO.Rule.class));
        assertEquals(1, result.size());
        assertEquals("rule_key_2", result.getFirst().key());
    }

    @Test
    void testDeleteData() {
        List<DTOSnapshot> snapshots = DTOSnapshots(2);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.data()))
                    .toList());
        });

        // Query initial data for region 0
        List<DTO.Data> initial = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionData WHERE region_id = 0;", DTO.Data.class));
        assertEquals(3, initial.size());

        // Delete all data for region 0
        assertDoesNotThrow(() -> {
            this.dataBase_.delete(initial);
        });

        // Verify deletion
        List<DTO.Data> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionData WHERE region_id = 0;", DTO.Data.class));
        assertEquals(0, result.size());

        // Verify data in other regions still exists
        List<DTO.Data> otherRegionData = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionData WHERE region_id = 1;", DTO.Data.class));
        assertEquals(3, otherRegionData.size());
    }

    @Test
    void testUpdateAndDeleteCombined() {
        List<DTOSnapshot> snapshots = DTOSnapshots(6);
        assertDoesNotThrow(() -> {
            this.dataBase_.insert(snapshots.stream().map(DTOSnapshot::region).toList());
            this.dataBase_.insert(snapshots.stream()
                    .flatMap(s -> Arrays.stream(s.permissions()))
                    .toList());
        });

        // Get initial state
        List<DTO.Permission> allPerms = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission;", DTO.Permission.class));
        int initialCount = allPerms.size();

        // Update some permissions and delete others
        List<DTO.Permission> toUpdate = new ArrayList<>();
        List<DTO.Permission> toDelete = new ArrayList<>();

        for (int i = 0; i < allPerms.size(); i++) {
            if (i % 2 == 0) {
                toUpdate.add(new DTO.Permission(allPerms.get(i).regionId(), allPerms.get(i).playerUUID(), 7));
            } else {
                toDelete.add(allPerms.get(i));
            }
        }

        assertDoesNotThrow(() -> {
            this.dataBase_.write(null, toUpdate, toDelete);
        });

        // Verify combined operations
        List<DTO.Permission> result = assertDoesNotThrow(() ->
            this.dataBase_.query("SELECT * FROM regionPermission;", DTO.Permission.class));
        assertEquals(initialCount / 2, result.size());
        
        // Verify all remaining have level 7 (updated ones)
        for (DTO.Permission perm : result) {
            assertEquals(7, perm.level());
        }
    }


    //HELPERS
    static DataBase memoryDatabase() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
            DataBaseInitializer.runScript(conn, DataBaseTest.class.getClassLoader().getResource("schema/v1.sql"));
            return new DataBase(conn);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Object memoryDatabaseObject() {
        return memoryDatabase();
    }
    private static List<DTOSnapshot> DTOSnapshots(int count) {
        return snapshots(count).stream()
                .map(s -> new DTOSnapshot(
                        toRegionDTO(s),
                        toPermDTOs(s.permissions(), s.id()).toArray(new DTO.Permission[0]),
                        toRuleDTOs(s.rules(), s.id()).toArray(new DTO.Rule[0]),
                        toDataDTOs(s.data(), s.id()).toArray(new DTO.Data[0])
                ))
                .toList();
    }

    private record DTOSnapshot(DTO.Region region, DTO.Permission[] permissions, DTO.Rule[] rules, DTO.Data[] data) {}
}
