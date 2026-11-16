package com.MiniGame.TankArena.game;
import lombok.Data;

@Data
public class Bullet {
    private String id;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private String ownerId;
    private long spawnTime;
}
