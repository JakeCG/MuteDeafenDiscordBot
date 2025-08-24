package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.config.BotProperties;
import com.jakec.mutediscordbot.metrics.BotMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CooldownService {
	
	private final BotProperties botProperties;
	private final BotMetrics botMetrics;
	private final Map<Long, Instant> cooldowns = new ConcurrentHashMap<>();
	private final Map<Long, AtomicInteger> rateLimitCounters = new ConcurrentHashMap<>();
	
	public boolean isUserOnCooldown(long userId) {
		if (!botProperties.getSpamPrevention().isEnableRateLimit()) {
			return false;
		}
		
		Instant cooldownTime = cooldowns.get(userId);
		return cooldownTime != null && Duration.between(cooldownTime, Instant.now())
											   .compareTo(botProperties.getSpamPrevention().getCooldown()) < 0;
	}
	
	public boolean checkAndUpdate(long userId) {
		if (!botProperties.getSpamPrevention().isEnableRateLimit()) {
			return true;
		}
		if (isUserOnCooldown(userId)) {
			botMetrics.incrementCooldownBlocks();
			log.debug("User {} blocked by cooldown", userId);
			return false;
		}
		
		if (isRateLimited(userId)) {
			botMetrics.incrementRateLimits();
			log.warn("User {} is rate limited", userId);
			return false;
		}
		
		updateCooldown(userId);
		incrementRateCounter(userId);
		return true;
	}
	
	private void updateCooldown(long userId) {
		cooldowns.put(userId, Instant.now());
	}
	
	private boolean isRateLimited(long userId) {
		AtomicInteger counter = rateLimitCounters.get(userId);
		return counter != null && counter.get() >= botProperties.getSpamPrevention().getMaxAnnouncementsPerMinute();
	}
	
	private void incrementRateCounter(long userId) {
		rateLimitCounters.computeIfAbsent(userId, k -> new AtomicInteger(0))
				.incrementAndGet();
	}
	
	@Scheduled(fixedRate = 60000) // Every minute
	public void resetRateCounters() {
		int clearedUsers = rateLimitCounters.size();
		rateLimitCounters.clear();
		log.debug("Reset rate counters for {} users", clearedUsers);
	}
	
	@Scheduled(fixedRate = 300000) // Every 5 minutes
	public void cleanupOldCooldowns() {
		Instant cutoff = Instant.now().minus(botProperties.getSpamPrevention().getCooldown()
																				.multipliedBy(2));
		int initialSize = cooldowns.size();
		
		cooldowns.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
		
		int removed = initialSize - cooldowns.size();
		if (removed > 0) {
			log.debug("Cleaned up {} old cooldown entries", removed);
		}
	}
	
	public Map<String, Object> getStats() {
		return Map.of(
				"activeCooldowns", cooldowns.size(),
				"activeRateLimits", rateLimitCounters.size(),
				"cooldownDuration", botProperties.getSpamPrevention().getCooldown().toString()
		);
	}
}
