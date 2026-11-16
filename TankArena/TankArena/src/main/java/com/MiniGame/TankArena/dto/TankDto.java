package com.MiniGame.TankArena.dto;
import lombok.Data;

@Data
public class TankDto {
    private String id;
    private double x;
    private double y;
    private double angle;  // in degrees, for turret direction
    private String color;
    private String playerName;
    private int health;
    private int score = 0;
    private boolean alive = true;
}
