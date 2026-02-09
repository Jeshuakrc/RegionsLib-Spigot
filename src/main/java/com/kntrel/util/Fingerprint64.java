package com.kntrel.util;

import java.util.UUID;

public final class Fingerprint64 {
    private long h_;

    public Fingerprint64(long seed) {
        this.h_ = seed;
    }

    private static long mix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private void addLongRaw(long x) {
        this.h_ ^= mix64(x);
        this.h_ *= 0x9E3779B97F4A7C15L;
    }

    public Fingerprint64 addLong(long x) {
        addLongRaw(x); return this;
    }
    public Fingerprint64 addInt(int x) {
        addLongRaw(x); return this;
    }
    public Fingerprint64 addBool(boolean b) {
        addLongRaw(b ? 1L : 0L); return this;
    }
    public Fingerprint64 addUUID(UUID u) {
        addLongRaw(u.getMostSignificantBits());
        addLongRaw(u.getLeastSignificantBits());
        return this;
    }
    public Fingerprint64 addString(String s) {
        if (s == null) { addLongRaw(0L); return this; }
        addInt(s.length());
        for (int i = 0; i < s.length(); i++) {
            addInt(s.charAt(i));
        }
        return this;
    }
    public Fingerprint64 addDouble(double d) {
        addLongRaw(Double.doubleToLongBits(d));
        return this;
    }

    public long finish() {
        return mix64(h_);
    }
}