package com.MiniGame.TankArena.game;
import lombok.Data;
import java.util.Random;
import org.springframework.web.socket.WebSocketSession;

@Data
public class Player {
    private String id;
    private String name = "Player";


    private int health = 100; // Starting health
    private static final int MAX_HEALTH = 100;
    private static final int DAMAGE_PER_HIT = 20; // 5 hits = 100 damage


    private double x = 100 + new Random().nextDouble() * 600;
    private double y = 100 + new Random().nextDouble() * 400;
    private double angle = 0;
    private int score = 0;
    private boolean alive = true;
    private long respawnTime = 0;  // NEW: time to respawn
    private boolean ai = false;

    //PowerUps
    private double speedMultiplier = 1.0;
    private long speedBoostEndTime = 0;

    // Movement state
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;

    private static final Random random = new Random();

    // NEW: Kill and schedule respawn
    // Inside com.MiniGame.TankArena.game.Player.java

    public void kill() {
        this.alive = false;
        this.respawnTime = System.currentTimeMillis() + 3000;

        // BUG FIX: Reset movement flags on death
        this.movingUp = false;
        this.movingDown = false;
        this.movingLeft = false;
        this.movingRight = false;
    }

    public boolean isAI() {
        return ai;
    }

    public void takeDamage() {
        this.health -= DAMAGE_PER_HIT;
        if (this.health <= 0) {
            this.health = 0;
            this.kill(); // Use your existing kill logic
        }
    }


    // NEW: Update respawn in game loop
    public void updateRespawn(GameEngine gameEngine) {
        if (!alive && System.currentTimeMillis() >= respawnTime) {

            for (int attempts = 0; attempts < 100; attempts++) {
                this.x = 100 + random.nextDouble() * (GameEngine.MAP_WIDTH - 200);
                this.y = 100 + random.nextDouble() * (GameEngine.MAP_HEIGHT - 200);
                this.angle = random.nextDouble() * 360;

                if (!gameEngine.collidesWithAnyObstacle(this.x, this.y, 25, false)) {
                    break;
                }
            }

            this.alive = true;
            this.respawnTime = 0;
        }
    }
    public void applySpeedBoost(long duration) {
        this.speedMultiplier = 1.5;
        this.speedBoostEndTime = System.currentTimeMillis() + duration;
    }

    public void updateSpeedBoost() {
        if (speedBoostEndTime > 0 && System.currentTimeMillis() >= speedBoostEndTime) {
            this.speedMultiplier = 1.0;
            this.speedBoostEndTime = 0;
        }
    }

    public void heal(int amount) {
        this.health = Math.min(MAX_HEALTH, this.health + amount);
    }

}

