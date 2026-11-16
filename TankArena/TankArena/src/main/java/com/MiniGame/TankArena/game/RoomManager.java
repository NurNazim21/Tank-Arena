package com.MiniGame.TankArena.game;

import com.MiniGame.TankArena.controller.GameController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RoomManager {
    // Maps RoomId -> GameEngine
    private final Map<String, GameEngine> rooms = new ConcurrentHashMap<>();

    public GameEngine getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> new GameEngine());
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

    public Map<String, GameEngine> getAllRooms() {
        return rooms;
    }
}