package com.MiniGame.TankArena.game;
import com.MiniGame.TankArena.dto.*;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;




import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;



@Component
@Getter
public class GameEngine {
    private String theme = "desert";
    private static final double TANK_SPEED = 4;
    private static final double BULLET_SPEED = 10.0;
    static final int MAP_WIDTH = 5376;
    static final int MAP_HEIGHT = 2944;

    private static final long ROUND_DURATION_MS = 300_000L; // 10 minutes (CHANGED)
    private static final double TANK_RADIUS = 40;
    private static final double BULLET_RADIUS = 10.0;
    private static final double POWERUP_RADIUS = 30.0;

    // Power-up spawning
    private static final long POWERUP_SPAWN_INTERVAL = 10_000L; // Every 15 seconds
    private static final long POWERUP_LIFETIME = 20_000L; // Disappears after 20 seconds
    private static final int MAX_POWERUPS = 5;

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = Collections.synchronizedList(new ArrayList<>());
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<PowerUp> powerUps = Collections.synchronizedList(new ArrayList<>());

    private final Random random = new Random();
    private long roundStartTime = System.currentTimeMillis();
    private long lastPowerUpSpawn = System.currentTimeMillis();
    private String winner = null;

    public GameEngine() {
        // Constructor remains the same
    }

    public void addPlayer(String playerId, Player player, String mode) {
        players.put(playerId, player);

        if ("SINGLE".equalsIgnoreCase(mode)) {
            spawnAI(3);
        }

        if (players.size() == 1) {
            roundStartTime = System.currentTimeMillis();
            winner = null;
        }
    }

    public void setTheme(String theme) {
        this.theme = theme;

    }

    public void removePlayer(String playerId) {
        Player p = players.get(playerId);
        if (p != null) {
            p.setMovingUp(false);
            p.setMovingDown(false);
            p.setMovingLeft(false);
            p.setMovingRight(false);
        }
        players.remove(playerId);
    }

    public void handleMove(String playerId, String direction, boolean pressed) {
        Player player = players.get(playerId);
        if (player == null || !player.isAlive()) return;

        if (direction.startsWith("STOP_")) {
            direction = direction.substring(5);
            pressed = false;
        }

        switch (direction.toUpperCase()) {
            case "UP" -> player.setMovingUp(pressed);
            case "DOWN" -> player.setMovingDown(pressed);
            case "LEFT" -> player.setMovingLeft(pressed);
            case "RIGHT" -> player.setMovingRight(pressed);
        }
    }

    public void spawnAI(int count) {
        for (int i = 0; i < count; i++) {
            Player ai = new Player();
            ai.setId("AI_" + UUID.randomUUID());
            ai.setName("BOT_" + (i + 1));
            ai.setAi(true);
            ai.setX(200 + random.nextDouble() * (MAP_WIDTH - 400));
            ai.setY(200 + random.nextDouble() * (MAP_HEIGHT - 400));
            players.put(ai.getId(), ai);
        }
    }

    public void handleShoot(String playerId) {
        Player player = players.get(playerId);
        if (player == null || !player.isAlive()) return;

        double angleRad = Math.toRadians(player.getAngle());
        double vx = Math.cos(angleRad) * BULLET_SPEED;
        double vy = Math.sin(angleRad) * BULLET_SPEED;

        Bullet bullet = new Bullet();
        bullet.setId(UUID.randomUUID().toString());
        bullet.setX(player.getX() + Math.cos(angleRad) * 25);
        bullet.setY(player.getY() + Math.sin(angleRad) * 25);
        bullet.setVx(vx);
        bullet.setVy(vy);
        bullet.setOwnerId(playerId);
        bullet.setSpawnTime(System.currentTimeMillis());

        bullets.add(bullet);
    }

    public void update() {
        if (players.isEmpty()) return;

        // AI logic
        players.values().forEach(p -> {
            if (p.isAI()) {
                AITankController.update(p, new ArrayList<>(players.values()), this);
            }
        });

        updatePlayers();
        updateBullets();
        checkCollisions();
        updatePowerUps();
        spawnPowerUps();
        checkPowerUpCollisions();
        players.values().forEach(p -> {
            p.updateRespawn(this);
            p.updateSpeedBoost();
        });
        checkRoundEnd();
    }

    public synchronized void resetGame() {
        players.clear();
        bullets.clear();
        powerUps.clear();
        winner = null;
        roundStartTime = System.currentTimeMillis();
        lastPowerUpSpawn = System.currentTimeMillis();
    }

    private void updatePlayers() {
        for (Player p : players.values()) {
            if (!p.isAlive()) continue;

            double oldX = p.getX();
            double oldY = p.getY();
            double nextX = oldX;
            double nextY = oldY;

            // Apply speed with multiplier
            double speed = TANK_SPEED * p.getSpeedMultiplier();

            if (p.isMovingUp()) nextY -= speed;
            if (p.isMovingDown()) nextY += speed;
            if (p.isMovingLeft()) nextX -= speed;
            if (p.isMovingRight()) nextX += speed;

            // Sliding physics
            double tryX = Math.max(TANK_RADIUS, Math.min(MAP_WIDTH - TANK_RADIUS, nextX));
            if (!collidesWithAnyObstacle(tryX, oldY, TANK_RADIUS, false) &&
                    !collidesWithOtherPlayers(p.getId(), tryX, oldY)) {
                p.setX(tryX);
            }

            double tryY = Math.max(TANK_RADIUS, Math.min(MAP_HEIGHT - TANK_RADIUS, nextY));
            if (!collidesWithAnyObstacle(p.getX(), tryY, TANK_RADIUS, false) &&
                    !collidesWithOtherPlayers(p.getId(), p.getX(), tryY)) {
                p.setY(tryY);
            }

            // Set angle
            if (p.isMovingLeft()) p.setAngle(180);
            else if (p.isMovingRight()) p.setAngle(0);
            else if (p.isMovingUp()) p.setAngle(270);
            else if (p.isMovingDown()) p.setAngle(90);
        }
    }

    private void updateBullets() {
        long currentTime = System.currentTimeMillis();

        bullets.removeIf(b -> {
            b.setX(b.getX() + b.getVx());
            b.setY(b.getY() + b.getVy());

            if (currentTime - b.getSpawnTime() > 2000) return true;

            boolean outOfBounds = b.getX() < 0 || b.getX() > MAP_WIDTH ||
                    b.getY() < 0 || b.getY() > MAP_HEIGHT;
            boolean hitObstacle = collidesWithAnyObstacle(b.getX(), b.getY(), BULLET_RADIUS, true);

            return outOfBounds || hitObstacle;
        });
    }

    // NEW: Power-up spawning
    private void spawnPowerUps() {
        long now = System.currentTimeMillis();
        if (now - lastPowerUpSpawn < POWERUP_SPAWN_INTERVAL) return;
        if (powerUps.size() >= MAX_POWERUPS) return;

        PowerUp powerUp = new PowerUp();
        powerUp.setId(UUID.randomUUID().toString());
        powerUp.setType(random.nextBoolean() ? "HEALTH" : "SPEED");
        powerUp.setX(200 + random.nextDouble() * (MAP_WIDTH - 400));
        powerUp.setY(200 + random.nextDouble() * (MAP_HEIGHT - 400));
        powerUp.setSpawnTime(now);

        powerUps.add(powerUp);
        lastPowerUpSpawn = now;
    }

    // NEW: Update power-ups (remove expired)
    private void updatePowerUps() {
        long now = System.currentTimeMillis();
        powerUps.removeIf(p -> !p.isActive() || (now - p.getSpawnTime() > POWERUP_LIFETIME));
    }

    // NEW: Check power-up collisions
    private void checkPowerUpCollisions() {
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            if (!powerUp.isActive()) continue;

            for (Player player : players.values()) {
                if (!player.isAlive()) continue;

                double dx = player.getX() - powerUp.getX();
                double dy = player.getY() - powerUp.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < TANK_RADIUS + POWERUP_RADIUS) {
                    // Apply power-up effect
                    if ("HEALTH".equals(powerUp.getType())) {
                        player.heal(30); // Heal 30 HP
                    } else if ("SPEED".equals(powerUp.getType())) {
                        player.applySpeedBoost(10000); // 10 seconds speed boost
                    }

                    powerUp.setActive(false);
                    break;
                }
            }
        }

        powerUps.removeIf(p -> !p.isActive());
    }

    public boolean collidesWithAnyObstacle(double x, double y, double radius, boolean isBullet) {
        for (Obstacle o : obstacles) {
            double left = o.getX() - o.getWidth() / 2;
            double right = o.getX() + o.getWidth() / 2;
            double top = o.getY() - o.getHeight() / 2;
            double bottom = o.getY() + o.getHeight() / 2;

            if (x + radius > left && x - radius < right &&
                    y + radius > top && y - radius < bottom) {
                return true;
            }
        }
        return false;
    }

    private void checkCollisions() {
        List<Bullet> toRemove = new ArrayList<>();

        for (Bullet bullet : bullets) {
            for (Player target : players.values()) {
                if (!target.isAlive() || target.getId().equals(bullet.getOwnerId())) continue;

                double dx = target.getX() - bullet.getX();
                double dy = target.getY() - bullet.getY();
                if (Math.sqrt(dx * dx + dy * dy) < TANK_RADIUS + BULLET_RADIUS) {
                    target.takeDamage(); // Use damage system instead of instant kill

                    Player owner = players.get(bullet.getOwnerId());
                    if (owner != null && !target.isAlive()) {
                        owner.setScore(owner.getScore() + 1);
                    }

                    toRemove.add(bullet);
                    break;
                }
            }
        }
        bullets.removeAll(toRemove);
    }

    public boolean collidesWithOtherPlayers(String selfId, double x, double y) {
        for (Player other : players.values()) {
            if (other.getId().equals(selfId) || !other.isAlive()) continue;

            double dx = x - other.getX();
            double dy = y - other.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < (TANK_RADIUS * 2)) {
                return true;
            }
        }
        return false;
    }

    private void checkRoundEnd() {
        long elapsed = System.currentTimeMillis() - roundStartTime;
        if (elapsed >= ROUND_DURATION_MS && winner == null) {
            winner = players.values().stream()
                    .max(Comparator.comparingInt(Player::getScore))
                    .map(Player::getName)
                    .orElse("Nobody");
        }
    }

    public GameStateDto getCurrentState() {
        GameStateDto state = new GameStateDto();

        long elapsed = System.currentTimeMillis() - roundStartTime;
        int remaining = winner == null ? (int) ((ROUND_DURATION_MS - elapsed) / 1000) : 0;
        state.setRemainingTime(Math.max(0, remaining));
        state.setWinner(winner);

        state.setTanks(players.values().stream().map(p -> {
            TankDto dto = new TankDto();
            dto.setId(p.getId());
            dto.setX(p.getX());
            dto.setY(p.getY());
            dto.setAngle(p.getAngle());
            dto.setPlayerName(p.getName());
            dto.setScore(p.getScore());
            dto.setAlive(p.isAlive());
            dto.setHealth(p.getHealth()); // Include health
            dto.setColor(p.isAI() ? "#FF4444" : getColorForPlayer(p));
            return dto;
        }).collect(Collectors.toList()));

        state.setBullets(bullets.stream().map(b -> {
            BulletDto dto = new BulletDto();
            dto.setId(b.getId());
            dto.setX(b.getX());
            dto.setY(b.getY());
            dto.setVx(b.getVx());
            dto.setVy(b.getVy());
            dto.setOwnerId(b.getOwnerId());
            return dto;
        }).collect(Collectors.toList()));

        state.setObstacles(obstacles.stream().map(o -> {
            ObstacleDto dto = new ObstacleDto();
            dto.setType(o.getType());
            dto.setX(o.getX());
            dto.setY(o.getY());
            dto.setWidth(o.getWidth());
            dto.setHeight(o.getHeight());
            return dto;
        }).collect(Collectors.toList()));

        // NEW: Add power-ups to state
        state.setPowerUps(powerUps.stream()
                .filter(PowerUp::isActive)
                .map(p -> {
                    PowerUpDto dto = new PowerUpDto();
                    dto.setId(p.getId());
                    dto.setType(p.getType());
                    dto.setX(p.getX());
                    dto.setY(p.getY());
                    return dto;
                }).collect(Collectors.toList()));

        Map<String, Integer> scoreboard = new LinkedHashMap<>();
        players.values().stream()
                .sorted((a, b) -> b.getScore() - a.getScore())
                .forEach(p -> scoreboard.put(p.getName(), p.getScore()));
        state.setScoreboard(scoreboard);

        state.setServerTime(System.currentTimeMillis());
        state.setTheme(this.theme); // Make sure this is sent to frontend

        return state;
    }

    private String getColorForPlayer(Player p) {
        if (p.isAI()) return "#FF3333";
        int hash = p.getId().hashCode();
        return String.format("#%06X", (hash & 0xFFFFFF));
    }
}
