package com.kntrel.mc.regionLib.persistence.sqlite;

import org.jetbrains.annotations.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

class DataBase {

    //FIELDS
    private final Connection conn_;
    private final Map<Class<?>, DTODescriptor> cache_;


    //CONSTRUCTORS
    DataBase(Connection connection) {
        this.conn_ = connection;
        this.cache_ = new HashMap<>();
    }


    //API
    public <T> List<T> query(String sql, Class<T> dtoClass, int limit) throws SQLException {
        try (PreparedStatement stmt = this.conn_.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            return this.buildDTOs(rs, dtoClass, limit);
        }
    }
    public <T> List<T> query(String sql, Class<T> dtoClass) throws SQLException {
        return this.query(sql, dtoClass, -1);
    }
    public <T> Optional<T> queryOne(String sql, Class<T> dtoClass) throws SQLException {
        List<T> r = this.query(sql, dtoClass, 1);
        if (r.isEmpty()) { return Optional.empty(); }
        return Optional.of(r.getFirst());
    }

    public void write(@Nullable Collection<?> inserts, @Nullable Collection<?> updates, @Nullable Collection<?> deletes) throws SQLException {
        if (inserts == null && updates == null && deletes == null) { return; }

        Map<Class<?>, List<Object>> insertGroups = null, updateGroups = null, deleteGroups = null;
        if (inserts != null) {
            insertGroups = inserts.stream().collect(Collectors.groupingBy(Object::getClass));
        }
        if (updates != null) {
            updateGroups = updates.stream().collect(Collectors.groupingBy(Object::getClass));
        }
        if (deletes != null) {
            deleteGroups = deletes.stream().collect(Collectors.groupingBy(Object::getClass));
        }

        try {
            this.conn_.setAutoCommit(false);

            if (insertGroups != null) {
                for (Map.Entry<Class<?>, List<Object>> entry : insertGroups.entrySet()) {
                    this.insertToTable(this.getDescriptor(entry.getKey()), entry.getValue());
                }
            }
            if (updateGroups != null) {
                for (Map.Entry<Class<?>, List<Object>> entry : updateGroups.entrySet()) {
                    this.updateTable(this.getDescriptor(entry.getKey()), entry.getValue());
                }
            }
            if (deleteGroups != null) {
                for (Map.Entry<Class<?>, List<Object>> entry : deleteGroups.entrySet()) {
                    this.deleteFromTable(this.getDescriptor(entry.getKey()), entry.getValue());
                }
            }

            this.conn_.commit();
        } catch (SQLException e) {
            this.conn_.rollback();
            throw e;
        } finally {
            this.conn_.setAutoCommit(true);
        }
    }
    public void insert(Collection<?> DTOs) throws SQLException {
        this.write(DTOs, null, null);
    }
    public void update(Collection<?> DTOs) throws SQLException {
        this.write(null, DTOs, null);
    }
    public void delete(Collection<?> DTOs) throws SQLException {
        this.write(null, null, DTOs);
    }
    public Connection getConnection() {
        return this.conn_;
    }

    //PRIVATE
    private DTODescriptor getDescriptor(Class<?> dtoClass) {
        return this.cache_.computeIfAbsent(dtoClass, DTODescriptor::new);
    }
    @SuppressWarnings("unchecked")
    private <T> List<T> buildDTOs(ResultSet rs, Class<T> dtoClass, int limit) throws SQLException {
        DTODescriptor descriptor = this.getDescriptor(dtoClass);
        List<DTODescriptor.Column> cols = descriptor.getColumns();
        List<T> dtos = new ArrayList<>();

        while (rs.next()) {
            if (limit >= 0 && limit-- == 0) { break; }
            Object[] args = new Object[cols.size()];
            int index = 0;
            for (DTODescriptor.Column column : cols) {
                Object value = rs.getObject(column.name(), column.type());
                args[index++] = value;
            }
            try {
                T dto = (T) descriptor.getConstructor().newInstance(args);
                dtos.add(dto);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to instantiate DTO of type " + dtoClass.getName(), e);
            }
        }

        return dtos;
    }
    private void insertToTable(DTODescriptor desc, Iterable<?> instances) throws SQLException {
        List<DTODescriptor.Column> cols = desc.getColumns();
        try (PreparedStatement stmt = this.conn_.prepareStatement(desc.getInsertSQL())) {
            for (Object obj : instances) {
                for (DTODescriptor.Column col : cols) {
                    Object value;
                    try {
                        value = col.accessor().invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to access value of column " + col.name() + " for DTO of type " + obj.getClass().getName(), e);
                    }
                    stmt.setObject(col.index() + 1, value);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    private void updateTable(DTODescriptor desc, Iterable<?> instances) throws SQLException {
        List<DTODescriptor.Column> cols = desc.getColumns();
        List<DTODescriptor.Column> idCols = desc.getIdColumns();
        try (PreparedStatement stmt = this.conn_.prepareStatement(desc.getUpdateSQL())) {
            for (Object obj : instances) {
                int index = 1;
                for (DTODescriptor.Column col : cols) {
                    if (col.isId()) { continue; }
                    Object value;
                    try {
                        value = col.accessor().invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to access value of column " + col.name() + " for DTO of type " + obj.getClass().getName(), e);
                    }
                    stmt.setObject(index++, value);
                }
                for (DTODescriptor.Column idCol : idCols) {
                    Object value;
                    try {
                        value = idCol.accessor().invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to access value of ID column " + idCol.name() + " for DTO of type " + obj.getClass().getName(), e);
                    }
                    stmt.setObject(index++, value);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    private void deleteFromTable(DTODescriptor desc, Iterable<?> instances) throws SQLException {
        List<DTODescriptor.Column> idCols = desc.getIdColumns();
        try (PreparedStatement stmt = this.conn_.prepareStatement(desc.getDeleteSQL())) {
            for (Object obj : instances) {
                int index = 1;
                for (DTODescriptor.Column idCol : idCols) {
                    Object value;
                    try {
                        value = idCol.accessor().invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to access value of ID column " + idCol.name() + " for DTO of type " + obj.getClass().getName(), e);
                    }
                    stmt.setObject(index++, value);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}