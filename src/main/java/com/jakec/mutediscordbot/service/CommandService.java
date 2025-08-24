package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.metrics.BotMetrics;
import com.jakec.mutediscordbot.model.AnnouncementResult;
import com.jakec.mutediscordbot.model.VoiceAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {
	
	private final BotMetrics botMetrics;
	private final MessageTemplateService messageTemplateService;
	private final VoiceStateService voiceStateService;
	private final AnnouncementService announcementService;
	
	private BotMetrics.MetricsSnapshot getMetricsSnapshot() {
		return botMetrics.getSnapshot();
	}
	
	private VoiceStateService.VoiceStateStats getVoiceStats() {
		return voiceStateService.getStats();
	}
	
	private Map<String, Object> getTemplateStats() {
		return messageTemplateService.getTemplateStats();
	}
	
	private int getIntFromStats(Map<String, Object> stats, String key) {
		return ((Number) stats.get(key)).intValue();
	}
	
	private static class MessageBuilder {
		private final StringBuilder builder = new StringBuilder();
		
		MessageBuilder append(String text) {
			builder.append(text);
			return this;
		}
		
		MessageBuilder appendLine(String format, Object... args) {
			builder.append(String.format(format + "\n", args));
			return this;
		}
		
		MessageBuilder appendSection(String title) {
			builder.append("\n").append(title).append("\n");
			return this;
		}
		
		String build() {
			return builder.toString();
		}
	}
	
	private static final class Commands {
		static final String PING = "!ping";
		static final String HELP = "!help";
		static final String STATUS = "!status";
		static final String STATS = "!stats";
		static final String METRICS = "!metrics";
		static final String TEMPLATES = "!templates";
		static final String VOICE = "!voice";
		static final String TEST = "!test";
	}
	
	public Optional<String> processCommand(String command, MessageReceivedEvent event) {
		String lowerCommand = command.toLowerCase();
		
		return switch (lowerCommand) {
			case Commands.PING -> Optional.of(handlePingCommand(event));
			case Commands.HELP -> Optional.of(getHelpMessage());
			case Commands.STATUS -> Optional.of(getStatusMessage(event));
			case Commands.STATS -> Optional.of(getStatsMessage());
			case Commands.METRICS -> Optional.of(getMetricsMessage());
			case Commands.TEMPLATES -> Optional.of(getTemplatesMessage());
			case Commands.VOICE -> Optional.of(getVoiceStatsMessage());
			case Commands.TEST -> Optional.of(handleTestCommand(event));
			default -> Optional.empty(); // Unknown command - no response
		};
	}
	
	private String handlePingCommand(MessageReceivedEvent event) {
		long ping = event.getJDA().getGatewayPing();
		return String.format("🏓 Pong! Gateway ping: %dms", ping);
	}
	
	private String getHelpMessage() {
		return """
            🤖 **Mute/Deafen Bot Commands:**
            • `!ping` - Health check with latency
            • `!status` - Bot operational status
            • `!stats` - Usage statistics
            • `!metrics` - Detailed metrics snapshot
            • `!templates` - Message template statistics
            • `!voice` - Voice state change statistics
            • `!test` - Send a test announcement
            • `!help` - This help message
            
            🎭 **Features:**
            • Announces mute/unmute actions
            • Announces deafen/undeafen actions
            • Smart spam prevention with cooldowns
            • Fun random messages with {user}, {time}, {channel}, {guild} variables
            • Retry mechanism for reliable message delivery
            """;
	}
	
	private String getStatusMessage(MessageReceivedEvent event) {
		long guilds = event.getJDA().getGuilds().size();
		Map<String, Object> templateStats = getTemplateStats();
		
		return String.format("""
            ✅ **Bot Status: ONLINE**
            🏰 Connected to %d guilds
            📊 Monitoring voice state changes
            📝 %d message templates loaded
            👥 %d users with custom messages
            🎯 Ready to announce!
            """,
				guilds,
				getIntFromStats(templateStats, "totalDefaultTemplates"),
				getIntFromStats(templateStats, "customUserCount"));
	}
	
	private String getStatsMessage() {
		BotMetrics.MetricsSnapshot metrics = getMetricsSnapshot();
		
		return String.format("""
            📈 **Bot Statistics:**
            🎭 Voice changes: %d
            📢 Announcements: %d successful, %d failed
            📊 Success rate: %.2f%%
            🚫 Cooldown blocks: %d
            ⚡ Rate limits: %d
            💥 Errors: %d
            ⌨️ Commands processed: %d
            """,
				metrics.totalVoiceStateChanges(),
				metrics.successfulAnnouncements(),
				metrics.failedAnnouncements(),
				metrics.successRatePercentage().doubleValue(),
				metrics.cooldownBlocks(),
				metrics.rateLimits(),
				metrics.errors(),
				metrics.commandsProcessed());
	}
	
	private String getMetricsMessage() {
		BotMetrics.MetricsSnapshot metrics = getMetricsSnapshot();
		
		MessageBuilder builder = new MessageBuilder()
									.appendLine("📊 **Detailed Metrics:**")
									.appendLine("🎭 Total Voice Changes: %d", metrics.totalVoiceStateChanges())
									.append("**Voice Actions:**\n");
		
		metrics.voiceActionCounts().forEach((action, count) ->
													builder.appendLine("  • %s: %d", action, count));
		
		return builder
			.appendSection("**Announcement Performance:**")
			.appendLine("✅ Successful: %d", metrics.successfulAnnouncements())
			.appendLine("❌ Failed: %d", metrics.failedAnnouncements())
			.appendLine("📈 Success Rate: %.2f%%", metrics.successRatePercentage().doubleValue())
			.appendSection("**Rate Limiting:**")
			.appendLine("🚫 Cooldown Blocks: %d", metrics.cooldownBlocks())
			.appendLine("⚡ Rate Limits: %d", metrics.rateLimits())
			.appendSection("**System:**")
			.appendLine("💥 Errors: %d", metrics.errors())
			.appendLine("⌨️ Commands: %d", metrics.commandsProcessed())
			.build();
	}
	
	private String getTemplatesMessage() {
		Map<String, Object> templateStats = getTemplateStats();
		
		MessageBuilder builder = new MessageBuilder()
										.appendLine("📝 **Message Template Statistics:**")
										.appendLine("📊 Total Default Templates: %d",
												getIntFromStats(templateStats, "totalDefaultTemplates"))
										.appendLine("👥 Users with Custom Messages: %d",
												getIntFromStats(templateStats, "customUserCount"))
										.appendLine("🎭 Actions Configured: %d",
												getIntFromStats(templateStats, "actionsConfigured"))
										.appendLine("🏷️ Use Nicknames: %s", templateStats.get("useNicknames"))
										.append("""
                    
                    **Available Template Variables:**
                    • `{user}` - User display name
                    • `{action}` - Voice action (muted/unmuted/etc.)
                    • `{emoji}` - Action emoji
                    • `{time}` - Current time (HH:mm:ss)
                    • `{channel}` - Voice channel name
                    • `{guild}` - Guild ID
                    
                    **Template counts per action:**
                    """
										);
		
		for (VoiceAction action : VoiceAction.values()) {
			List<String> templates = messageTemplateService.getTemplatesForAction(action);
			builder.appendLine("  • %s: %d templates",
					action.name().toLowerCase(), templates.size());
		}
		
		return builder.build();
	}
	
	private String getVoiceStatsMessage() {
		VoiceStateService.VoiceStateStats voiceStats = getVoiceStats();
		
		MessageBuilder builder = new MessageBuilder()
										.appendLine("🎭 **Voice State Statistics:**")
										.appendLine("📊 Total Voice Changes: %d", voiceStats.totalChanges())
										.appendSection("**Action Breakdown:**")
										.appendLine("🔇 Mutes: %d", voiceStats.muteCount())
										.appendLine("🎤 Unmutes: %d", voiceStats.unmuteCount())
										.appendLine("👂❌ Deafens: %d", voiceStats.deafenCount())
										.appendLine("👂 Undeafens: %d", voiceStats.undeafenCount());
		
		if (voiceStats.totalChanges() > 0) {
			builder.appendSection("**Action Distribution:**")
					.appendLine("Mutes: %.1f%%",
							(voiceStats.muteCount() * 100.0) / voiceStats.totalChanges())
					.appendLine("Unmutes: %.1f%%",
							(voiceStats.unmuteCount() * 100.0) / voiceStats.totalChanges())
					.appendLine("Deafens: %.1f%%",
							(voiceStats.deafenCount() * 100.0) / voiceStats.totalChanges())
					.appendLine("Undeafens: %.1f%%",
							(voiceStats.undeafenCount() * 100.0) / voiceStats.totalChanges());
		}
		
		return builder.build();
	}
	
	private String handleTestCommand(MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return "Test command only works in servers!";
		}
		
		AnnouncementResult result = announcementService.sendTestAnnouncement(
				event.getGuild(),
				"Bot functionality check from " + event.getAuthor().getEffectiveName()
		);
		
		return switch (result) {
			case AnnouncementResult.Success success ->
					"Test announcement sent successfully to #" + success.channelName() + "!";
			case AnnouncementResult.Failure failure ->
					"Failed to send test announcement: " + failure.errorMessage();
		};
	}
}