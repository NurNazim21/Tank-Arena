package com.MiniGame.TankArena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class TankArenaApplication {

	public static void main(String[] args) {
		SpringApplication.run(TankArenaApplication.class, args);
	}

}
