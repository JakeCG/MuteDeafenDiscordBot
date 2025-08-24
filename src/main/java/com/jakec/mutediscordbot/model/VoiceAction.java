package com.jakec.mutediscordbot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum VoiceAction {
	MUTED("User muted their microphone", "🔇", true),
	UNMUTED("User unmuted their microphone", "🎤", true),
	DEAFENED("User deafened themselves", "👂❌", false),
	UNDEAFENED("User undeafened themselves", "👂", false);
	
	private final String description;
	private final String defaultEmoji;
	private final boolean isMuteAction;
	
	public boolean isDeafenAction() {
		return !isMuteAction;
	}
	
	public Optional<VoiceAction> fromString(String name) {
		return Arrays.stream(values())
				.filter(action -> action.name().equalsIgnoreCase(name))
				.findFirst();
	}
	
	public static Predicate<VoiceAction> isMuted() {
		return VoiceAction::isMuteAction;
	}
	
	public static Predicate<VoiceAction> isDeafened() {
		return VoiceAction::isDeafenAction;
	}
}
