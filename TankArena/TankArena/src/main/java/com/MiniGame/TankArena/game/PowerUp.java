package com.MiniGame.TankArena.game;

import lombok.Data;


@Data
public class PowerUp {
    private String id;
    private String type; // "HEALTH" or "SPEED"
    private double x;
    private double y;
    private long spawnTime;
    private boolean active = true;
}
