package com.example;

record Point2D(double x, double y) {
    Point2D add(Point2D other) { return new Point2D(x + other.x, y + other.y); }
    Point2D mul(double s) { return new Point2D(x * s, y * s); }
    double distance(Point2D o) { return Math.hypot(x - o.x, y - o.y); }
}
