package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Main extends JPanel {
    static final int WIDTH = 1000;
    static final int HEIGHT = 900;
    static final int NUM_AGENTS = 10;

    private final List<Agent> agents = new CopyOnWriteArrayList<>();
    private final Map<Agent, List<Trail>> trails = new ConcurrentHashMap<>();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        for (int i = 0; i < NUM_AGENTS; i++) {
            agents.add(new Agent(new Point2D(ThreadLocalRandom.current().nextDouble(WIDTH),
                    ThreadLocalRandom.current().nextDouble(HEIGHT))));
        }
        exec.scheduleAtFixedRate(this::update, 0, 16, TimeUnit.MILLISECONDS);
    }

    private void update() {
        agents.forEach(a -> a.update());
        handleInteractions();
        trails.values().forEach(list -> list.removeIf(Trail::expired));
        repaint();
    }

    private void handleInteractions() {
        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                Agent a = agents.get(i);
                Agent b = agents.get(j);
                double dist = a.pos.distance(b.pos);
                if (dist < 20) {
                    talk(a, b);
                }
            }
        }
    }

    private void talk(Agent a, Agent b) {
        Agent talker = a.facingScore(b) > b.facingScore(a) ? a : b;
        Agent listener = talker == a ? b : a;
        talker.steal(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (var entry : trails.entrySet()) {
            for (var t : entry.getValue()) {
                float alpha = t.alpha();
                g2.setColor(new Color(t.color().getRed(), t.color().getGreen(), t.color().getBlue(), (int) (255 * alpha)));
                g2.fillOval((int) t.pos().x() - 3, (int) t.pos().y() - 3, 6, 6);
            }
        }
        for (var agent : agents) {
            g2.setColor(agent.healthColor());
            g2.fillOval((int) agent.pos.x() - 10, (int) agent.pos.y() - 10, 20, 20);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Agents");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Main panel = new Main();
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
        });
    }

    // supporting classes
    static record Point2D(double x, double y) {
        Point2D add(Point2D other) { return new Point2D(x + other.x, y + other.y); }
        Point2D mul(double s) { return new Point2D(x * s, y * s); }
        double distance(Point2D o) { return Math.hypot(x - o.x, y - o.y); }
    }

    sealed interface AgentEvent permits AgentDead { }
    static final class AgentDead implements AgentEvent {
        final Agent killer;
        final Agent dead;
        AgentDead(Agent killer, Agent dead) { this.killer = killer; this.dead = dead; }
    }

    static final class Trail {
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

    final class Agent {
        Point2D pos;
        Point2D vel;
        double speed = 2;
        double heading;
        double hp = 100;
        final List<Boost> boosts = new ArrayList<>();
        Agent(Point2D p) {
            pos = p;
            heading = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            vel = new Point2D(Math.cos(heading), Math.sin(heading));
        }
        void update() {
            boosts.removeIf(Boost::expired);
            double boostFactor = boosts.stream().mapToDouble(b -> b.factor).reduce(1.0, (a, b) -> a * b);
            double dx = vel.x * speed * boostFactor;
            double dy = vel.y * speed * boostFactor;
            pos = new Point2D((pos.x + dx + WIDTH) % WIDTH, (pos.y + dy + HEIGHT) % HEIGHT);
            trails.computeIfAbsent(this, k -> new ArrayList<>()).add(new Trail(pos, speedColor()));
        }
        double facingScore(Agent other) {
            double angleToOther = Math.atan2(other.pos.y - pos.y, other.pos.x - pos.x);
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
                handleEvent(dead);
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

    static final class Boost {
        final double factor;
        final long end;
        Boost(double factor, long durationMs) {
            this.factor = factor;
            this.end = System.currentTimeMillis() + durationMs;
        }
        boolean expired() { return System.currentTimeMillis() > end; }
    }

    void handleEvent(AgentDead event) {
        agents.remove(event.dead);
        trails.remove(event.dead);
        exec.schedule(() -> {
            Agent a = new Agent(event.dead.pos);
            agents.add(a);
        }, 5, TimeUnit.SECONDS);
    }
}
