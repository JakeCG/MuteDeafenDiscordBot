package com.jakec.mutediscordbot.model;

import java.time.Instant;

public sealed interface AnnouncementResult {
	
	record Success(String message, String channelName, Instant timestamp)
			implements AnnouncementResult {}
	
	record Failure(String errorMessage, Instant timestamp)
			implements AnnouncementResult {}
	
	static Success success(String message, String channelName) {
		return new Success(message, channelName, Instant.now());
	}
	
	static Failure failure(String errorMessage) {
		return new Failure(errorMessage, Instant.now());
	}
	
	default boolean isSuccess() {
		return this instanceof Success;
	}
	
	default boolean isFailure() {
		return this instanceof Failure;
	}
	
	default String getMessage() {
		return switch (this) {
			case Success(var message, var channelName, var timestamp) -> message;
			case Failure(var errorMessage, var timestamp) -> errorMessage;
		};
	}
}