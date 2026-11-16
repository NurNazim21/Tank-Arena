package com.MiniGame.TankArena.config;
import com.MiniGame.TankArena.game.RoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component //for multiplayer
@EnableScheduling
@RequiredArgsConstructor
public class GameLoopScheduler {

    private final RoomManager roomManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 16) // Changed from 40ms to 16ms for 60 FPS (1000ms / 60fps ≈ 16ms)
    public void gameLoop() {
        roomManager.getAllRooms().forEach((roomId, engine) -> {
            engine.update();
            messagingTemplate.convertAndSend("/topic/gameState/" + roomId, engine.getCurrentState());
        });
    }
}
