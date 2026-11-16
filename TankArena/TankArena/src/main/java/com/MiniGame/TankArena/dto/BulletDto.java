package com.MiniGame.TankArena.dto;
import lombok.Data;

@Data
public class BulletDto {
    private String id;
    private double x;
    private double y;
    private double vx;
    private double vy;
    private String ownerId;
}
