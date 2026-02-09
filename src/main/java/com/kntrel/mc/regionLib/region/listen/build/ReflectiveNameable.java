package com.kntrel.mc.regionLib.region.listen.build;

/**
 * Allows a builder to set a name derived from reflection.
 *
 * @param <T> builder type
 */
public interface ReflectiveNameable<T> {

    T namedAs(String name);
}
