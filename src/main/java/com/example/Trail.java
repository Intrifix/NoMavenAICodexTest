package com.example;

import java.awt.Color;

final class Trail {
    private final Point2D pos;
    private final Color color;
    private final long createTime = System.currentTimeMillis();
    Trail(Point2D pos, Color color) {
        this.pos = pos; this.color = color;
    }
    boolean expired() { return System.currentTimeMillis() - createTime > 2000; }
    float alpha() { return 1f - (System.currentTimeMillis() - createTime) / 2000f; }
    Point2D pos() { return pos; }
    Color color() { return color; }
}
