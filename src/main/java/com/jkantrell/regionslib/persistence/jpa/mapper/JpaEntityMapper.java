package com.jkantrell.regionslib.persistence.jpa.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface JpaEntityMapper<T, E extends JpaEntity<T>> {

    //STATIC
    public static <T, E extends JpaEntity<T>> E toEntity(Class<E> clazz, T src) {
        if (src == null) {
            return null;
        }

        // instantiate the entity
        E entity;
        try {
            entity = clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(clazz.getName() + " initialization error: " + e.getMessage(), e);
        }

        // introspect the model (src) and the entity (clazz)
        BeanInfo modelInfo, entityInfo;
        try {
            modelInfo = Introspector.getBeanInfo(src.getClass(), Object.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Error introspecting " + src.getClass().getName() + ": " + e.getMessage(), e);
        }
        try {
            entityInfo = Introspector.getBeanInfo(clazz, Object.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Error introspecting " + clazz.getName() + ": " + e.getMessage(), e);
        }

        // map entity property name → PropertyDescriptor (for quick lookup)
        Map<String, PropertyDescriptor> entityPdMap = Stream.of(entityInfo.getPropertyDescriptors())
                .collect(Collectors.toMap(PropertyDescriptor::getName, d -> d));

        // For each readable model property, copy it into the entity if possible
        for (PropertyDescriptor modelPd : modelInfo.getPropertyDescriptors()) {
            Method read = modelPd.getReadMethod();
            if (read == null) {
                continue;
            }
            PropertyDescriptor entityPd = entityPdMap.get(modelPd.getName());
            if (entityPd == null) {
                continue;
            }
            Method write = entityPd.getWriteMethod();
            if (write == null) {
                continue;
            }
            // ensure types are compatible (e.g. same or assignable)
            Class<?> returnType = read.getReturnType();
            Class<?> paramType  = write.getParameterTypes()[0];
            if (!paramType.isAssignableFrom(returnType)) {
                continue;
            }

            // invoke getter on src, then setter on newly created entity
            Object value;
            try {
                value = read.invoke(src);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error invoking reader method '" + src.getClass().getName() + "#" + read.getName() + ": " + e.getMessage(), e);
            }
            try {
                write.invoke(entity, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error invoking writer method '" + clazz.getName() + "#" + write.getName() + ": " + e.getMessage(), e);
            }
        }

        return entity;
    }


    //DEFINITION
    Class<T> getModelClass();
    Class<E> getEntityClass();

    default E toEntity(T src) {
        return JpaEntityMapper.toEntity(this.getEntityClass(), src);
    }

    T toModel(E src);
}
