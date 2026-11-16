package com.MiniGame.TankArena.game;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Obstacle {
    private String type;     // "tree" or "stone"
    private double x;
    private double y;
    private double width;
    private double height;
    private boolean blocksBullets;
}
