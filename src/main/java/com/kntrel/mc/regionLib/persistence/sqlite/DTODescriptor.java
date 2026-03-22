package com.kntrel.mc.regionLib.persistence.sqlite;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class DTODescriptor {

    //ASSETS
    private static final Map<Class<?>, Class<?>> WRAPPERS = Map.ofEntries(
        Map.entry(int.class, Integer.class),
        Map.entry(long.class, Long.class),
        Map.entry(double.class, Double.class),
        Map.entry(float.class, Float.class),
        Map.entry(boolean.class, Boolean.class),
        Map.entry(byte.class, Byte.class),
        Map.entry(char.class, Character.class),
        Map.entry(short.class, Short.class)
    );

    record Column(int index, String name, Parameter constructorParameter, RecordComponent component, boolean isId) {
        public Class<?> type() { return WRAPPERS.getOrDefault(this.component.getType(), this.component.getType()); }
        public Method accessor() { return this.component().getAccessor(); }
    }
    record Projection(DTODescriptor descriptor, List<Column> columns) {
        Projection {
            columns = List.copyOf(columns);
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("Projection must contain at least one column.");
            }
        }
    }


    //FIELDS
    private final Class<?> dtoClass_;
    private final String tableName_;
    private final Constructor<?> constructor_;
    private final List<Column> columns_;
    private final Map<String, Column> columnMap_;
    private final List<Column> idColumns_;
    private final Projection fullProjection_;
    private final String updateSQL_, insertSQL_, deleteSQL_;


    //CONSTRUCTOR
    DTODescriptor(Class<?> dtoClass) {
        ensureRecord(dtoClass);
        this.dtoClass_ = dtoClass;
        this.tableName_ = findTableName(dtoClass);
        this.constructor_ = findConstructor(dtoClass);
        this.columns_ = findColumns(dtoClass, this.constructor_);
        this.columnMap_ = indexColumns(this.columns_);
        this.idColumns_ = this.columns_.stream().filter(Column::isId).toList();
        this.fullProjection_ = new Projection(this, this.columns_);
        this.insertSQL_ = buildInsert(this.tableName_, this.columns_);
        this.updateSQL_ = buildUpdate(this.tableName_, this.columns_);
        this.deleteSQL_ = buildDelete(this.tableName_, this.columns_);
    }


    //GETTERS
    public Class<?> getType() {
        return this.dtoClass_;
    }
    public String getTableName() {
        return this.tableName_;
    }
    public Constructor<?> getConstructor() {
        return this.constructor_;
    }
    public List<Column> getColumns() {
        return this.columns_;
    }
    public List<Column> getIdColumns() {
        return this.idColumns_;
    }
    public Optional<Column> getColumn(String name) {
        return Optional.ofNullable(this.columnMap_.get(name));
    }
    public Projection fullProjection() {
        return this.fullProjection_;
    }
    public Projection project(String... columnNames) {
        return this.project(Arrays.asList(columnNames));
    }
    public Projection project(Iterable<String> columnNames) {
        List<Column> columns = new ArrayList<>();
        for (String columnName : columnNames) {
            Column column = this.getColumn(columnName).orElseThrow(() ->
                    new IllegalArgumentException("Unknown column '" + columnName + "' for DTO " + this.dtoClass_.getName())
            );
            columns.add(column);
        }
        return new Projection(this, columns);
    }
    public String getUpdateSQL() {
        return this.updateSQL_;
    }
    public String getInsertSQL() {
        return this.insertSQL_;
    }
    public String getDeleteSQL() {
        return this.deleteSQL_;
    }


    //HELPERS
    private static void ensureRecord(Class<?> dtoClass) {
        if (!dtoClass.isRecord()) {
            throw new IllegalArgumentException("DTO class must be a record type.");
        }
    }
    private static String findTableName(Class<?> dtoClass) {
        DTO.Table tableAnnotation = dtoClass.getAnnotation(DTO.Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isEmpty()) {
            return tableAnnotation.value();
        } else {
            return dtoClass.getSimpleName();
        }
    }
    private static Constructor<?> findConstructor(Class<?> dtoClass) {
        Constructor<?>[] constructors = dtoClass.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException("DTO record must consist only of the canonical constructor");
        }
        return constructors[0];
    }
    private static List<Column> findColumns(Class<?> dtoClass, Constructor<?> constructor) {
        RecordComponent[] components = dtoClass.getRecordComponents();
        Parameter[] constructorParameters = constructor.getParameters();
        List<Column> out = new ArrayList<>();
        for (int i = 0; i < components.length; i++) {
            Parameter constructorParameter = constructorParameters[i];
            RecordComponent component = components[i];
            DTO.Column columnAnnotation = component.getAnnotation(DTO.Column.class);
            if (columnAnnotation == null) {
                columnAnnotation = constructorParameter.getAnnotation(DTO.Column.class);
            }
            String columnName = (columnAnnotation == null)
                    ? component.getName()
                    : columnAnnotation.value();
            out.add(new Column(
                    i,
                    columnName,
                    constructorParameter,
                    component,
                    component.isAnnotationPresent(DTO.Id.class) || constructorParameter.isAnnotationPresent(DTO.Id.class)
            ));
        }
        return List.copyOf(out);
    }
    private static Map<String, Column> indexColumns(List<Column> columns) {
        Map<String, Column> out = new HashMap<>();
        for (Column column : columns) {
            Column previous = out.put(column.name(), column);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate column '" + column.name() + "' in DTO descriptor.");
            }
        }
        return Map.copyOf(out);
    }
    private static String buildInsert(String tableName, List<Column> columns) {
        return    "INSERT INTO "
                + tableName
                + "("
                + String.join(", ", columns.stream().map(Column::name).toList())
                + ") VALUES ("
                + String.join(", ", Collections.nCopies(columns.size(), "?"))
                + ");";
    }
    private static String buildUpdate(String tableName, List<Column> columns) {
        List<Column> idColumns = columns.stream().filter(Column::isId).toList();
        List<String> nonIdColumns = columns.stream().filter(col -> !col.isId()).map(col -> col.name() + " = ?").toList();
        List<String> idConditions = idColumns.stream().map(col -> col.name() + " = ?").toList();
        return    "UPDATE "
                + tableName
                + " SET "
                + String.join(", ", nonIdColumns)
                + " WHERE "
                + String.join(" AND ", idConditions)
                + ";";
    }
    private static String buildDelete(String tableName, List<Column> columns) {
        List<Column> idColumns = columns.stream().filter(Column::isId).toList();
        List<String> idConditions = idColumns.stream().map(col -> col.name() + " = ?").toList();
        return    "DELETE FROM "
                + tableName
                + " WHERE "
                + String.join(" AND ", idConditions)
                + ";";
    }
}


