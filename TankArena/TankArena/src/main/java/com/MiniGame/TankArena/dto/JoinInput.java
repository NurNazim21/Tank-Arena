package com.MiniGame.TankArena.dto;
import lombok.Data;

@Data
public class JoinInput {
    private String playerName = "Player";
    private String mode; // "SINGLE" or "MULTI"
    private String theme;
}