package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.config.BotProperties;
import com.jakec.mutediscordbot.metrics.BotMetrics;
import com.jakec.mutediscordbot.model.AnnouncementResult;
import com.jakec.mutediscordbot.model.VoiceAction;
import com.jakec.mutediscordbot.model.VoiceStateChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {
	
	private final MessageTemplateService messageService;
	private final ChannelService channelService;
	private final BotProperties botProperties;
	private final BotMetrics botMetrics;
	
	@Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
	public AnnouncementResult processAnnouncement(VoiceStateChange stateChange, Guild guild) {
		if (!shouldAnnounceAction(stateChange.action())) {
			log.debug("Action {} disabled in configuration", stateChange.action());
			return AnnouncementResult.failure("Action disabled in configuration");
		}
		
		if (stateChange.isBot() && !botProperties.getAnnouncements().isIncludeBots()) {
			log.debug("Bot action ignored for user {}", stateChange.getUserId());
			return AnnouncementResult.failure("Bot actions excluded");
		}
		
		Optional<String> messageOpt = messageService.generateMessage(stateChange);
		if (messageOpt.isEmpty()) {
			log.warn("No message template found for action: {}", stateChange.action());
			botMetrics.incrementFailedAnnouncements();
			return AnnouncementResult.failure("No message template available");
		}
		
		String message = messageOpt.get();
		
		return channelService.findAnnouncementChannel(guild)
								.map(channel -> sendMessage(channel, message))
								.orElseGet(() -> {
									log.error("No available channels in guild: {}", guild.getName());
									botMetrics.incrementFailedAnnouncements();
									return AnnouncementResult.failure("No available channels");
								});
	}
	
	private AnnouncementResult sendMessage(TextChannel channel, String message) {
		try {
			channel.sendMessage(message).queue(
					success -> {
						log.info("Message sent to #{}: {}", channel.getName(), message);
						botMetrics.incrementSuccessfulAnnouncements();
					},
					error -> {
						log.error("Discord API error for #{}: {}", channel.getName(), error.getMessage());
						botMetrics.incrementFailedAnnouncements();
					}
			);
			
			return AnnouncementResult.success(message, channel.getName());
			
		} catch (Exception e) {
			log.error("Exception queuing message to #{}: {}", channel.getName(), e.getMessage(), e);
			botMetrics.incrementFailedAnnouncements();
			return AnnouncementResult.failure("Send error: " + e.getMessage());
		}
	}
	
	private boolean shouldAnnounceAction(VoiceAction action) {
		return switch (action) {
			case MUTED, UNMUTED -> botProperties.getAnnouncements().isMute();
			case DEAFENED, UNDEAFENED -> botProperties.getAnnouncements().isDeafen();
		};
	}
	
	public AnnouncementResult sendTestAnnouncement(Guild guild, String testMessage) {
		Optional<TextChannel> channelOpt = channelService.findAnnouncementChannel(guild);
		
		if (channelOpt.isEmpty()) {
			log.warn("No channel available for test message in guild: {}", guild.getName());
			return AnnouncementResult.failure("No channel available for test");
		}
		
		String formattedMessage = "**Test Announcement:** " + testMessage;
		return sendMessage(channelOpt.get(), formattedMessage);
	}
}