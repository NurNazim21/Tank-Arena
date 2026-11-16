package com.MiniGame.TankArena.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/hello")
    public String hello() {
        return "Tank Arena server is running!";
    }
}
