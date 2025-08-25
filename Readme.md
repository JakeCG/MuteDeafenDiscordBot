# Discord Mute/Deafen Announcer Bot

A sophisticated Discord bot that announces when users mute/unmute or deafen/undeafen with fun, customizable messages. Built with modern Java, Spring Boot, and enterprise-grade architecture.

## Features

### Core Functionality
- **Voice State Monitoring** - Automatically detects and announces mute/unmute and deafen/undeafen actions
- **Smart Spam Prevention** - Cooldown system and rate limiting to prevent message flooding
- **Fun Random Messages** - Multiple message templates with emoji and personality
- **Template Variables** - Dynamic messages with `{user}`, `{time}`, `{channel}`, `{guild}` placeholders

### Advanced Features
- **Custom User Messages** - Personalized announcements for specific users
- **Comprehensive Metrics** - Detailed statistics and performance monitoring via Micrometer
- **Health Monitoring** - Connection health checks and automatic reconnection
- **Retry Logic** - Resilient message delivery with automatic retries
- **Production Ready** - Graceful shutdown, proper error handling, and logging

### Bot Commands
- `!ping` - Health check with gateway latency
- `!status` - Bot operational status and guild information
- `!stats` - Usage statistics and success rates
- `!metrics` - Detailed metrics breakdown
- `!templates` - Message template statistics and available variables
- `!voice` - Voice state change statistics with percentages
- `!test` - Send a test announcement to verify functionality
- `!help` - Complete command documentation

## Architecture

This bot showcases modern Java and Spring Boot best practices:

### Clean Architecture
- **Domain Models** - Immutable records and sealed interfaces
- **Service Layer** - Business logic separation with dependency injection
- **Configuration Management** - Type-safe configuration with validation
- **Event Handling** - Clean delegation pattern with command service

### Modern Java Features
- **Sealed Interfaces** - Type-safe result handling with pattern matching
- **Records** - Immutable data classes for better design
- **Switch Expressions** - Clean command routing and result processing
- **Optional API** - Null-safe programming throughout

### Technology Stack
- **Java 21** - Latest LTS with modern language features
- **Spring Boot 3.2** - Enterprise framework with auto-configuration
- **JDA 5.0** - Discord API wrapper with voice state support
- **Micrometer** - Metrics and observability integration
- **Lombok** - Boilerplate reduction and cleaner code
- **SLF4J + Logback** - Structured logging with file rotation

## Quick Start

### Prerequisites
- Java 21 or higher
- Discord Bot Token ([Create one here](https://discord.com/developers/applications))
- Gradle 8.0+ (or use included wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/discord-mute-deafen-bot.git
   cd discord-mute-deafen-bot
   ```

2. **Set up environment variables**
   ```bash
   # Linux/Mac
   export DISCORD_BOT_TOKEN=your_bot_token_here
   
   # Windows
   set DISCORD_BOT_TOKEN=your_bot_token_here
   ```

3. **Build and run**
   ```bash
   ./gradlew bootRun
   ```

4. **Invite bot to your server**
    - Go to [Discord Developer Portal](https://discord.com/developers/applications)
    - Select your application → OAuth2 → URL Generator
    - Check `bot` scope and required permissions:
        - Send Messages
        - View Channels
        - Use Embedded Activities (optional, for rich messages)
    - Use generated URL to invite bot to your server

## Configuration

Configure the bot using `application.yml`:

```yaml
discord:
  bot:
    token: ${DISCORD_BOT_TOKEN}
    announcement-channel: general
    announcements:
      mute: true
      deafen: true
      include-bots: false
      use-nicknames: true
    spam-prevention:
      cooldown: PT3S
      max-announcements-per-minute: 20
      enable-rate-limit: true
    messages:
      mute-templates:
        - "🤫 **{user}** has gone silent!"
        - "🎤❌ **{user}** dropped the mic!"
        - "🔇 **{user}** is now in stealth mode!"
      unmute-templates:
        - "🎤 **{user}** is back on the mic!"
        - "🔊 **{user}** has returned to the conversation!"
        - "💬 **{user}** is ready to speak again!"
      # ... additional templates
      custom-user-messages:
        "123456789012345678": # User ID
          - "The boss has spoken!"
          - "Our leader emerges!"
```

### Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| `announcement-channel` | Channel name for announcements | `general` |
| `announcements.mute` | Enable mute/unmute announcements | `true` |
| `announcements.deafen` | Enable deafen/undeafen announcements | `true` |
| `announcements.include-bots` | Include bot voice changes | `false` |
| `announcements.use-nicknames` | Use server nicknames vs usernames | `true` |
| `spam-prevention.cooldown` | Cooldown between user announcements | `PT3S` (3 seconds) |
| `spam-prevention.max-announcements-per-minute` | Rate limit per user | `20` |

### Template Variables

Use these placeholders in your message templates:

- `{user}` - User display name (respects nickname setting)
- `{action}` - Voice action (muted, unmuted, deafened, undeafened)
- `{emoji}` - Action-specific emoji
- `{time}` - Current time (HH:mm:ss format)
- `{channel}` - Voice channel name
- `{guild}` - Guild/server ID

## Monitoring & Metrics

### Built-in Endpoints
- `/actuator/health` - Application health status
- `/actuator/metrics` - All available metrics
- `/actuator/prometheus` - Prometheus metrics export

### Key Metrics
- `bot.voice.state.changes.total` - Total voice state changes
- `bot.announcements.success` - Successful announcements counter
- `bot.announcements.failed` - Failed announcements counter
- `bot.announcements.success.rate` - Success rate percentage gauge
- `bot.errors.total` - Total errors encountered

### Dashboard Commands
Use bot commands in Discord for real-time monitoring:
- `!stats` - Quick overview with success rates
- `!metrics` - Detailed metrics breakdown
- `!voice` - Voice action statistics with percentages

## Production Deployment

### Docker Deployment

```dockerfile
FROM openjdk:21-jre-slim

WORKDIR /app
COPY build/libs/discord-mute-bot-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run as non-root user
RUN adduser --system --group discord-bot
USER discord-bot

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Variables

```bash
DISCORD_BOT_TOKEN=your_production_token
SPRING_PROFILES_ACTIVE=production
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_JAKEC_MUTEDISCORDBOT=DEBUG
```

### Production Configuration

```yaml
# application-production.yml
discord:
  bot:
    spam-prevention:
      cooldown: PT5S  # Longer cooldown for production
      max-announcements-per-minute: 10

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    com.jakec.mutediscordbot: INFO
  file:
    name: logs/discord-bot.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
```

## Development

### Project Structure
```
src/main/java/com/jakec/mutediscordbot/
├── model/                    # Domain models (records, enums)
│   ├── VoiceAction.java
│   ├── VoiceStateChange.java
│   └── AnnouncementResult.java
├── config/                   # Spring configuration
│   ├── BotProperties.java
│   ├── BotConfiguration.java
│   └── JdaConfiguration.java
├── service/                  # Business logic services
│   ├── VoiceStateService.java
│   ├── AnnouncementService.java
│   ├── MessageTemplateService.java
│   ├── ChannelService.java
│   ├── CooldownService.java
│   └── CommandService.java
├── handler/                  # Discord event handling
│   └── DiscordEventHandler.java
├── metrics/                  # Observability
│   └── BotMetrics.java
└── MuteDeafenBotApplication.java
```

### Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/discord-mute-deafen-bot.git
cd discord-mute-deafen-bot

# Run tests
./gradlew test

# Build executable JAR
./gradlew bootJar

# Run locally
./gradlew bootRun
```

### Adding New Commands

1. Add command constant in `CommandService.Commands`
2. Add case in `processCommand()` switch expression
3. Implement command handler method
4. Update help message in `getHelpMessage()`

Example:
```java
private static final class Commands {
    static final String PING = "!ping";
    static final String NEW_COMMAND = "!newcommand";  // Add here
}

public Optional<String> processCommand(String command, MessageReceivedEvent event) {
    return switch (command.toLowerCase()) {
        case Commands.NEW_COMMAND -> Optional.of(handleNewCommand(event));  // Add here
        // ... other cases
    };
}

private String handleNewCommand(MessageReceivedEvent event) {
    return "New command response!";
}
```

## Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Style Guidelines
- Use **Java 21** features when appropriate
- Follow **Spring Boot** best practices
- Use **descriptive commit messages**
- Update **documentation** for new features

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
