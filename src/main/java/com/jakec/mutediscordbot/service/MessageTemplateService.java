package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.config.BotProperties;
import com.jakec.mutediscordbot.model.VoiceAction;
import com.jakec.mutediscordbot.model.VoiceStateChange;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.channel.Channel;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Service
@Slf4j
public class MessageTemplateService {
	
	private final BotProperties botProperties;
	private final Map<VoiceAction, List<String>> actionTemplates;
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	public MessageTemplateService(BotProperties botProperties) {
		this.botProperties = botProperties;
		this.actionTemplates = Map.of(
				VoiceAction.MUTED, botProperties.getMessages().getMuteTemplates(),
				VoiceAction.UNMUTED, botProperties.getMessages().getUnmuteTemplates(),
				VoiceAction.DEAFENED, botProperties.getMessages().getDeafenTemplates(),
				VoiceAction.UNDEAFENED, botProperties.getMessages().getUndeafenTemplates()
		);
		
		log.info("Initialized message templates for {} voice actions", actionTemplates.size());
	}
	
	public Optional<String> generateMessage(VoiceStateChange stateChange) {
		if (stateChange == null) {
			log.warn("Cannot generate message for null state change");
			return Optional.empty();
		}
		
		Optional<String> customTemplate = getCustomUserTemplate(stateChange.getUserId(), stateChange.action());
		if (customTemplate.isPresent()) {
			return customTemplate.map(template -> formatTemplate(template, stateChange));
		}
		
		return getRandomTemplate(stateChange.action())
					   .map(template -> formatTemplate(template, stateChange));
	}
	
	public Optional<String> getRandomTemplate(VoiceAction action) {
		List<String> templates = actionTemplates.get(action);
		
		if (templates == null || templates.isEmpty()) {
			log.warn("No templates configured for action: {}", action);
			return Optional.empty();
		}
		
		int randomIndex = ThreadLocalRandom.current().nextInt(templates.size());
		return Optional.of(templates.get(randomIndex));
	}
	
	public List<String> getTemplatesForAction(VoiceAction action) {
		return Optional.ofNullable(actionTemplates.get(action))
						.orElse(List.of());
	}
	
	public Function<VoiceStateChange, String> createCustomFormatter(String template) {
		return stateChange -> formatTemplate(template, stateChange);
	}
	
	private String formatTemplate(String template, VoiceStateChange stateChange) {
		return template
				.replace("{user}", getUserDisplayName(stateChange))
				.replace("{action}", stateChange.action().name().toLowerCase())
				.replace("{emoji}", stateChange.getActionEmoji())
				.replace("{time}", LocalTime.now().format(timeFormatter))
				.replace("{channel}", getChannelName(stateChange))
				.replace("{guild}", getGuildId(stateChange));
	}
	
	private String getUserDisplayName(VoiceStateChange stateChange) {
		return botProperties.getAnnouncements().isUseNicknames()
					   ? stateChange.getUserDisplayName()
					   : stateChange.getUserName();
	}
	
	private String getChannelName(VoiceStateChange stateChange) {
		return Optional.ofNullable(stateChange.member().getVoiceState())
									.map(GuildVoiceState::getChannel)
									.map(Channel::getName)
									.orElse("voice-channel");
	}
	
	private String getGuildId(VoiceStateChange stateChange) {
		return stateChange.guildId();
	}
	
	private Optional<String> getCustomUserTemplate(long userId, VoiceAction action) {
		String userIdStr = String.valueOf(userId);
		List<String> customMessages = botProperties.getMessages()
													.getCustomUserMessages()
													.get(userIdStr);
		
		if (customMessages == null || customMessages.isEmpty()) {
			return Optional.empty();
		}
		
		int randomIndex = ThreadLocalRandom.current().nextInt(customMessages.size());
		return Optional.of(customMessages.get(randomIndex));
	}
	
	public Map<String, Object> getTemplateStats() {
		int totalTemplates = actionTemplates.values().stream()
														.mapToInt(List::size)
														.sum();
		
		int customUserCount = botProperties.getMessages()
											.getCustomUserMessages()
											.size();
		
		return Map.of(
				"totalDefaultTemplates", totalTemplates,
				"customUserCount", customUserCount,
				"actionsConfigured", actionTemplates.size(),
				"useNicknames", botProperties.getAnnouncements().isUseNicknames()
		);
	}
}