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

    final List<Agent> agents = new CopyOnWriteArrayList<>();
    final Map<Agent, List<Trail>> trails = new ConcurrentHashMap<>();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        for (int i = 0; i < NUM_AGENTS; i++) {
            agents.add(new Agent(this, new Point2D(ThreadLocalRandom.current().nextDouble(WIDTH),
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



    void handleEvent(AgentDead event) {
        agents.remove(event.dead);
        trails.remove(event.dead);
        exec.schedule(() -> {
            Agent a = new Agent(this, event.dead.pos);
            agents.add(a);
        }, 5, TimeUnit.SECONDS);
    }
}
