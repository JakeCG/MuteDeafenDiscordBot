package com.jakec.mutediscordbot;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Objects;

@Slf4j
@EnableRetry
@SpringBootApplication
public class MuteDeafenBotApplication {
	
	public static void main(String[] args) {
		log.info("Starting Mute/Deafen Discord Bot...");
		
		if (System.getenv("DISCORD_TOKEN") == null) {
			Dotenv dotenv = Dotenv.load();
			System.setProperty("DISCORD_TOKEN", Objects.requireNonNull(dotenv.get("DISCORD_TOKEN")));
		}
		
		System.setProperty("jda.voice.pool.size", "2");
		System.setProperty("jda.ws.pool.size", "4");
		
		SpringApplication app = new SpringApplication(MuteDeafenBotApplication.class);
		app.setLogStartupInfo(true);
		app.run(args);
		
		log.info("Bot application started successfully!");
	}
}

