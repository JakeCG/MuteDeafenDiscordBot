package com.jakec.mutediscordbot.service;

import com.jakec.mutediscordbot.config.BotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS;
import static net.dv8tion.jda.api.Permission.MESSAGE_SEND;
import static net.dv8tion.jda.api.Permission.VIEW_CHANNEL;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {
	
	private final BotProperties botProperties;
	
	public Optional<TextChannel> findAnnouncementChannel(Guild guild) {
		String channelName = botProperties.getAnnouncementChannel();
		
		return guild.getTextChannelsByName(channelName, true)
							.stream()
							.filter(canSendMessages(guild))
							.findFirst()
							.or(() -> {
								log.warn("Announcement channel '{}' not found in guild '{}'", channelName,
										guild.getName()
								);
								return findFallbackChannel(guild);
							});
	}
	
	public Optional<TextChannel> findFallbackChannel(Guild guild) {
		log.debug("Searching for fallback channel in guild '{}'", guild.getName());
		
		return guild.getTextChannels()
						.stream()
						.filter(canSendMessages(guild))
						.filter(hasCommonName())
						.findFirst()
						.or(() -> guild.getTextChannels()
										.stream()
										.filter(canSendMessages(guild))
										.findFirst());
	}
	
	public List<TextChannel> getAllValidChannels(Guild guild) {
		return guild.getTextChannels()
						.stream()
						.filter(canSendMessages(guild))
						.toList();
	}
	
	private Predicate<TextChannel> canSendMessages(Guild guild) {
		return channel -> channel.canTalk() &&
										guild.getSelfMember().hasPermission(channel, MESSAGE_SEND, VIEW_CHANNEL,
												MESSAGE_EMBED_LINKS);
	}
	
	private Predicate<TextChannel> hasCommonName() {
		List<String> commonNames = List.of("general", "chat", "main", "lobby", "announcements");
		return channel -> commonNames.contains(channel.getName().toLowerCase());
	}
	
	public boolean validateChannelAccess(Guild guild) {
		return findAnnouncementChannel(guild).isPresent() || findFallbackChannel(guild).isPresent();
	}
}
