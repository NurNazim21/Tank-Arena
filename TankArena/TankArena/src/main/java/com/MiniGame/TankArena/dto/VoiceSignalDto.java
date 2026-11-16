package com.MiniGame.TankArena.dto;

import lombok.Data;

/**
 * Voice Signal DTO for WebRTC signaling
 */
@Data
public class VoiceSignalDto {
    private String targetId;    // Recipient player ID
    private Object signal;      // WebRTC signal (offer/answer/ice-candidate)
}
