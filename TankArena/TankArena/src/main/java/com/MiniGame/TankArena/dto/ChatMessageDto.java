package com.MiniGame.TankArena.dto;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String message;
    public void setMessage(String message) {
        this.message = message;
    }
}
