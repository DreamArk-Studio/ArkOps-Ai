package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.PermissionManager;
import com.arkops.manager.ServerActionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class OpsCommandHandler {

    private final ArkOpsAi plugin;
    private final PermissionManager permissionManager;
    private final ServerActionManager actionManager;

    public OpsCommandHandler(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.actionManager = plugin.getServerActionManager();
    }

    public void handleCommand(CommandSender sender, String command) {
        UUID playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        String playerName = sender.getName();
        PermissionManager.PermissionLevel level = permissionManager.getPermissionLevel(playerId);

        sender.sendMessage("§eAI 正在分析请求...");

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
            sender.sendMessage("§cAI 已达到最大迭代次数，停止执行");
            return;
        }

        plugin.getOpenAiManager().sendRequestWithMessages(messages, tools).thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (response.has("error")) {
                    sender.sendMessage("§c" + response.get("error").getAsString());
                    return;
                }

                JsonArray choices = response.getAsJsonArray("choices");
                if (choices == null || choices.size() == 0) {
                    sender.sendMessage("§cAI 未返回有效响应");
                    return;
                }

                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");

                if (message.has("tool_calls") && !message.get("tool_calls").isJsonNull()) {
                    JsonArray toolCalls = message.getAsJsonArray("tool_calls");

                    if (iteration == 0) {
                        sender.sendMessage("§e§l===== AI 开始执行 =====");
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
                            sender.sendMessage("§7步骤 " + (iteration + 1) + "." + (i + 1) + ": §f" + toolName);
                            sender.sendMessage("§a结果: " + result);
                        }

                        plugin.getArkOpsLogger().logAction(playerName, "AI 工具调用: " + toolName, result);

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
                        sender.sendMessage("§b§l[ArkOps-Ai] §7" + content);
                        plugin.getArkOpsLogger().logAction(playerName, "AI 问答: " + originalCommand, "成功");
                    } else {
                        sender.sendMessage("§e§l===== 执行完成 =====");
                        sender.sendMessage("§b§l[ArkOps-Ai] §7" + content);
                        plugin.getArkOpsLogger().logAction(playerName, "AI Agent 完成: " + originalCommand, "成功");
                    }
                }
            });
        }).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§cAI 请求失败: " + ex.getMessage());
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
                    return "服务器已重载";
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
                    return "未知工具: " + toolName;
            }
        } catch (Exception e) {
            return "执行失败: " + e.getMessage();
        }
    }

    private String checkPermission(UUID playerId, String playerName, JsonObject args) {
        String requiredLevel = args.get("required_level").getAsString();
        PermissionManager.PermissionLevel required = PermissionManager.PermissionLevel.fromString(requiredLevel);
        PermissionManager.PermissionLevel current = permissionManager.getPermissionLevel(playerId);

        if (current.getLevel() >= required.getLevel()) {
            return "权限检查通过。当前权限: " + current.getDisplayName();
        } else {
            return "权限不足。当前: " + current.getDisplayName() + ", 需要: " + required.getDisplayName();
        }
    }

    private String setPermission(String playerName, String level) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        PermissionManager.PermissionLevel permLevel = PermissionManager.PermissionLevel.fromString(level);
        permissionManager.setPermissionLevel(offlinePlayer.getUniqueId(), playerName, permLevel);
        return "已将 " + playerName + " 的权限设置为: " + permLevel.getDisplayName();
    }

    private String buildSystemPrompt(String playerName, PermissionManager.PermissionLevel level, CommandSender sender) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 ArkOps-Ai，一个 Minecraft Purpur 服务器的 AI 运维助手。\n\n");

        prompt.append("当前请求者: ").append(playerName).append("\n");
        prompt.append("请求者权限等级: ").append(level.getDisplayName()).append("\n\n");

        prompt.append("服务器信息:\n");
        prompt.append("- 版本: ").append(plugin.getServer().getVersion()).append("\n");
        prompt.append("- 在线玩家: ").append(plugin.getServer().getOnlinePlayers().size()).append("\n");
        prompt.append("- 插件数量: ").append(plugin.getServer().getPluginManager().getPlugins().length).append("\n\n");

        prompt.append("工作规则:\n");
        prompt.append("1. 你必须先检查执行者是否有足够的权限执行操作\n");
        prompt.append("2. 对于复杂任务，分步骤执行（例如热卸载插件：先检查权限，再查找插件，最后执行）\n");
        prompt.append("3. 每个操作都要记录日志\n");
        prompt.append("4. 你可以连续多轮调用工具来完成复杂任务\n");
        prompt.append("5. 当所有步骤完成后，返回简洁的中文总结\n");
        prompt.append("6. 如果权限不足，直接返回错误信息，不要继续执行\n\n");

        prompt.append("权限等级说明:\n");
        prompt.append("- DISABLED: 无任何权限\n");
        prompt.append("- PLAYER: 只能问答游戏问题\n");
        prompt.append("- ADMIN: 可执行插件管理、玩家管理、世界管理、命令执行\n");
        prompt.append("- SUPER_ADMIN: 拥有所有权限，包括服务器开关、封禁玩家、权限设置\n\n");

        prompt.append("可用工具:\n");
        prompt.append("- check_permission: 检查权限\n");
        prompt.append("- restart_server: 重启服务器 (SUPER_ADMIN)\n");
        prompt.append("- stop_server: 关闭服务器 (SUPER_ADMIN)\n");
        prompt.append("- hot_reload_plugin: 热重载插件 (ADMIN)\n");
        prompt.append("- hot_unload_plugin: 热卸载插件 (ADMIN)\n");
        prompt.append("- hot_load_plugin: 热加载插件 (ADMIN)\n");
        prompt.append("- list_plugins: 列出插件及功能 (ADMIN)\n");
        prompt.append("- execute_command: 执行任意命令 (ADMIN)\n");
        prompt.append("- set_game_time: 设置游戏时间 (ADMIN)\n");
        prompt.append("- set_weather: 设置天气 (ADMIN)\n");
        prompt.append("- set_game_mode: 设置游戏模式 (ADMIN)\n");
        prompt.append("- get_server_info: 获取服务器信息 (ADMIN)\n");
        prompt.append("- get_player_info: 获取玩家信息 (ADMIN)\n");
        prompt.append("- teleport_player: 传送玩家 (ADMIN)\n");
        prompt.append("- give_item: 给予物品 (ADMIN)\n");
        prompt.append("- kick_player: 踢出玩家 (ADMIN)\n");
        prompt.append("- ban_player: 封禁玩家 (SUPER_ADMIN)\n");
        prompt.append("- set_permission: 设置玩家权限 (SUPER_ADMIN)\n");
        prompt.append("- get_online_players: 获取在线玩家列表\n\n");

        prompt.append("重要: 在执行任何操作前，必须先调用 check_permission 检查权限。如果权限不足，直接返回错误信息，不要继续执行。\n");
        prompt.append("对于复杂操作，请分步骤调用工具。\n");

        return prompt.toString();
    }

    private JsonArray buildTools(PermissionManager.PermissionLevel level) {
        JsonArray tools = new JsonArray();

        tools.add(createTool("check_permission", "检查执行者是否有足够权限",
                createPropsBuilder().add("required_level", "string", "需要的权限等级: DISABLED, PLAYER, ADMIN, SUPER_ADMIN", true).build()));

        if (level.getLevel() >= PermissionManager.PermissionLevel.SUPER_ADMIN.getLevel()) {
            tools.add(createTool("restart_server", "重启服务器", createPropsBuilder().build()));
            tools.add(createTool("stop_server", "关闭服务器", createPropsBuilder().build()));
            tools.add(createTool("ban_player", "封禁玩家",
                    createPropsBuilder()
                            .add("player", "string", "玩家名称", true)
                            .add("reason", "string", "封禁原因", false).build()));
            tools.add(createTool("set_permission", "设置玩家权限",
                    createPropsBuilder()
                            .add("player", "string", "玩家名称", true)
                            .add("level", "string", "权限等级: DISABLED, PLAYER, ADMIN, SUPER_ADMIN", true).build()));
        }

        if (level.getLevel() >= PermissionManager.PermissionLevel.ADMIN.getLevel()) {
            tools.add(createTool("hot_reload_plugin", "热重载插件",
                    createPropsBuilder().add("plugin_name", "string", "插件名称", true).build()));
            tools.add(createTool("hot_unload_plugin", "热卸载插件",
                    createPropsBuilder().add("plugin_name", "string", "插件名称", true).build()));
            tools.add(createTool("hot_load_plugin", "热加载插件",
                    createPropsBuilder().add("plugin_name", "string", "插件名称", true).build()));
            tools.add(createTool("list_plugins", "列出所有插件及功能介绍", createPropsBuilder().build()));
            tools.add(createTool("execute_command", "执行服务器命令",
                    createPropsBuilder().add("command", "string", "要执行的命令", true).build()));
            tools.add(createTool("set_game_time", "设置游戏时间",
                    createPropsBuilder().add("time", "string", "时间: day, night, noon, midnight, sunrise, sunset 或数字", true).build()));
            tools.add(createTool("set_weather", "设置天气",
                    createPropsBuilder().add("weather", "string", "天气: clear, rain, thunder", true).build()));
            tools.add(createTool("set_game_mode", "设置游戏模式",
                    createPropsBuilder()
                            .add("player", "string", "玩家名称", true)
                            .add("game_mode", "string", "游戏模式: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR", true).build()));
            tools.add(createTool("get_server_info", "获取服务器状态信息", createPropsBuilder().build()));
            tools.add(createTool("get_player_info", "获取玩家详细信息",
                    createPropsBuilder().add("player", "string", "玩家名称", true).build()));
            tools.add(createTool("teleport_player", "传送玩家到另一个玩家",
                    createPropsBuilder()
                            .add("target", "string", "要被传送的玩家", true)
                            .add("destination", "string", "目标玩家", true).build()));
            tools.add(createTool("give_item", "给予玩家物品",
                    createPropsBuilder()
                            .add("player", "string", "玩家名称", true)
                            .add("item", "string", "物品 ID", true)
                            .add("amount", "integer", "数量", false).build()));
            tools.add(createTool("kick_player", "踢出玩家",
                    createPropsBuilder()
                            .add("player", "string", "玩家名称", true)
                            .add("reason", "string", "踢出原因", false).build()));
        }

        tools.add(createTool("get_online_players", "获取当前在线玩家列表", createPropsBuilder().build()));
        tools.add(createTool("reload_server", "重载服务器配置", createPropsBuilder().build()));

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
