package com.MiniGame.TankArena.config;

import com.MiniGame.TankArena.controller.GameController;
import com.MiniGame.TankArena.game.GameEngine;
import com.MiniGame.TankArena.game.RoomManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RoomManager roomManager;


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        if (sessionId != null && roomId != null) {
            log.info("Player {} disconnected from room {}", sessionId, roomId);

            GameEngine engine = roomManager.getOrCreateRoom(roomId);
            engine.removePlayer(sessionId);

            // If it was a single player room, delete the room entirely
            if (roomId.equals(sessionId)) {
                roomManager.removeRoom(roomId);
            } else {
                // Otherwise just update other players in the lobby
               // gameController.sendGameState(roomId, engine);
            }
        }
    }
}