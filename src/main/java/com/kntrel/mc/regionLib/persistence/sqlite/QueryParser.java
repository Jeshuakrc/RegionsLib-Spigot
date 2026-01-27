package com.kntrel.mc.regionLib.persistence.sqlite;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.region.RegionField;
import com.kntrel.mc.regionLib.region.hierarchy.Hierarchy;
import com.kntrel.mc.regionLib.region.repository.Condition;
import com.kntrel.mc.regionLib.region.repository.Query;
import com.kntrel.mc.regionLib.util.Area;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.*;

class QueryParser {


    //CONSTANTS
    private static final String SELECT_CLAUSE = "SELECT DISTINCT region.* FROM region";


    //FIELDS
    private final RegionContext ctx_;


    //CONSTRUCTOR
    QueryParser(RegionContext ctx) {
        this.ctx_ = ctx;
    }


    //API
    public String parse(Query query) {
        StringBuilder sql = new StringBuilder(SELECT_CLAUSE);
        Set<String> joins = new HashSet<>();

        // Collect required joins from condition
        Condition condition = query.getCondition();
        if (condition != null) {
            collectJoins(condition, joins);
        }

        // Add JOIN clauses
        for (String join : joins) {
            sql.append(" LEFT JOIN ").append(join);
        }

        // Build WHERE clause
        String whereClause = buildWhereClause(query);
        if (!whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }

        // Build ORDER BY clause
        Optional<Query.Ordering> ordering = query.getOrdering();
        if (ordering.isPresent()) {
            sql.append(" ORDER BY region.").append(ordering.get().field().getName());
            sql.append(ordering.get().ascending() ? " ASC" : " DESC");
        }

        // Build LIMIT clause
        if (query.getLimit() > 0) {
            sql.append(" LIMIT ").append(query.getLimit());
        }

        sql.append(";");
        return sql.toString();
    }


    //HELPERS
    private String buildWhereClause(Query query) {
        List<String> clauses = new ArrayList<>();

        // Add condition if present
        Condition condition = query.getCondition();
        if (condition != null) {
            String conditionSql = this.parseCondition(condition);
            if (condition instanceof Condition.Or && !query.includesDestroyed()) {
                conditionSql = "(" + conditionSql + ")";
            }
            if (!conditionSql.isEmpty()) {
                clauses.add(conditionSql);
            }
        }

        // Add destroyed filter if needed
        if (!query.includesDestroyed()) {
            clauses.add("region.destroyed = 0");
        }

        return String.join(" AND ", clauses);
    }

    private static void collectJoins(Condition condition, Set<String> joins) {

        String join = switch (condition) {
            case Condition.HasRule ignore -> "regionRule ON region.id = regionRule.region_id";
            case Condition.HasDataKey ignore -> "regionData ON region.id = regionData.region_id";
            case Condition.DataValueIs ignore -> "regionData ON region.id = regionData.region_id";
            case Condition.HasMember ignore -> "regionPermission ON region.id = regionPermission.region_id";
            case Condition.HasMemberWithLevel ignore -> "regionPermission ON region.id = regionPermission.region_id";
            case Condition.IsAllowed ignore -> "regionPermission ON region.id = regionPermission.region_id";

            default -> "";
        };
        if (!join.isEmpty()) {
            joins.add(join);
            return;
        }

        switch (condition) {
            case Condition.And and -> and.conditions().forEach(c -> collectJoins(c, joins));
            case Condition.Or or -> or.conditions().forEach(c -> collectJoins(c, joins));
            case Condition.Not not -> collectJoins(not.condition(), joins);
            default -> {}
        }
    }


    private String parseCondition(Condition condition) {
        return switch (condition) {
            case Condition.Absolute absolute -> absolute.getValue() ? "1=1" : "1=0";

            case Condition.And and -> and.conditions().stream()
                    .map(this::parseCondition)
                    .filter(s -> !s.isEmpty())
                    .reduce((a, b) -> a + " AND " + b)
                    .orElse("");

            case Condition.Or or -> or.conditions().stream()
                    .map(this::parseCondition)
                    .filter(s -> !s.isEmpty())
                    .map(s -> "(" + s + ")")
                    .reduce((a, b) -> a + " OR " + b)
                    .orElse("");

            case Condition.Not not -> "NOT (" + parseCondition(not.condition()) + ")";

            case Condition.Equal<?> equal -> parseEqual(equal);

            case Condition.Comparative<?> comparative -> parseComparative(comparative);

            case Condition.HasRule hasRule -> "regionRule.key = '" + escapeSqlString(hasRule.ruleName()) + "'";

            case Condition.RuleIs<?> ruleIs -> "regionRule.key = '" + escapeSqlString(ruleIs.rule().getName()) + "' AND regionRule.value = '" + escapeSqlString(String.valueOf(ruleIs.value())) + "'";

            case Condition.HasDataKey hasDataKey -> "regionData.key = '" + escapeSqlString(hasDataKey.key()) + "'";

            case Condition.DataValueIs dataValueIs -> "regionData.key = '" + escapeSqlString(dataValueIs.key()) + "' AND regionData.value = '" + escapeSqlString(dataValueIs.value().toString()) + "'";

            case Condition.HasMember hasMember -> "regionPermission.player_uuid = '" + escapeSqlString(hasMember.player().getUniqueId().toString()) + "'";

            case Condition.HasMemberWithLevel hasMemberWithLevel -> "regionPermission.player_uuid = '" + escapeSqlString(hasMemberWithLevel.player().getUniqueId().toString()) + "' AND regionPermission.level >= " + hasMemberWithLevel.level();
            case Condition.IsAllowed isAllowed -> this.parseIsAllowed(isAllowed);

            // Positional conditions: convert to AND conditions of comparisons
            case Condition.At at -> parseAtCondition(at);
            case Condition.In in -> parseInCondition(in);
            case Condition.InChunk inChunk -> parseInChunkCondition(inChunk);
        };
    }

    private String parseIsAllowed(Condition.IsAllowed isAllowed) {
        StringJoiner sj = new StringJoiner(") OR (", "((", "))");
        sj.setEmptyValue("");
        List<Hierarchy> hierarchies = this.ctx_.getHierarchyRepository().getAll();
        List<Long> restricted = new ArrayList<>();
        for (Hierarchy hierarchy : hierarchies) {
            Hierarchy.Group g = hierarchy.getLowestGroupAllowedTo(isAllowed.ability()).orElse(null);
            if (g == null) { continue; }
            restricted.add(hierarchy.getId());
            sj.add("region.hierarchy = " + hierarchy.getId() + " AND regionPermission.level <= " + g.getLevel());
        }
        if (restricted.size() != hierarchies.size()) {
            sj.add("region.hierarchy NOT IN (" + String.join(", ", restricted.stream().map(String::valueOf).toList()) + ")");
        }
        return sj.toString();
    }

    private static String parseComparative(Condition.Comparative<?> comparative) {
        RegionField<?> field = comparative.field();
        String fieldName = "region." + field.getName();
        Object value = comparative.value();
        if (value == null) {
            return (comparative instanceof Condition.Equal<?>)
                    ? fieldName + " IS NULL"
                    : "";
        }

        return switch (comparative) {
            case Condition.Contains contains -> fieldName + " LIKE '%" + escapeSqlString(contains.value()) + "%'";
            case Condition.Equal<?> equal -> parseEqual(equal);
            case Condition.GreaterThan<?> gt -> compare(gt.field(), gt.value(), ">");
            case Condition.GreaterThanEqual<?> gte -> compare(gte.field(), gte.value(), ">=");
            case Condition.LessThan<?> lt -> compare(lt.field(), lt.value(), "<");
            case Condition.LessThanEqual<?> lte -> compare(lte.field(), lte.value(), "<=");
        };
    }

    private static String parseEqual(Condition.Equal<?> equal) {
        RegionField<?> field = equal.field();
        Object value = equal.value();

        String fieldName = "region." + field.getName();

        if (value == null) {
            return fieldName + " IS NULL";
        }

        return switch (value) {
            case Boolean boolValue -> fieldName + " " + (boolValue ? "!=" : "=") + " 0";
            case World world -> fieldName + " = '" + escapeSqlString(world.getUID().toString()) + "'";
            case String strValue -> fieldName + " = '" + escapeSqlString(strValue) + "'";
            case Number numValue -> fieldName + " = " + numValue;
            default -> "";
        };
    }

    private static String compare(RegionField<?> field, Object value, String operator) {
        String fieldName = "region." + field.getName();

        if (value instanceof Number numValue) {
            return fieldName + " " + operator + " " + numValue;
        } else if (value instanceof String strValue) {
            return fieldName + " " + operator + " '" + escapeSqlString(strValue) + "'";
        }

        return "";
    }

    private static String escapeSqlString(String str) {
        return str.replace("'", "''");
    }

    private String parseAtCondition(Condition.At at) {
        Location point = at.point();
        List<Condition> conditions = new ArrayList<>();

        // For each axis: min <= value <= max
        conditions.add(Condition.lessThanEqual(RegionField.MIN_X, point.getX()));
        conditions.add(Condition.greaterThanEqual(RegionField.MAX_X, point.getX()));
        conditions.add(Condition.lessThanEqual(RegionField.MIN_Y, point.getY()));
        conditions.add(Condition.greaterThanEqual(RegionField.MAX_Y, point.getY()));
        conditions.add(Condition.lessThanEqual(RegionField.MIN_Z, point.getZ()));
        conditions.add(Condition.greaterThanEqual(RegionField.MAX_Z, point.getZ()));
        conditions.add(Condition.equal(RegionField.WORLD, point.getWorld()));

        return this.parseCondition(Condition.AND(conditions));
    }


    private String parseInCondition(Condition.In in) {
        Area area = in.area();
        List<Condition> conditions = new ArrayList<>();

        // For each axis: region.min < area.max AND region.max > area.min
        conditions.add(Condition.lessThan(RegionField.MIN_X, area.getMaxX()));
        conditions.add(Condition.greaterThan(RegionField.MAX_X, area.getMinX()));
        conditions.add(Condition.lessThan(RegionField.MIN_Y, area.getMaxY()));
        conditions.add(Condition.greaterThan(RegionField.MAX_Y, area.getMinY()));
        conditions.add(Condition.lessThan(RegionField.MIN_Z, area.getMaxZ()));
        conditions.add(Condition.greaterThan(RegionField.MAX_Z, area.getMinZ()));
        conditions.add(Condition.equal(RegionField.WORLD, area.getWorld()));

        return this.parseCondition(Condition.AND(conditions));
    }

    private String parseInChunkCondition(Condition.InChunk inChunk) {
        double chunkMinX = inChunk.x() << 4;
        double chunkMinZ = inChunk.z() << 4;
        double chunkMaxX = chunkMinX + 16.0;
        double chunkMaxZ = chunkMinZ + 16.0;
        World world = inChunk.world();

        List<Condition> conditions = new ArrayList<>();

        // Check for overlap: region.min < chunk.max AND region.max > chunk.min
        conditions.add(Condition.lessThan(RegionField.MIN_X, chunkMaxX));
        conditions.add(Condition.greaterThan(RegionField.MAX_X, chunkMinX));
        conditions.add(Condition.lessThan(RegionField.MIN_Z, chunkMaxZ));
        conditions.add(Condition.greaterThan(RegionField.MAX_Z, chunkMinZ));
        conditions.add(Condition.equal(RegionField.WORLD, world));

        return this.parseCondition(Condition.AND(conditions));
    }
}
