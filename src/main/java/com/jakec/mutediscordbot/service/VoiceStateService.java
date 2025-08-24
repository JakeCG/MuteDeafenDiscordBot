package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.config.BotProperties;
import com.jakec.mutediscordbot.metrics.BotMetrics;
import com.jakec.mutediscordbot.model.AnnouncementResult;
import com.jakec.mutediscordbot.model.VoiceAction;
import com.jakec.mutediscordbot.model.VoiceStateChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceStateService {
	
	private final AnnouncementService announcementService;
	private final CooldownService cooldownService;
	private final BotProperties botProperties;
	private final BotMetrics botMetrics;
	
	private final ConcurrentHashMap<String, Boolean> muteCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Boolean> deafenCache = new ConcurrentHashMap<>();
	
	public void handleVoiceStateUpdate(Member member, GuildVoiceState newState, Guild guild) {
		if (shouldIgnoreUpdate(member, newState)) {
			return;
		}
		
		String userId = member.getId();
		
		boolean wasMuted = muteCache.getOrDefault(userId, false);
		boolean wasDeafened = deafenCache.getOrDefault(userId, false);
		
		boolean nowMuted = newState.isSelfMuted();
		boolean nowDeafened = newState.isSelfDeafened();
		
		Optional<VoiceAction> action = Optional.empty();
		
		if (!wasMuted && nowMuted) action = Optional.of(VoiceAction.MUTED);
		else if (wasMuted && !nowMuted) action = Optional.of(VoiceAction.UNMUTED);
		else if (!wasDeafened && nowDeafened) action = Optional.of(VoiceAction.DEAFENED);
		else if (wasDeafened && !nowDeafened) action = Optional.of(VoiceAction.UNDEAFENED);
		
		action.map(a -> VoiceStateChange.of(member, a))
				.filter(this::passesFilters)
				.ifPresent(stateChange -> processStateChange(stateChange, guild));
		
		muteCache.put(userId, nowMuted);
		deafenCache.put(userId, nowDeafened);
	}
	
	private boolean shouldIgnoreUpdate(Member member, GuildVoiceState newState) {
		if (newState == null) {
			log.debug("Ignoring update for {}: newState is null", member.getEffectiveName());
			return true;
		}
		
		if (member.getUser().isBot() && !botProperties.getAnnouncements().isIncludeBots()) {
			log.debug("Ignoring bot update for {}", member.getEffectiveName());
			return true;
		}
		
		return false;
	}
	
	private boolean passesFilters(VoiceStateChange stateChange) {
		boolean passes = cooldownService.checkAndUpdate(stateChange.getUserId());
		
		if (!passes) {
			log.debug("State change filtered out for {}: {} (cooldown/rate limit)",
					stateChange.getUserName(), stateChange.action());
		}
		
		return passes;
	}
	
	public void processStateChange(VoiceStateChange stateChange, Guild guild) {
		log.info("Processing {} for {} in {}",
				stateChange.action(),
				stateChange.getUserName(),
				guild.getName());
		
		botMetrics.incrementVoiceStateChanges(stateChange.action());
		
		try {
			AnnouncementResult result = announcementService.processAnnouncement(stateChange, guild);
			
			switch (result) {
				case AnnouncementResult.Success success ->
						log.debug("Successfully processed announcement for {} to {}",
								stateChange.getUserName(), success.channelName());
				
				case AnnouncementResult.Failure failure ->
						log.debug("Announcement not sent for {}: {}",
								stateChange.getUserName(), failure.errorMessage());
			}
			
		} catch (Exception e) {
			log.error("Error processing announcement for {}: {}",
					stateChange.getUserName(), e.getMessage(), e);
			botMetrics.incrementErrors();
		}
	}
	
	public VoiceStateStats getStats() {
		return new VoiceStateStats(
				botMetrics.getTotalVoiceStateChanges(),
				botMetrics.getVoiceStateChanges(VoiceAction.MUTED),
				botMetrics.getVoiceStateChanges(VoiceAction.UNMUTED),
				botMetrics.getVoiceStateChanges(VoiceAction.DEAFENED),
				botMetrics.getVoiceStateChanges(VoiceAction.UNDEAFENED)
		);
	}
	
	public record VoiceStateStats(
			long totalChanges,
			long muteCount,
			long unmuteCount,
			long deafenCount,
			long undeafenCount
	) {}
}
