# ArkOps-Ai

ArkOps-Ai is an AI-powered server operations management plugin for PurpurMC (Minecraft). It enables server administrators to manage their servers through natural language commands powered by OpenAI's GPT models.

## Features

- **AI-Powered Operations**: Use natural language to manage your server through the `/ops` command
- **Multi-Step Agent System**: AI can execute complex tasks in multiple steps with automatic permission checks
- **Permission-Based Access Control**: Four permission levels ensure secure operations
- **Internationalization**: Supports English and Chinese languages
- **Comprehensive Server Management**: Restart, stop, reload, plugin management, player management, world management, and more

## Permission Levels

| Level | Description | Capabilities |
|-------|-------------|--------------|
| **DISABLED** | No permissions | Cannot use any features |
| **PLAYER** | Default | Can only ask game-related questions |
| **ADMIN** | Administrator | Plugin management, player management, world management, command execution |
| **SUPER_ADMIN** | Super Administrator | All permissions including server control, player banning, permission settings |

## Installation

1. Download the latest `ArkOps-Ai-1.0.0.jar` from the [Releases](https://github.com/DreamArk-Studio/ArkOps-Ai/releases) page
2. Place the jar file in your server's `plugins` folder
3. Start or restart your server
4. Configure the plugin in `plugins/ArkOps-Ai/config.yml`
5. Add your OpenAI API key to the configuration file

## Configuration

### config.yml

```yaml
# Language setting (en, zh)
language: "en"

# OpenAI API Configuration
openai:
  # OpenAI API Key
  api-key: "YOUR_API_KEY_HERE"
  # Model to use
  model: "gpt-5.4"
  # API timeout (seconds)
  timeout: 60
  # Max response tokens
  max-tokens: 2000
  # Temperature (creativity, 0-1)
  temperature: 0.7
```

### permissions.yml

Configure player permission levels in `plugins/ArkOps-Ai/permissions.yml`:

```yaml
players:
  # Example:
  # 6f12f43f-150b-3437-9836-651e535176ec:
  #   name: "PlayerName"
  #   level: "SUPER_ADMIN"
```

## Usage

### Basic Commands

```
/ops                          # Show help
/ops restart server           # Restart the server
/ops stop server              # Stop the server
/ops hot-reload Essentials    # Hot-reload a plugin
/ops hot-unload PluginName    # Hot-unload a plugin
/ops hot-load PluginName      # Hot-load a plugin
/ops list plugins             # List all plugins with descriptions
/ops execute command <cmd>    # Execute any server command
/ops set game time to night   # Set game time
/ops set weather to clear     # Set weather
/ops set my gamemode to creative  # Set game mode
/ops server status            # Get server status
/ops player info <name>       # Get player information
/ops teleport <player> to <target>  # Teleport player
/ops give item diamond 64 to <player>  # Give items
/ops kick player <name>       # Kick a player
/ops ban player <name>        # Ban a player (SUPER_ADMIN only)
```

### Natural Language Examples

The AI understands natural language, so you can use flexible commands:

- `/ops restart the server please`
- `/ops what plugins are installed?`
- `/ops set the time to sunset`
- `/ops give me 64 diamonds`
- `/ops teleport me to the spawn`

## Building from Source

### Requirements

- Java 21
- Maven 3.6+

### Build Steps

```bash
git clone https://github.com/DreamArk-Studio/ArkOps-Ai.git
cd ArkOps-Ai
mvn clean package -DskipTests
```

The compiled jar file will be in `target/ArkOps-Ai-1.0.0.jar`.

## Architecture

- **ArkOpsAi**: Main plugin class
- **LanguageManager**: Handles internationalization
- **PermissionManager**: Manages player permission levels
- **OpenAiManager**: Handles OpenAI API communication
- **ServerActionManager**: Executes server operations
- **OpsCommandExecutor**: Command entry point
- **OpsCommandHandler**: AI agent loop and tool execution

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues, feature requests, or questions, please open an issue on [GitHub](https://github.com/DreamArk-Studio/ArkOps-Ai/issues).
