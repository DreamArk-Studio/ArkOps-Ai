package com.example.myskill;

import com.arkops.skill.Skill;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 模板 Skill - 展示如何正确实现 ArkOps-Ai Skill
 * 
 * 重要说明：
 * - Skill 不是 Bukkit Plugin，不能当作 Plugin 使用
 * - 注册事件时必须使用 mainPlugin：Bukkit.getPluginManager().registerEvents(this, mainPlugin)
 * - 必须有无参构造函数
 */
public class MySkill implements Skill, Listener {

    private JavaPlugin mainPlugin;

    @Override
    public String getId() {
        return "my_skill";
    }

    @Override
    public String getName() {
        return "My Skill Template";
    }

    @Override
    public String getDescription() {
        return "A template Skill demonstrating correct implementation patterns. " +
               "Invoke when user asks for a demonstration or wants to test the Skill system.";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Your Name";
    }

    @Override
    public List<JsonObject> getTools() {
        List<JsonObject> tools = new ArrayList<>();

        JsonObject greetTool = new JsonObject();
        JsonObject greetFunction = new JsonObject();
        greetFunction.addProperty("name", "greet_player");
        greetFunction.addProperty("description", "Send a greeting message to a player.");

        JsonObject greetProps = new JsonObject();
        JsonObject playerNameProp = new JsonObject();
        playerNameProp.addProperty("type", "string");
        playerNameProp.addProperty("description", "The name of the player to greet");
        greetProps.add("player_name", playerNameProp);

        greetFunction.add("parameters", greetProps);
        greetTool.add("function", greetFunction);
        greetTool.addProperty("type", "function");

        tools.add(greetTool);

        JsonObject adminTool = new JsonObject();
        JsonObject adminFunction = new JsonObject();
        adminFunction.addProperty("name", "admin_operation");
        adminFunction.addProperty("description", "Perform an administrative operation (requires ADMIN permission).");

        JsonObject adminProps = new JsonObject();
        JsonObject actionProp = new JsonObject();
        actionProp.addProperty("type", "string");
        actionProp.addProperty("description", "The admin action to perform");
        adminProps.add("action", actionProp);

        adminFunction.add("parameters", adminProps);
        adminTool.add("function", adminFunction);
        adminTool.addProperty("type", "function");

        tools.add(adminTool);
        return tools;
    }

    @Override
    public String getToolPermissionLevel(String toolName) {
        switch (toolName) {
            case "greet_player":
                return "PLAYER";
            case "admin_operation":
                return "ADMIN";
            default:
                return "ADMIN";
        }
    }

    @Override
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        switch (toolName) {
            case "greet_player":
                String playerName = args.get("player_name").getAsString();
                return greetPlayer(playerName);
            case "admin_operation":
                String action = args.get("action").getAsString();
                return performAdminAction(sender, action);
            default:
                return "Unknown tool: " + toolName;
        }
    }

    @Override
    public String getSystemPrompt() {
        return "=== My Skill Template ===\n" +
               "You have the following tools:\n" +
               "- greet_player: Send a greeting message to a player (available to all players)\n" +
               "- admin_operation: Perform an administrative operation (requires ADMIN permission)\n\n" +
               "Use greet_player when players ask for a demonstration or want to be greeted.\n" +
               "Use admin_operation only when an admin requests administrative actions.\n";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        this.mainPlugin = mainPlugin;
        
        mainPlugin.getLogger().info("[MySkill] Initializing MySkill...");
        
        // 正确：使用 mainPlugin 注册事件
        Bukkit.getPluginManager().registerEvents(this, mainPlugin);
        
        mainPlugin.getLogger().info("[MySkill] MySkill enabled successfully!");
    }

    @Override
    public void onDisable() {
        mainPlugin.getLogger().info("[MySkill] MySkill disabled.");
    }

    private String greetPlayer(String playerName) {
        return "Hello, " + playerName + "! Welcome to the server!";
    }

    private String performAdminAction(CommandSender sender, String action) {
        return "Admin action performed: " + action + " by " + sender.getName();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        mainPlugin.getLogger().info("[MySkill] Player joined: " + event.getPlayer().getName());
    }
}
