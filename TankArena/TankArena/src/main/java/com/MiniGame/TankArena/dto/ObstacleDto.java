package com.MiniGame.TankArena.dto;

import lombok.Data;

@Data
public class ObstacleDto {
    private String type;
    private double x;
    private double y;
    private double width;
    private double height;
}
