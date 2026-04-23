package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.LanguageManager;
import com.arkops.manager.PermissionManager;
import com.arkops.manager.ServerActionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class OpsCommandHandler {

    private final ArkOpsAi plugin;
    private final LanguageManager lang;
    private final PermissionManager permissionManager;
    private final ServerActionManager actionManager;

    public OpsCommandHandler(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.permissionManager = plugin.getPermissionManager();
        this.actionManager = plugin.getServerActionManager();
    }

    public void handleCommand(CommandSender sender, String command) {
        UUID playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        String playerName = sender.getName();
        PermissionManager.PermissionLevel level = permissionManager.getPermissionLevel(playerId);

        sender.sendMessage(lang.getMessage("command.ai_analyzing"));

        String systemPrompt = buildSystemPrompt(playerName, level, sender);
        JsonArray tools = buildTools(level);

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", command);
        messages.add(userMsg);

        executeAgentLoop(sender, playerName, playerId, command, messages, tools, 0);
    }

    private void executeAgentLoop(CommandSender sender, String playerName, UUID playerId, String originalCommand, JsonArray messages, JsonArray tools, int iteration) {
        if (iteration >= 10) {
            sender.sendMessage(lang.getMessage("command.max_iterations"));
            return;
        }

        plugin.getOpenAiManager().sendRequestWithMessages(messages, tools).thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (response.has("error")) {
                    sender.sendMessage(lang.getMessage("command.ai_failed", response.get("error").getAsString()));
                    return;
                }

                JsonArray choices = response.getAsJsonArray("choices");
                if (choices == null || choices.size() == 0) {
                    sender.sendMessage(lang.getMessage("command.ai_failed", "Invalid response"));
                    return;
                }

                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");

                if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                    JsonArray toolCalls = message.getAsJsonArray("tool_calls");

                    if (iteration == 0) {
                        sender.sendMessage(lang.getMessage("agent.start"));
                    }

                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.add("tool_calls", message.get("tool_calls"));
                    messages.add(assistantMsg);

                    for (int i = 0; i < toolCalls.size(); i++) {
                        JsonObject toolCall = toolCalls.get(i).getAsJsonObject();
                        String toolCallId = toolCall.get("id").getAsString();
                        String toolName = toolCall.getAsJsonObject("function").get("name").getAsString();
                        String arguments = toolCall.getAsJsonObject("function").get("arguments").getAsString();

                        JsonObject args = com.google.gson.JsonParser.parseString(arguments).getAsJsonObject();
                        String result = executeToolCall(sender, playerName, playerId, toolName, args);

                        if (!toolName.equals("check_permission")) {
                            sender.sendMessage(lang.getMessage("agent.step", iteration + 1, i + 1, toolName));
                            sender.sendMessage(lang.getMessage("agent.result", result));
                        }

                        plugin.getArkOpsLogger().logAction(playerName, lang.getMessage("ai.tool_log", toolName), result);

                        JsonObject toolResult = new JsonObject();
                        toolResult.addProperty("role", "tool");
                        toolResult.addProperty("tool_call_id", toolCallId);
                        toolResult.addProperty("content", result);
                        messages.add(toolResult);
                    }

                    executeAgentLoop(sender, playerName, playerId, originalCommand, messages, tools, iteration + 1);
                } else if (message.has("content") && !message.get("content").isJsonNull()) {
                    String content = message.get("content").getAsString();

                    if (iteration == 0) {
                        sender.sendMessage(lang.getMessage("ai.response", content));
                        plugin.getArkOpsLogger().logAction(playerName, lang.getMessage("ai.qa_log", originalCommand), "Success");
                    } else {
                        sender.sendMessage(lang.getMessage("agent.complete"));
                        sender.sendMessage(lang.getMessage("ai.response", content));
                        plugin.getArkOpsLogger().logAction(playerName, lang.getMessage("ai.agent_log", originalCommand), "Success");
                    }
                }
            });
        }).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(lang.getMessage("command.ai_failed", ex.getMessage()));
            });
            return null;
        });
    }

    private String executeToolCall(CommandSender sender, String playerName, UUID playerId, String toolName, JsonObject args) {
        try {
            switch (toolName) {
                case "check_permission":
                    return checkPermission(playerId, playerName, args);
                case "restart_server":
                    return actionManager.restartServer(sender);
                case "stop_server":
                    return actionManager.stopServer(sender);
                case "reload_server":
                    actionManager.reloadServer(sender);
                    return lang.getMessage("server.reload");
                case "hot_reload_plugin":
                    return actionManager.hotReloadPlugin(sender, args.get("plugin_name").getAsString());
                case "hot_unload_plugin":
                    return actionManager.hotUnloadPlugin(sender, args.get("plugin_name").getAsString());
                case "hot_load_plugin":
                    return actionManager.hotLoadPlugin(sender, args.get("plugin_name").getAsString());
                case "list_plugins":
                    return actionManager.listPlugins();
                case "execute_command":
                    return actionManager.executeCommand(sender, args.get("command").getAsString());
                case "set_game_time":
                    return actionManager.setTime(sender, args.get("time").getAsString());
                case "set_weather":
                    return actionManager.setWeather(sender, args.get("weather").getAsString());
                case "set_game_mode":
                    return actionManager.setGameMode(sender,
                            args.get("player").getAsString(),
                            args.get("game_mode").getAsString());
                case "get_server_info":
                    return actionManager.getServerInfo();
                case "get_player_info":
                    return actionManager.getPlayerInfo(args.get("player").getAsString());
                case "teleport_player":
                    return actionManager.teleportPlayer(sender,
                            args.get("target").getAsString(),
                            args.get("destination").getAsString());
                case "give_item":
                    return actionManager.giveItem(sender,
                            args.get("player").getAsString(),
                            args.get("item").getAsString(),
                            args.has("amount") ? args.get("amount").getAsInt() : 1);
                case "kick_player":
                    return actionManager.kickPlayer(sender,
                            args.get("player").getAsString(),
                            args.has("reason") ? args.get("reason").getAsString() : null);
                case "ban_player":
                    return actionManager.banPlayer(sender,
                            args.get("player").getAsString(),
                            args.has("reason") ? args.get("reason").getAsString() : null);
                case "set_permission":
                    return setPermission(args.get("player").getAsString(), args.get("level").getAsString());
                case "get_online_players":
                    return String.join(", ", actionManager.getOnlinePlayers());
                default:
                    return lang.getMessage("error.general", "Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            return lang.getMessage("error.general", e.getMessage());
        }
    }

    private String checkPermission(UUID playerId, String playerName, JsonObject args) {
        String requiredLevel = args.get("required_level").getAsString();
        PermissionManager.PermissionLevel required = PermissionManager.PermissionLevel.fromString(requiredLevel);
        PermissionManager.PermissionLevel current = permissionManager.getPermissionLevel(playerId);

        if (current.getLevel() >= required.getLevel()) {
            return lang.getMessage("permission.check_passed", current.getDisplayName());
        } else {
            return lang.getMessage("permission.insufficient", current.getDisplayName(), required.getDisplayName());
        }
    }

    private String setPermission(String playerName, String level) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        PermissionManager.PermissionLevel permLevel = PermissionManager.PermissionLevel.fromString(level);
        permissionManager.setPermissionLevel(offlinePlayer.getUniqueId(), playerName, permLevel);
        return lang.getMessage("permission.set_success", playerName, permLevel.getDisplayName());
    }

    private String buildSystemPrompt(String playerName, PermissionManager.PermissionLevel level, CommandSender sender) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are ArkOpsAI, an ArkOpsAI operations assistant for a Minecraft Purpur server.\n\n");

        prompt.append("Current requester: ").append(playerName).append("\n");
        prompt.append("Requester permission level: ").append(level.getDisplayName()).append("\n\n");

        prompt.append("Server info:\n");
        prompt.append("- Version: ").append(plugin.getServer().getVersion()).append("\n");
        prompt.append("- Online players: ").append(plugin.getServer().getOnlinePlayers().size()).append("\n");
        prompt.append("- Plugin count: ").append(plugin.getServer().getPluginManager().getPlugins().length).append("\n\n");

        prompt.append("Working rules:\n");
        prompt.append("1. You must check if the executor has sufficient permission before any operation\n");
        prompt.append("2. For complex tasks, execute in steps (e.g., hot-unload a plugin: check permission, find plugin, then execute)\n");
        prompt.append("3. Log every operation\n");
        prompt.append("4. You can call tools across multiple rounds to complete complex tasks\n");
        prompt.append("5. When all steps are done, return a concise summary\n");
        prompt.append("6. If permission is insufficient, return error directly, do not continue\n\n");

        prompt.append("Permission levels:\n");
        prompt.append("- DISABLED: No permissions\n");
        prompt.append("- PLAYER: Can only ask game-related questions\n");
        prompt.append("- ADMIN: Can manage plugins, players, worlds, and execute commands\n");
        prompt.append("- SUPER_ADMIN: Has all permissions, including server control, banning players, permission settings\n\n");

        prompt.append("Available tools:\n");
        prompt.append("- check_permission: Check permission\n");
        prompt.append("- restart_server: Restart server (SUPER_ADMIN)\n");
        prompt.append("- stop_server: Stop server (SUPER_ADMIN)\n");
        prompt.append("- hot_reload_plugin: Hot-reload plugin (ADMIN)\n");
        prompt.append("- hot_unload_plugin: Hot-unload plugin (ADMIN)\n");
        prompt.append("- hot_load_plugin: Hot-load plugin (ADMIN)\n");
        prompt.append("- list_plugins: List plugins with descriptions (ADMIN)\n");
        prompt.append("- execute_command: Execute any command (ADMIN)\n");
        prompt.append("- set_game_time: Set game time (ADMIN)\n");
        prompt.append("- set_weather: Set weather (ADMIN)\n");
        prompt.append("- set_game_mode: Set game mode (ADMIN)\n");
        prompt.append("- get_server_info: Get server info (ADMIN)\n");
        prompt.append("- get_player_info: Get player info (ADMIN)\n");
        prompt.append("- teleport_player: Teleport player (ADMIN)\n");
        prompt.append("- give_item: Give item (ADMIN)\n");
        prompt.append("- kick_player: Kick player (ADMIN)\n");
        prompt.append("- ban_player: Ban player (SUPER_ADMIN)\n");
        prompt.append("- set_permission: Set player permission (SUPER_ADMIN)\n");
        prompt.append("- get_online_players: Get online player list\n\n");

        prompt.append("Important: Before any operation, you must call check_permission first. If permission is insufficient, return error directly.\n");
        prompt.append("For complex operations, call tools in steps.\n");

        return prompt.toString();
    }

    private JsonArray buildTools(PermissionManager.PermissionLevel level) {
        JsonArray tools = new JsonArray();

        tools.add(createTool("check_permission", "Check if the executor has sufficient permission",
                createPropsBuilder().add("required_level", "string", "Required permission level: DISABLED, PLAYER, ADMIN, SUPER_ADMIN", true).build()));

        if (level.getLevel() >= PermissionManager.PermissionLevel.SUPER_ADMIN.getLevel()) {
            tools.add(createTool("restart_server", "Restart the server", createPropsBuilder().build()));
            tools.add(createTool("stop_server", "Stop the server", createPropsBuilder().build()));
            tools.add(createTool("ban_player", "Ban a player",
                    createPropsBuilder()
                            .add("player", "string", "Player name", true)
                            .add("reason", "string", "Ban reason", false).build()));
            tools.add(createTool("set_permission", "Set player permission",
                    createPropsBuilder()
                            .add("player", "string", "Player name", true)
                            .add("level", "string", "Permission level: DISABLED, PLAYER, ADMIN, SUPER_ADMIN", true).build()));
        }

        if (level.getLevel() >= PermissionManager.PermissionLevel.ADMIN.getLevel()) {
            tools.add(createTool("hot_reload_plugin", "Hot-reload a plugin",
                    createPropsBuilder().add("plugin_name", "string", "Plugin name", true).build()));
            tools.add(createTool("hot_unload_plugin", "Hot-unload a plugin",
                    createPropsBuilder().add("plugin_name", "string", "Plugin name", true).build()));
            tools.add(createTool("hot_load_plugin", "Hot-load a plugin",
                    createPropsBuilder().add("plugin_name", "string", "Plugin name", true).build()));
            tools.add(createTool("list_plugins", "List all plugins with descriptions", createPropsBuilder().build()));
            tools.add(createTool("execute_command", "Execute a server command",
                    createPropsBuilder().add("command", "string", "Command to execute", true).build()));
            tools.add(createTool("set_game_time", "Set game time",
                    createPropsBuilder().add("time", "string", "Time: day, night, noon, midnight, sunrise, sunset or number", true).build()));
            tools.add(createTool("set_weather", "Set weather",
                    createPropsBuilder().add("weather", "string", "Weather: clear, rain, thunder", true).build()));
            tools.add(createTool("set_game_mode", "Set game mode",
                    createPropsBuilder()
                            .add("player", "string", "Player name", true)
                            .add("game_mode", "string", "Game mode: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR", true).build()));
            tools.add(createTool("get_server_info", "Get server status info", createPropsBuilder().build()));
            tools.add(createTool("get_player_info", "Get player detailed info",
                    createPropsBuilder().add("player", "string", "Player name", true).build()));
            tools.add(createTool("teleport_player", "Teleport a player to another player",
                    createPropsBuilder()
                            .add("target", "string", "Player to teleport", true)
                            .add("destination", "string", "Target player", true).build()));
            tools.add(createTool("give_item", "Give item to player",
                    createPropsBuilder()
                            .add("player", "string", "Player name", true)
                            .add("item", "string", "Item ID", true)
                            .add("amount", "integer", "Amount", false).build()));
            tools.add(createTool("kick_player", "Kick a player",
                    createPropsBuilder()
                            .add("player", "string", "Player name", true)
                            .add("reason", "string", "Kick reason", false).build()));
        }

        tools.add(createTool("get_online_players", "Get current online player list", createPropsBuilder().build()));
        tools.add(createTool("reload_server", "Reload server configuration", createPropsBuilder().build()));

        return tools;
    }

    private JsonObject createTool(String name, String description, JsonObject parameters) {
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");

        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);

        tool.add("function", function);
        return tool;
    }

    static class PropsBuilder {
        private final JsonObject obj;
        private final JsonArray required;

        PropsBuilder() {
            obj = new JsonObject();
            obj.addProperty("type", "object");
            JsonObject properties = new JsonObject();
            obj.add("properties", properties);
            required = new JsonArray();
        }

        PropsBuilder add(String name, String type, String description, boolean isRequired) {
            JsonObject prop = new JsonObject();
            prop.addProperty("type", type);
            prop.addProperty("description", description);
            obj.getAsJsonObject("properties").add(name, prop);
            if (isRequired) {
                required.add(name);
            }
            return this;
        }

        JsonObject build() {
            if (required.size() > 0) {
                obj.add("required", required);
            }
            return obj;
        }
    }

    private PropsBuilder createPropsBuilder() {
        return new PropsBuilder();
    }
}
