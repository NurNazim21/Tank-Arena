package com.MiniGame.TankArena.dto;
import lombok.Data;
import java.util.List;
import java.util.Map;


@Data
public class GameStateDto {
    private List<WallDto> walls;
    private String phase;  // "playing" or "finished"
    private String winner;
    private long remainingTime;
    private List<TankDto> tanks;
    private List<BulletDto> bullets;
    private List<PowerUpDto> powerUps;
    private List<ObstacleDto> obstacles;
    private Map<String, Integer> scoreboard;  // playerId → score
    private long serverTime;  // for lag compensation later if needed
    private String theme;
}
