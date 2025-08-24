package com.jakec.mutediscordbot.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Builder;
import lombok.With;
import net.dv8tion.jda.api.entities.Member;

import java.time.Instant;
import java.util.Objects;

@Builder
@With
public record VoiceStateChange(
	@NotNull Member member,
	@NotNull VoiceAction action,
	@PastOrPresent @NotNull Instant timestamp,
	@NotBlank String guildId
	) {
	
	public VoiceStateChange {
		Objects.requireNonNull(member, "Member cannot be null");
		Objects.requireNonNull(action, "Action cannot be null");
		Objects.requireNonNull(timestamp, "Timestamp cannot be null");
	}
	
	public static VoiceStateChange of(Member member, VoiceAction action) {
		return VoiceStateChange.builder()
								.member(member)
								.action(action)
								.timestamp(Instant.now())
								.guildId(member.getGuild().getId())
								.build();
	}
	
	public String getUserName() {
		return member.getEffectiveName();
	}
	
	public String getUserDisplayName() {
		return member.getUser().getGlobalName() != null
			? member.getUser().getGlobalName() : member.getUser().getEffectiveName();
	}
	
	public long getUserId() {
		return member.getUser().getIdLong();
	}
	
	public boolean isBot() {
		return member.getUser().isBot();
	}
	
	public boolean isMuteAction() {
		return action.isMuteAction();
	}
	
	public boolean isDeafenAction() {
		return action.isDeafenAction();
	}
	
	public String getActionEmoji() {
		return action.getDefaultEmoji();
	}
}
