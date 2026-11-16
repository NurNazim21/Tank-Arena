package com.MiniGame.TankArena.controller;

import com.MiniGame.TankArena.dto.*;
import com.MiniGame.TankArena.game.GameEngine;
import com.MiniGame.TankArena.game.Player;
import com.MiniGame.TankArena.game.RoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.MiniGame.TankArena.dto.ChatMessageDto;





import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final RoomManager roomManager;
    private final SimpMessagingTemplate messagingTemplate;






    @MessageMapping("/join")
    public void join(@Payload JoinInput input, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) return;

        String mode = (input != null && input.getMode() != null) ? input.getMode() : "MULTI";
        String name = (input != null && input.getPlayerName() != null) ? input.getPlayerName() : "Player";
        String roomId = "SINGLE".equalsIgnoreCase(mode) ? sessionId : "LOBBY";
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        GameEngine engine = roomManager.getOrCreateRoom(roomId);

        // FIX: Only set theme if the engine is new/empty (First Player)
        if (engine.getPlayers().isEmpty() && input.getTheme() != null) {
            engine.setTheme(input.getTheme());
        }

        Player player = new Player();
        player.setId(sessionId);
        player.setName(name);
        engine.addPlayer(sessionId, player, mode);


        // ⭐ ADD THIS: Broadcast player joined event for voice chat
        Map<String, Object> joinEvent = new HashMap<>();
        joinEvent.put("playerId", sessionId);
        joinEvent.put("playerName", name);
        joinEvent.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/playerJoined/" + roomId,(Object)joinEvent);

    }


    @MessageMapping("/move")
    public void move(@Payload MoveInput input, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        if (roomId == null) return;

        GameEngine engine = roomManager.getOrCreateRoom(roomId);
        engine.handleMove(headerAccessor.getSessionId(), input.getDirection(), true);
    }

    @MessageMapping("/shoot")
    public void shoot(SimpMessageHeaderAccessor headerAccessor) {
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        if (roomId == null) return;

        GameEngine engine = roomManager.getOrCreateRoom(roomId);
        engine.handleShoot(headerAccessor.getSessionId());
    }

    @MessageMapping("/reset")
    public void resetGame(SimpMessageHeaderAccessor headerAccessor) {
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        if (roomId == null) return;

        GameEngine engine = roomManager.getOrCreateRoom(roomId);
        engine.resetGame();
        //sendGameState(roomId, engine);
    }

    @MessageMapping("/voiceSignal")
    public void handleVoiceSignal(@Payload VoiceSignalDto signal,
                                  SimpMessageHeaderAccessor headerAccessor) {
        String senderId = headerAccessor.getSessionId();
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (roomId == null || signal.getTargetId() == null) return;

        // Create response with sender info
        Map<String, Object> response = new HashMap<>();
        response.put("senderId", senderId);
        response.put("signal", signal.getSignal());

        // Send directly to the target user
        messagingTemplate.convertAndSendToUser(
                signal.getTargetId(),
                "/queue/voiceSignal",
                response
        );
    }

    // ========== EMOJI SYSTEM ==========

    @MessageMapping("/emoji")
    public void handleEmoji(@Payload EmojiDto emoji,
                            SimpMessageHeaderAccessor headerAccessor) {
        String playerId = headerAccessor.getSessionId();
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (roomId == null) return;

        GameEngine engine = roomManager.getOrCreateRoom(roomId);
        Player player = engine.getPlayers().get(playerId);

        if (player == null) return;

        Map<String, Object> emojiData = new HashMap<>();
        emojiData.put("playerId", playerId);
        emojiData.put("playerName", player.getName());
        emojiData.put("emoji", emoji.getEmoji());
        emojiData.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/emoji/" + roomId,(Object) emojiData);
    }

    // ========== CHAT SYSTEM ==========

    @MessageMapping("/chat")
    public void handleChat(@Payload ChatMessageDto message,
                           SimpMessageHeaderAccessor headerAccessor) {
        String playerId = headerAccessor.getSessionId();
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (roomId == null) return;

        GameEngine engine = roomManager.getOrCreateRoom(roomId);
        Player player = engine.getPlayers().get(playerId);

        if (player == null) return;

        // Validate message
        String chatText = message.getMessage();
        if (chatText == null || chatText.trim().isEmpty() || chatText.length() > 100) {
            return;
        }

        // Broadcast message to all players in room
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("playerId", playerId);
        chatData.put("playerName", player.getName());
        chatData.put("message", chatText.trim());
        chatData.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/chat/" + roomId,(Object) chatData);
    }
}






