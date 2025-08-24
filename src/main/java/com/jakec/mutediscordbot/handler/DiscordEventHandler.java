package com.jakec.mutediscordbot.handler;

import com.jakec.mutediscordbot.metrics.BotMetrics;
import com.jakec.mutediscordbot.service.CommandService;
import com.jakec.mutediscordbot.service.MessageTemplateService;
import com.jakec.mutediscordbot.service.VoiceStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventHandler extends ListenerAdapter {
	
	private final VoiceStateService voiceStateService;
	private final CommandService commandService;
	private final BotMetrics botMetrics;
	private final MessageTemplateService messageTemplateService;
	
	@Override
	public void onReady(ReadyEvent event) {
		log.info("Bot ready! Logged in as: {}", event.getJDA().getSelfUser().getName());
		log.info("Connected to {} guilds", event.getJDA().getGuilds().size());
		
		Map<String, Object> templateStats = messageTemplateService.getTemplateStats();
		log.info("Template stats: {}", templateStats);
	}
	
	@Override
	public void onGuildVoiceSelfMute(@NotNull GuildVoiceSelfMuteEvent event) {
		handleVoiceStateEvent(event.getVoiceState());
	}
	
	@Override
	public void onGuildVoiceSelfDeafen(@NotNull GuildVoiceSelfDeafenEvent event) {
		handleVoiceStateEvent(event.getVoiceState());
	}
	
	@Override
	public void onGuildVoiceGuildMute(@NotNull GuildVoiceGuildMuteEvent event) {
		handleVoiceStateEvent(event.getVoiceState());
	}
	
	@Override
	public void onGuildVoiceGuildDeafen(@NotNull GuildVoiceGuildDeafenEvent event) {
		handleVoiceStateEvent(event.getVoiceState());
	}
	
	private void handleVoiceStateEvent(GuildVoiceState newState) {
		try {
			if (newState == null) return;
			
			Member member = newState.getMember();
			voiceStateService.handleVoiceStateUpdate(member, newState, newState.getGuild());
			
		} catch (Exception e) {
			log.error("Error handling voice state update: {}", e.getMessage(), e);
			botMetrics.incrementErrors();
		}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		
		String message = event.getMessage().getContentRaw().trim();
		
		if (!message.startsWith("!")) return;
		
		try {
			Optional<String> response = commandService.processCommand(message, event);
			
			response.ifPresent(resp ->
									   event.getChannel().sendMessage(resp).queue(
											   success -> botMetrics.incrementCommandsProcessed(),
											   error -> {
												   log.error("Failed to send command response: {}", error.getMessage());
												   botMetrics.incrementErrors();
											   }
									   )
			);
			
		} catch (Exception e) {
			log.error("Error processing command '{}': {}", message, e.getMessage(), e);
			botMetrics.incrementErrors();
			
			event.getChannel().sendMessage("An error occurred processing your command.").queue();
		}
	}
}
