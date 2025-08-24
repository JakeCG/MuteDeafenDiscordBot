package com.jakec.mutediscordbot.config;

import com.jakec.mutediscordbot.handler.DiscordEventHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JdaConfiguration {
	
	private final BotProperties botProperties;
	private volatile JDA jda;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	@Bean
	@Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
	public JDA jda(DiscordEventHandler eventHandler) {
		try {
			log.info("Starting Discord bot connection...");
			
			if (botProperties.getToken() == null || botProperties.getToken().trim().isEmpty()) {
				throw new IllegalArgumentException("Discord bot token is required but not provided");
			}
			
			this.jda = JDABuilder.createDefault(botProperties.getToken())
							.addEventListeners(eventHandler)
							.addEventListeners(new BotLifecycleListener())
							.enableIntents(
									GatewayIntent.GUILD_VOICE_STATES,
									GatewayIntent.MESSAGE_CONTENT,
									GatewayIntent.GUILD_MESSAGES
							)
							.setMemberCachePolicy(MemberCachePolicy.VOICE)
							.setStatus(OnlineStatus.ONLINE)
							.setActivity(Activity.listening("for mute/deafen changes"))
							.setAutoReconnect(true)
							.build();
			
			
			scheduleConnectionHealthCheck();
			
			log.info("Discord bot connection initiated (non-blocking)");
			return jda;
			
		} catch (Exception e) {
			log.error("Failed to initialize Discord bot: {}", e.getMessage(), e);
			throw new RuntimeException("Discord bot initialization failed", e);
		}
	}
	
	private static class BotLifecycleListener extends ListenerAdapter {
		
		@Override
		public void onReady(@NotNull ReadyEvent event) {
			log.info("Discord bot ready! Connected as: {}", event.getJDA().getSelfUser().getName());
			log.info("Connected to {} guilds", event.getJDA().getGuilds().size());
			log.info("Bot is now monitoring voice state changes");
			
			
			long ping = event.getJDA().getGatewayPing();
			if (ping > 0) {
				log.info("Gateway ping: {}ms", ping);
			}
		}
	}
	
	private void scheduleConnectionHealthCheck() {
		scheduler.schedule(() -> {
			if (jda != null) {
				JDA.Status status = jda.getStatus();
				switch (status) {
					case CONNECTED -> log.info("Discord bot health check: Connected successfully");
					case CONNECTING_TO_WEBSOCKET, IDENTIFYING_SESSION, LOADING_SUBSYSTEMS ->
							log.info("Discord bot health check: Still connecting ({})", status);
					case DISCONNECTED, FAILED_TO_LOGIN, SHUTDOWN -> {
						log.error("Discord bot health check: Connection failed ({})", status);
						attemptReconnection();
					}
					default -> log.warn("⚠Discord bot health check: Unknown status ({})", status);
				}
			}
		}, 30, TimeUnit.SECONDS);
	}
	
	private void attemptReconnection() {
		if (jda != null && jda.getStatus() == JDA.Status.FAILED_TO_LOGIN) {
			log.info("Attempting Discord bot reconnection...");
			try {
				jda.shutdown();
				Thread.sleep(5000);
				
				log.warn("Bot reconnection requires restart - please check logs and restart application");
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("Reconnection attempt interrupted", e);
			}
		}
	}
	
	@PreDestroy
	public void shutdown() {
		log.info("Shutting down Discord bot...");
		
		try {
			if (!scheduler.isShutdown()) {
				scheduler.shutdown();
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			}
			
			if (jda != null) {
				jda.shutdown();
				if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
					log.warn("⚠Discord bot shutdown timed out, forcing shutdown");
					jda.shutdownNow();
				}
				log.info("Discord bot shutdown complete");
			}
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Shutdown interrupted, forcing immediate shutdown");
			if (jda != null) {
				jda.shutdownNow();
			}
		} catch (Exception e) {
			log.error("Error during Discord bot shutdown: {}", e.getMessage(), e);
			if (jda != null) {
				jda.shutdownNow();
			}
		}
	}
	
	public boolean isBotReady() {
		return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
	}
	
	public String getBotStatus() {
		if (jda == null) {
			return "NOT_INITIALIZED";
		}
		return jda.getStatus().toString();
	}
}