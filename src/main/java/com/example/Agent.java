package com.example;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class Agent {
    private final Main main;
    Point2D pos;
    Point2D vel;
    double speed = 2;
    double heading;
    double hp = 100;
    final List<Boost> boosts = new ArrayList<>();

    Agent(Main main, Point2D p) {
        this.main = main;
        pos = p;
        heading = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        vel = new Point2D(Math.cos(heading), Math.sin(heading));
    }

    void update() {
        boosts.removeIf(Boost::expired);
        double boostFactor = boosts.stream().mapToDouble(b -> b.factor).reduce(1.0, (a, b) -> a * b);
        double dx = vel.x() * speed * boostFactor;
        double dy = vel.y() * speed * boostFactor;
        pos = new Point2D((pos.x() + dx + Main.WIDTH) % Main.WIDTH,
                (pos.y() + dy + Main.HEIGHT) % Main.HEIGHT);
        main.trails.computeIfAbsent(this, k -> new ArrayList<>()).add(new Trail(pos, speedColor()));
    }

    double facingScore(Agent other) {
        double angleToOther = Math.atan2(other.pos.y() - pos.y(), other.pos.x() - pos.x());
        double diff = Math.abs(Math.atan2(Math.sin(angleToOther - heading), Math.cos(angleToOther - heading)));
        return Math.PI - diff;
    }

    void steal(Agent victim) {
        if (victim.hp <= 0 || this == victim) return;
        hp = Math.min(100, hp + 10);
        victim.hp -= 10;
        boosts.add(new Boost(1.05, 30000));
        if (victim.hp <= 0) {
            speed *= 1.02;
            var dead = new AgentDead(this, victim);
            main.handleEvent(dead);
        }
    }

    Color healthColor() {
        int g = (int) Math.min(255, hp / 100 * 255);
        int r = 255 - g;
        return new Color(r, g, 0);
    }

    Color speedColor() {
        float s = (float) Math.min(1.0, speed / 5);
        return new Color(Color.HSBtoRGB(0.6f * (1 - s), 1f, 1f));
    }
}
