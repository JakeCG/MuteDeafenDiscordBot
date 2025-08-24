package com.jakec.mutediscordbot.metrics;

import com.jakec.mutediscordbot.model.VoiceAction;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe metrics collection service for Discord bot operations.
 * Integrates with Micrometer for monitoring and observability.
 */
@Service
@Slf4j
public class BotMetrics {
	
	private final MeterRegistry meterRegistry;
	
	private final Map<VoiceAction, AtomicLong> voiceStateChanges;
	private final AtomicLong successfulAnnouncements = new AtomicLong(0);
	private final AtomicLong failedAnnouncements = new AtomicLong(0);
	private final AtomicLong cooldownBlocks = new AtomicLong(0);
	private final AtomicLong rateLimits = new AtomicLong(0);
	private final AtomicLong errors = new AtomicLong(0);
	private final AtomicLong commandsProcessed = new AtomicLong(0);
	
	private final Counter successfulAnnouncementsCounter;
	private final Counter failedAnnouncementsCounter;
	private final Counter cooldownBlocksCounter;
	private final Counter rateLimitsCounter;
	private final Counter errorsCounter;
	private final Counter commandsProcessedCounter;
	private final Map<VoiceAction, Counter> voiceActionCounters;
	
	public BotMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		
		this.voiceStateChanges = Arrays.stream(VoiceAction.values())
									.collect(Collectors.toConcurrentMap(
										 action -> action,
										 action -> new AtomicLong(0),
										 (existing, replacement) -> existing,
										 ConcurrentHashMap::new
									));
		
		this.successfulAnnouncementsCounter = Counter.builder("bot.announcements.success")
												.description("Number of successful announcements")
												.register(meterRegistry);
		
		this.failedAnnouncementsCounter = Counter.builder("bot.announcements.failed")
											.description("Number of failed announcements")
											.register(meterRegistry);
		
		this.cooldownBlocksCounter = Counter.builder("bot.cooldown.blocks")
										.description("Number of announcements blocked by cooldown")
										.register(meterRegistry);
		
		this.rateLimitsCounter = Counter.builder("bot.rate.limits")
									.description("Number of rate limit hits")
									.register(meterRegistry);
		
		this.errorsCounter = Counter.builder("bot.errors")
								.description("Number of bot errors")
								.register(meterRegistry);
		
		this.commandsProcessedCounter = Counter.builder("bot.commands.processed")
											.description("Number of commands processed")
											.register(meterRegistry);
		
		this.voiceActionCounters = Arrays.stream(VoiceAction.values())
										.collect(Collectors.toConcurrentMap(
										action -> action,
										action -> Counter.builder("bot.voice.actions")
																	.description("Voice state changes by action")
																	.tag("action", action.name().toLowerCase())
																	.register(meterRegistry),
										(existing, replacement) -> existing,
											ConcurrentHashMap::new
										));
		
		registerGauges();
	}
	
	public void incrementVoiceStateChanges(VoiceAction action) {
		if (action == null) {
			log.warn("Attempted to increment voice state changes with null action");
			return;
		}
		
		voiceStateChanges.get(action).incrementAndGet();
		voiceActionCounters.get(action).increment();
	}
	
	public long getVoiceStateChanges(VoiceAction action) {
		return action != null ? voiceStateChanges.get(action).get() : 0;
	}
	
	public long getTotalVoiceStateChanges() {
		return voiceStateChanges.values().stream()
					   .mapToLong(AtomicLong::get)
					   .sum();
	}
	
	public void incrementSuccessfulAnnouncements() {
		successfulAnnouncements.incrementAndGet();
		successfulAnnouncementsCounter.increment();
	}
	
	public void incrementFailedAnnouncements() {
		failedAnnouncements.incrementAndGet();
		failedAnnouncementsCounter.increment();
	}
	
	public long getSuccessfulAnnouncements() {
		return successfulAnnouncements.get();
	}
	
	public long getFailedAnnouncements() {
		return failedAnnouncements.get();
	}
	
	public BigDecimal getSuccessRate() {
		long total = successfulAnnouncements.get() + failedAnnouncements.get();
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		
		return BigDecimal.valueOf(successfulAnnouncements.get())
					   .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
					   .multiply(BigDecimal.valueOf(100));
	}
	
	public void incrementCooldownBlocks() {
		cooldownBlocks.incrementAndGet();
		cooldownBlocksCounter.increment();
	}
	
	public void incrementRateLimits() {
		rateLimits.incrementAndGet();
		rateLimitsCounter.increment();
	}
	
	public long getCooldownBlocks() {
		return cooldownBlocks.get();
	}
	
	public long getRateLimits() {
		return rateLimits.get();
	}
	
	public void incrementErrors() {
		errors.incrementAndGet();
		errorsCounter.increment();
	}
	
	public void incrementCommandsProcessed() {
		commandsProcessed.incrementAndGet();
		commandsProcessedCounter.increment();
	}
	
	public long getErrors() {
		return errors.get();
	}
	
	public long getCommandsProcessed() {
		return commandsProcessed.get();
	}
	
	public MetricsSnapshot getSnapshot() {
		Map<String, Long> voiceActionCounts = voiceStateChanges.entrySet().stream()
																.collect(Collectors.toMap(
										entry -> entry.getKey()
																					.name().toLowerCase(),
															entry -> entry
																									.getValue().get()
																));
		
		return new MetricsSnapshot(
				getTotalVoiceStateChanges(),
				Collections.unmodifiableMap(voiceActionCounts),
				successfulAnnouncements.get(),
				failedAnnouncements.get(),
				getSuccessRate(),
				cooldownBlocks.get(),
				rateLimits.get(),
				errors.get(),
				commandsProcessed.get()
		);
	}
	
	public record MetricsSnapshot(
			long totalVoiceStateChanges,
			Map<String, Long> voiceActionCounts,
			long successfulAnnouncements,
			long failedAnnouncements,
			BigDecimal successRatePercentage,
			long cooldownBlocks,
			long rateLimits,
			long errors,
			long commandsProcessed
	) {}
	
	public void reset() {
		log.info("Resetting all bot metrics");
		voiceStateChanges.values().forEach(counter -> counter.set(0));
		successfulAnnouncements.set(0);
		failedAnnouncements.set(0);
		cooldownBlocks.set(0);
		rateLimits.set(0);
		errors.set(0);
		commandsProcessed.set(0);
	}
	
	private void registerGauges() {
		Gauge.builder("bot.voice.state.changes.total", this, BotMetrics::getTotalVoiceStateChanges)
				.description("Total voice state changes")
				.register(meterRegistry);
		
		Gauge.builder("bot.announcements.success.rate", this, metrics -> metrics
																								.getSuccessRate()
																								.doubleValue())
				.description("Announcement success rate percentage")
				.register(meterRegistry);
		
		Gauge.builder("bot.errors.total", this, BotMetrics::getErrors)
				.description("Total errors encountered")
				.register(meterRegistry);
	}
}