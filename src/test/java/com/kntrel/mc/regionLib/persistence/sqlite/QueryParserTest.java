package com.kntrel.mc.regionLib.persistence.sqlite;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.ability.Ability;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.hierarchy.HierarchyRepository;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.test.mock.MockWorld;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("QueryParser Tests")
class QueryParserTest {

    private QueryParser parser;
    private RegionContext mockContext;
    private World mockWorld;
    private HierarchyRepository mockHierarchyRepository;

    @BeforeEach
    void setUp() {
        this.mockContext = mock(RegionContext.class);
        this.mockWorld = MockWorld.mockWorld();
        this.mockHierarchyRepository = mock(HierarchyRepository.class);

        when(mockContext.getHierarchyRepository()).thenReturn(mockHierarchyRepository);
        when(mockHierarchyRepository.getAll()).thenReturn(List.of());
        when(mockWorld.getName()).thenReturn("world");

        this.parser = new QueryParser(this.mockContext);
    }

    private static String regionSelect(String rest) {
        String base = "SELECT DISTINCT region.* FROM region";
        if (rest.isEmpty()) { return base + ";"; }
        return base + " " + rest + ";";
    }
    private static String projectedSelect(String columns, String rest) {
        String base = "SELECT DISTINCT " + columns + " FROM region";
        if (rest.isEmpty()) { return base + ";"; }
        return base + " " + rest + ";";
    }

    @Nested
    @DisplayName("Basic Query Tests")
    class BasicQueryTests {

        @Test
        @DisplayName("Parse simple query without condition")
        void testSimpleQueryWithoutCondition() {
            Query query = Query.builder(null).asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse query with destroyed regions included")
        void testQueryIncludingDestroyed() {
            Query query = Query.builder(null).includeDestroyed().asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect(""),
                    sql
            );
        }

        @Test
        @DisplayName("Parse query with LIMIT clause")
        void testQueryWithLimit() {
            Query query = Query.builder(null).limit(10).asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed = 0 LIMIT 10"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse query with no limit")
        void testQueryWithoutLimit() {
            Query query = Query.builder(null).limit(0).asQuery();
            String sql = parser.parse(query);

            assertFalse(sql.contains("LIMIT"));
        }

        @Test
        @DisplayName("Parse query with ordering ascending")
        void testQueryWithOrderingAscending() {
            Query query = Query.builder(null)
                    .orderBy(RegionField.NAME)
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed = 0 ORDER BY region.name ASC"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse query with ordering descending")
        void testQueryWithOrderingDescending() {
            Query query = Query.builder(null)
                    .orderByDesc(RegionField.NAME)
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed = 0 ORDER BY region.name DESC"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse projected query with single field")
        void testProjectedQuerySingleField() {
            Query query = Query.builder(null).asQuery();
            String sql = parser.parse(query, RegionField.ID);

            assertEquals(
                    projectedSelect("region.id", "WHERE region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse projected query with multiple fields")
        void testProjectedQueryMultipleFields() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, "TestRegion"))
                    .orderBy(RegionField.ID)
                    .limit(5)
                    .asQuery();
            String sql = parser.parse(query, RegionField.ID, RegionField.NAME, RegionField.WORLD);

            assertEquals(
                    projectedSelect(
                            "region.id, region.name, region.world",
                            "WHERE region.name = 'TestRegion' AND region.destroyed = 0 ORDER BY region.id ASC LIMIT 5"
                    ),
                    sql
            );
        }
    }

    @Nested
    @DisplayName("Absolute Condition Tests")
    class AbsoluteConditionTests {

        @Test
        @DisplayName("Parse Absolute.TRUE condition")
        void testAbsoluteTrueCondition() {
            Query query = Query.builder(null).and(Condition.TRUE).asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE 1=1 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Absolute.FALSE condition")
        void testAbsoluteFalseCondition() {
            Query query = Query.builder(null).and(Condition.FALSE).asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE 1=0 AND region.destroyed = 0"),
                    sql
            );
        }
    }

    @Nested
    @DisplayName("Comparative Condition Tests")
    class ComparativeConditionTests {

        @Test
        @DisplayName("Parse Equal condition with String value")
        void testEqualConditionWithString() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, "TestRegion"))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name = 'TestRegion' AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Equal condition with Number value")
        void testEqualConditionWithNumber() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.MIN_X, 5.0))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.min_x = 5.0 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Equal condition with World value")
        void testEqualConditionWithWorld() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.WORLD, mockWorld))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.world = '" + mockWorld.getUID() + "' AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Equal condition with Boolean true")
        void testEqualConditionWithBooleanTrue() {
            Query query = Query.builder(null)
                    .and(Condition.isTrue(RegionField.DESTROYED))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed != 0 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Equal condition with Boolean false")
        void testEqualConditionWithBooleanFalse() {
            Query query = Query.builder(null)
                    .and(Condition.isFalse(RegionField.DESTROYED))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.destroyed = 0 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Equal condition with null value")
        void testEqualConditionWithNull() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, null))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name IS NULL AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse GreaterThan condition with numeric value")
        void testGreaterThanConditionNumeric() {
            Query query = Query.builder(null)
                    .and(Condition.greaterThan(RegionField.MIN_X, 3.0))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.min_x > 3.0 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse GreaterThan condition with String value")
        void testGreaterThanConditionString() {
            Query query = Query.builder(null)
                    .and(Condition.greaterThan(RegionField.NAME, "A"))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name > 'A' AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse LessThan condition with numeric value")
        void testLessThanConditionNumeric() {
            Query query = Query.builder(null)
                    .and(Condition.lessThan(RegionField.MIN_X, 10.0))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.min_x < 10.0 AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse LessThan condition with String value")
        void testLessThanConditionString() {
            Query query = Query.builder(null)
                    .and(Condition.lessThan(RegionField.NAME, "Z"))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name < 'Z' AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Contains condition")
        void testContainsCondition() {
            Query query = Query.builder(null)
                    .and(Condition.contains(RegionField.NAME, "Test"))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name LIKE '%Test%' AND region.destroyed = 0"),
                    sql
            );
        }

        @Test
        @DisplayName("Parse Contains condition with special characters")
        void testContainsConditionWithSpecialChars() {
            Query query = Query.builder(null)
                    .and(Condition.contains(RegionField.NAME, "Test'Region"))
                    .asQuery();
            String sql = parser.parse(query);

            assertEquals(
                    regionSelect("WHERE region.name LIKE '%Test''Region%' AND region.destroyed = 0"),
                    sql
            );
        }
    }

    @Nested
    @DisplayName("Relational Condition Tests")
    class RelationalConditionTests {

        @Test
        @DisplayName("Parse HasRule condition")
        void testHasRuleCondition() {
            Query query = Query.builder(null)
                    .and(Condition.hasRule("TestRule"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionRule.key = 'TestRule'"));
            assertTrue(sql.contains("LEFT JOIN regionRule ON region.id = regionRule.region_id"));
        }

        @Test
        @DisplayName("Parse HasRule condition with special characters")
        void testHasRuleConditionWithSpecialChars() {
            Query query = Query.builder(null)
                    .and(Condition.hasRule("Test'Rule"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionRule.key = 'Test''Rule'"));
        }

        @Test
        @DisplayName("Parse HasDataKey condition")
        void testHasDataKeyCondition() {
            Query query = Query.builder(null)
                    .and(Condition.hasDataKey("customKey"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionData.key = 'customKey'"));
            assertTrue(sql.contains("LEFT JOIN regionData ON region.id = regionData.region_id"));
        }

        @Test
        @DisplayName("Parse DataValueIs condition")
        void testDataValueIsCondition() {
            Query query = Query.builder(null)
                    .and(Condition.dataValueIs("key1", new JsonPrimitive("value1")))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionData.key = 'key1'"));
            assertTrue(sql.contains("regionData.value = '\"value1\"'"));
            assertTrue(sql.contains("LEFT JOIN regionData ON region.id = regionData.region_id"));
        }

        @Test
        @DisplayName("Parse HasMember condition")
        void testHasMemberCondition() {
            Player mockPlayer = mock(Player.class);
            UUID playerId = UUID.randomUUID();
            when(mockPlayer.getUniqueId()).thenReturn(playerId);

            Query query = Query.builder(null)
                    .and(Condition.hasMember(mockPlayer))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionPermission.player_uuid = '" + playerId.toString() + "'"));
            assertTrue(sql.contains("LEFT JOIN regionPermission ON region.id = regionPermission.region_id"));
        }

        @Test
        @DisplayName("Parse HasMemberWithLevel condition")
        void testHasMemberWithLevelCondition() {
            org.bukkit.entity.Player mockPlayer = mock(org.bukkit.entity.Player.class);
            UUID playerId = UUID.randomUUID();
            when(mockPlayer.getUniqueId()).thenReturn(playerId);

            Query query = Query.builder(null)
                    .and(Condition.hasMemberWithLevel(mockPlayer, 5))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("regionPermission.player_uuid = '" + playerId.toString() + "'"));
            assertTrue(sql.contains("regionPermission.level >= 5"));
            assertTrue(sql.contains("LEFT JOIN regionPermission ON region.id = regionPermission.region_id"));
        }

        @Test
        @DisplayName("Parse IsAllowed condition with no matching hierarchies")
        void testIsAllowedConditionNoHierarchies() {
            org.bukkit.entity.Player mockPlayer = mock(org.bukkit.entity.Player.class);
            Ability mockAbility = mock(Ability.class);

            Query query = Query.builder(null)
                    .and(Condition.isAllowed(mockPlayer, mockAbility))
                    .asQuery();
            String sql = parser.parse(query);

            // With no hierarchies, the condition should be empty
            assertFalse(sql.contains("AND AND"));
        }

        @Test
        @DisplayName("Parse IsAllowed condition with matching hierarchies")
        void testIsAllowedConditionWithHierarchies() {
            org.bukkit.entity.Player mockPlayer = mock(org.bukkit.entity.Player.class);
            Ability mockAbility = mock(Ability.class);
            
            Hierarchy mockHierarchy = mock(Hierarchy.class);
            Hierarchy.Group mockGroup = mock(Hierarchy.Group.class);
            
            when(mockHierarchy.getId()).thenReturn(1L);
            when(mockHierarchy.getLowestGroupAllowedTo(mockAbility)).thenReturn(java.util.Optional.of(mockGroup));
            when(mockGroup.getLevel()).thenReturn(10);
            when(mockHierarchyRepository.getAll()).thenReturn(List.of(mockHierarchy));

            Query query = Query.builder(null)
                    .and(Condition.isAllowed(mockPlayer, mockAbility))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.hierarchy = 1"));
            assertTrue(sql.contains("regionPermission.level <= 10"));
        }
    }

    @Nested
    @DisplayName("Positional Condition Tests")
    class PositionalConditionTests {

        @Test
        @DisplayName("Parse At condition")
        void testAtCondition() {
            Location point = new Location(mockWorld, 100.5, 64, 200.5);
            Query query = Query.builder(null)
                    .and(Condition.at(point))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.world = '" + mockWorld.getUID() + "'"));
            assertTrue(sql.contains("region.min_x <= 100.5"));
            assertTrue(sql.contains("region.max_x >= 100.5"));
            assertTrue(sql.contains("region.min_y <= 64"));
            assertTrue(sql.contains("region.max_y >= 64"));
            assertTrue(sql.contains("region.min_z <= 200.5"));
            assertTrue(sql.contains("region.max_z >= 200.5"));
        }

        @Test
        @DisplayName("Parse In condition with Area")
        void testInCondition() {
            Area area = new Area(0, 0, 0, 100, 100, 100, mockWorld);
            Query query = Query.builder(null)
                    .and(Condition.in(area))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.world = '" + mockWorld.getUID() + "'"));
            assertTrue(sql.contains("region.min_x < 100"));
            assertTrue(sql.contains("region.max_x > 0"));
            assertTrue(sql.contains("region.min_y < 100"));
            assertTrue(sql.contains("region.max_y > 0"));
            assertTrue(sql.contains("region.min_z < 100"));
            assertTrue(sql.contains("region.max_z > 0"));
        }

        @Test
        @DisplayName("Parse InChunk condition")
        void testInChunkCondition() {
            Query query = Query.builder(null)
                    .and(Condition.inChunk(5, 10, mockWorld))
                    .asQuery();
            String sql = parser.parse(query);

            // Chunk 5, 10 starts at (80, _, 160)
            assertTrue(sql.contains("region.world = '" + mockWorld.getUID() + "'"));
            assertTrue(sql.contains("region.min_x < 96"));  // chunkMaxX
            assertTrue(sql.contains("region.max_x > 80"));  // chunkMinX
            assertTrue(sql.contains("region.min_z < 176")); // chunkMaxZ
            assertTrue(sql.contains("region.max_z > 160")); // chunkMinZ
        }
    }

    @Nested
    @DisplayName("Composed Condition Tests")
    class ComposedConditionTests {

        @Test
        @DisplayName("Parse And condition with two conditions")
        void testAndConditionWithTwoConditions() {
            Condition condition = Condition.AND(
                    Condition.equal(RegionField.NAME, "Region1"),
                    Condition.equal(RegionField.MIN_X, 5.0)
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.name = 'Region1'"));
            assertTrue(sql.contains("region.min_x = 5.0"));
            assertTrue(sql.contains("AND"));
        }

        @Test
        @DisplayName("Parse And condition with multiple conditions")
        void testAndConditionWithMultipleConditions() {
            Condition condition = Condition.AND(
                    Condition.equal(RegionField.NAME, "Region1"),
                    Condition.equal(RegionField.MIN_X, 5.0),
                    Condition.isTrue(RegionField.DESTROYED)
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.name = 'Region1'"));
            assertTrue(sql.contains("region.min_x = 5.0"));
            assertTrue(sql.contains("region.destroyed != 0"));
        }

        @Test
        @DisplayName("Parse Or condition with two conditions")
        void testOrConditionWithTwoConditions() {
            Condition condition = Condition.OR(
                    Condition.equal(RegionField.NAME, "Region1"),
                    Condition.equal(RegionField.NAME, "Region2")
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("(region.name = 'Region1')"));
            assertTrue(sql.contains("(region.name = 'Region2')"));
            assertTrue(sql.contains("OR"));
        }

        @Test
        @DisplayName("Parse Or condition with multiple conditions")
        void testOrConditionWithMultipleConditions() {
            Condition condition = Condition.OR(
                    Condition.equal(RegionField.MIN_X, 1.0),
                    Condition.equal(RegionField.MIN_X, 5.0),
                    Condition.equal(RegionField.MIN_X, 10.0)
            );
            Query query = Query.builder(null).and(
                    condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("(region.min_x = 1.0)"));
            assertTrue(sql.contains("(region.min_x = 5.0)"));
            assertTrue(sql.contains("(region.min_x = 10.0)"));
        }

        @Test
        @DisplayName("Parse Not condition")
        void testNotCondition() {
            Condition condition = Condition.not(Condition.equal(RegionField.NAME, "Region1"));
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("NOT"));
            assertTrue(sql.contains("region.name = 'Region1'"));
        }

        @Test
        @DisplayName("Parse double negation")
        void testDoubleNegation() {
            Condition condition = Condition.not(Condition.not(Condition.equal(RegionField.NAME, "Region1")));
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("NOT (NOT (region.name = 'Region1'))"));
        }

        @Test
        @DisplayName("Parse And with Or as child")
        void testAndWithOrChild() {
            Condition condition = Condition.AND(
                    Condition.OR(
                            Condition.equal(RegionField.MIN_X, 5.0),
                            Condition.equal(RegionField.MIN_X, 10.0)
                    ),
                    Condition.equal(RegionField.NAME, "Region1")
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("(region.min_x = 5.0) OR (region.min_x = 10.0)"));
            assertTrue(sql.contains("region.name = 'Region1'"));
        }

        @Test
        @DisplayName("Parse Or with And as child")
        void testOrWithAndChild() {
            Condition condition = Condition.OR(
                    Condition.AND(
                            Condition.equal(RegionField.MIN_X, 5.0),
                            Condition.equal(RegionField.NAME, "Region1")
                    ),
                    Condition.equal(RegionField.MIN_X, 10.0)
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.min_x = 5.0"));
            assertTrue(sql.contains("region.name = 'Region1'"));
            assertTrue(sql.contains("(region.min_x = 10.0)"));
        }

        @Test
        @DisplayName("Parse complex nested condition")
        void testComplexNestedCondition() {
            Condition condition = Condition.AND(
                    Condition.OR(
                            Condition.equal(RegionField.MIN_X, 1.0),
                            Condition.equal(RegionField.MIN_X, 2.0)
                    ),
                    Condition.not(Condition.equal(RegionField.NAME, "Admin")),
                    Condition.greaterThan(RegionField.NAME, "A")
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("(region.min_x = 1.0)"));
            assertTrue(sql.contains("(region.min_x = 2.0)"));
            assertTrue(sql.contains("NOT (region.name = 'Admin')"));
            assertTrue(sql.contains("region.name > 'A'"));
        }
    }

    @Nested
    @DisplayName("Join Generation Tests")
    class JoinGenerationTests {

        @Test
        @DisplayName("No joins for simple Equal condition")
        void testNoJoinForSimpleEqual() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, "Test"))
                    .asQuery();
            String sql = parser.parse(query);

            assertFalse(sql.contains("JOIN"));
        }

        @Test
        @DisplayName("Single join for HasRule")
        void testSingleJoinForHasRule() {
            Query query = Query.builder(null)
                    .and(Condition.hasRule("TestRule"))
                    .asQuery();
            String sql = parser.parse(query);

            long joinCount = sql.split("JOIN").length - 1;
            assertEquals(1, joinCount);
            assertTrue(sql.contains("LEFT JOIN regionRule"));
        }

        @Test
        @DisplayName("No duplicate joins for multiple conditions using same join")
        void testNoDuplicateJoins() {
            Condition condition = Condition.AND(
                    Condition.hasRule("Rule1"),
                    Condition.hasRule("Rule2")
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            long joinCount = sql.split("LEFT JOIN regionRule").length - 1;
            assertEquals(1, joinCount);
        }

        @Test
        @DisplayName("Multiple different joins are included")
        void testMultipleDifferentJoins() {
            Condition condition = Condition.AND(
                    Condition.hasRule("TestRule"),
                    Condition.hasDataKey("customKey")
            );
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("LEFT JOIN regionRule"));
            assertTrue(sql.contains("LEFT JOIN regionData"));
        }

        @Test
        @DisplayName("Joins for positional conditions")
        void testNoJoinsForPositionalConditions() {
            Location point = new Location(mockWorld, 100, 64, 200);
            Query query = Query.builder(null)
                    .and(Condition.at(point))
                    .asQuery();
            String sql = parser.parse(query);

            // Positional conditions should not add joins
            int joinCount = 0;
            if (sql.contains("JOIN regionRule")) joinCount++;
            if (sql.contains("JOIN regionData")) joinCount++;
            if (sql.contains("JOIN regionPermission")) joinCount++;
            
            assertEquals(0, joinCount);
        }
    }

    @Nested
    @DisplayName("SQL Injection Prevention Tests")
    class SqlInjectionPreventionTests {

        @Test
        @DisplayName("Escape single quotes in String conditions")
        void testEscapeSingleQuotesInStringCondition() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, "O'Reilly"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("O''Reilly"));
            assertFalse(sql.contains("O'Reilly'"));
        }

        @Test
        @DisplayName("Escape single quotes in Contains condition")
        void testEscapeSingleQuotesInContains() {
            Query query = Query.builder(null)
                    .and(Condition.contains(RegionField.NAME, "Test'Value"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("Test''Value"));
        }

        @Test
        @DisplayName("Escape single quotes in HasRule condition")
        void testEscapeSingleQuotesInHasRule() {
            Query query = Query.builder(null)
                    .and(Condition.hasRule("Rule'Name"))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("Rule''Name"));
        }

        @Test
        @DisplayName("Escape single quotes in DataValueIs condition")
        void testEscapeSingleQuotesInDataValueIs() {
            Query query = Query.builder(null)
                    .and(Condition.dataValueIs("key'1", new JsonPrimitive("val'ue")))
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("key''1"));
            assertTrue(sql.contains("val''ue"));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCaseTests {

        @Test
        @DisplayName("Parse query with Absolute.TRUE and includeDestroyed")
        void testAbsoluteTrueWithDestroyedFilter() {
            Query query = Query.builder(null)
                    .and(Condition.TRUE)
                    .isDestroyed(false)
                    .asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("1=1"));
            assertTrue(sql.contains("region.destroyed = 0"));
        }

        @Test
        @DisplayName("Parse And and Or condition with empty list")
        void testAndConditionWithEmptyList() {
            Condition condition = Condition.AND(List.of());
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query),
                    expected = regionSelect("WHERE region.destroyed = 0");

            assertEquals(expected, sql);

            condition = Condition.OR(List.of());
            query = Query.builder(null).and(condition).asQuery();
            sql = parser.parse(query);

            assertEquals(expected, sql);
        }

        @Test
        @DisplayName("Parse And condition with single condition")
        void testAndConditionWithSingleCondition() {
            Condition condition = Condition.AND(Condition.equal(RegionField.NAME, "Test"));
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.name = 'Test'"));
        }

        @Test
        @DisplayName("Parse Or condition with single condition")
        void testOrConditionWithSingleCondition() {
            Condition condition = Condition.OR(Condition.equal(RegionField.NAME, "Test"));
            Query query = Query.builder(null).and(condition).asQuery();
            String sql = parser.parse(query);

            assertTrue(sql.contains("region.name = 'Test'"));
        }

        @Test
        @DisplayName("Ensure SELECT clause always appears first")
        void testSelectClauseFirst() {
            Query query = Query.builder(null)
                    .and(Condition.AND(
                            Condition.hasRule("Rule1"),
                            Condition.equal(RegionField.NAME, "Test")
                    ))
                    .asQuery();
            String sql = parser.parse(query);

            int selectIndex = sql.indexOf("SELECT");
            int joinIndex = sql.indexOf("JOIN");
            int whereIndex = sql.indexOf("WHERE");

            assertTrue(selectIndex < joinIndex);
            assertTrue(joinIndex < whereIndex);
        }

        @Test
        @DisplayName("Ensure WHERE clause always appears last (before LIMIT/ORDER BY)")
        void testWhereClausePlacement() {
            Query query = Query.builder(null)
                    .and(Condition.equal(RegionField.NAME, "Test"))
                    .orderBy(RegionField.NAME)
                    .limit(10)
                    .asQuery();
            String sql = parser.parse(query);

            int whereIndex = sql.indexOf("WHERE");
            int orderIndex = sql.indexOf("ORDER BY");
            int limitIndex = sql.indexOf("LIMIT");

            assertTrue(whereIndex < orderIndex);
            assertTrue(orderIndex < limitIndex);
        }

        @Test
        void testIncludeDestroyed() {
            Query query = Query.builder(null).nameIs("TestRegion").asQuery();
            String sql = parser.parse(query);
            assertEquals(
                    regionSelect("WHERE region.name = 'TestRegion' AND region.destroyed = 0"),
                    sql
            );

            query = Query.builder(null).nameIs("TestRegion").includeDestroyed().asQuery();
            sql = parser.parse(query);
            assertEquals(
                    regionSelect("WHERE region.name = 'TestRegion'"),
                    sql
            );

            query = Query.builder(null)
                    .nameIs("TestRegion")
                    .isEnabled()
                    .greaterThan(RegionField.ID, 15L)
                    .asQuery();
            sql = parser.parse(query);
            assertEquals(
                    regionSelect("WHERE region.name = 'TestRegion' AND region.enabled != 0 AND region.id > 15 AND region.destroyed = 0"),
                    sql
            );

            query = Query.builder(null)
                        .nameIs("TestRegion")
                        .isEnabled()
                    .or()
                        .greaterThan(RegionField.ID, 15L)
                    .or()
                        .idIs(42L)
                    .asQuery();
            sql = parser.parse(query);
            assertEquals(
                    regionSelect("WHERE ((region.name = 'TestRegion' AND region.enabled != 0) OR (region.id > 15) OR (region.id = 42)) AND region.destroyed = 0"),
                    sql
            );

        }
    }
}
