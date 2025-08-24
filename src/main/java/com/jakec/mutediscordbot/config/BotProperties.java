package com.jakec.mutediscordbot.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "discord.bot")
@Slf4j
public class BotProperties {
	
	@NotBlank(message = "Bot token is required")
	private String token;
	
	@NotBlank(message = "Announcement channel name is required")
	private String announcementChannel = "general";
	
	@NotNull @Valid
	private Announcements announcements = new Announcements();
	
	@NotNull @Valid
	private SpamPrevention spamPrevention = new SpamPrevention();
	
	@NotNull @Valid
	private Messages messages = new Messages();
	
	@Data
	public static class Announcements {
		private boolean mute = true;
		private boolean deafen = true;
		private boolean includeBots = false;
		private boolean useNicknames = true;
	}
	
	@Data
	public static class SpamPrevention {
		private Duration cooldown = Duration.ofSeconds(3);
		
		@Min(1)
		private int maxAnnouncementsPerMinute = 20;
		
		private boolean enableRateLimit = true;
		
		@PostConstruct
		public void validate() {
			long millis = cooldown.toMillis();
			if (millis < 100 || millis > 30_000) {
				throw new IllegalArgumentException("Cooldown must be between 100ms and 30 seconds");
			}
		}
	}
	
	@Data
	public static class Messages {
		@NotEmpty
		private List<String> muteTemplates = List.of(
				"🤫 **{user}** has gone silent!",
				"🎤❌ **{user}** dropped the mic!",
				"🔇 **{user}** is now in stealth mode!"
		);
		
		@NotEmpty
		private List<String> unmuteTemplates = List.of(
				"🎤 **{user}** is back on the mic!",
				"🔊 **{user}** has returned to the conversation!",
				"💬 **{user}** is ready to speak again!"
		);
		
		@NotEmpty
		private List<String> deafenTemplates = List.of(
				"👂❌ **{user}** has entered their own world!",
				"🔇 **{user}** is now in the zone!",
				"🎧 **{user}** tuned out the world!"
		);
		
		@NotEmpty
		private List<String> undeafenTemplates = List.of(
				"👂 **{user}** is back among the living!",
				"🔊 **{user}** rejoined reality!",
				"🎧❌ **{user}** plugged into the matrix!"
		);
		
		private Map<String, List<String>> customUserMessages = Map.of();
	}
}
