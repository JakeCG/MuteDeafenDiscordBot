package com.jakec.mutediscordbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
@ComponentScan(basePackages = "com.jakec.mutediscordbot")
@EnableAsync
@EnableScheduling
@Slf4j
public class BotConfiguration {
	
	public BotConfiguration() {
		log.info("Bot configuration initialised.");
	}
}
