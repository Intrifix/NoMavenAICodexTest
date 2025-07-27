package com.example;

final class Boost {
    final double factor;
    final long end;
    Boost(double factor, long durationMs) {
        this.factor = factor;
        this.end = System.currentTimeMillis() + durationMs;
    }
    boolean expired() { return System.currentTimeMillis() > end; }
}
