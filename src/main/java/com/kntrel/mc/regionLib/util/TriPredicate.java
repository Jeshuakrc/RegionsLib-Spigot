package com.kntrel.mc.regionLib.util;

@FunctionalInterface
public interface TriPredicate<A, B, C> {

    boolean test(A a, B b, C c);

}
