package com.MiniGame.TankArena.game;

import java.util.List;
import java.util.Random;

public class AITankController {

    private static final Random random = new Random();

    public static void update(Player ai,
                              List<Player> targets,
                              GameEngine engine) {

        if (!ai.isAlive()) return;

        // 1️⃣ Find nearest enemy
        Player target = findNearestTarget(ai, targets);
        if (target == null) return;

        // 2️⃣ Rotate toward target
        double dx = target.getX() - ai.getX();
        double dy = target.getY() - ai.getY();

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        ai.setAngle(angle);

        // 3️⃣ Move toward target
        ai.setMovingUp(false);
        ai.setMovingDown(false);
        ai.setMovingLeft(false);
        ai.setMovingRight(false);

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) ai.setMovingRight(true);
            else ai.setMovingLeft(true);
        } else {
            if (dy > 0) ai.setMovingDown(true);
            else ai.setMovingUp(true);
        }

        // 4️⃣ Shoot randomly (not spam)
        if (random.nextDouble() < 0.02) {
            engine.handleShoot(ai.getId());
        }
    }

    private static Player findNearestTarget(Player ai, List<Player> players) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : players) {
            if (!p.isAlive() || p.getId().equals(ai.getId())) continue;

            double dx = p.getX() - ai.getX();
            double dy = p.getY() - ai.getY();
            double dist = dx * dx + dy * dy;

            if (dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }
}
