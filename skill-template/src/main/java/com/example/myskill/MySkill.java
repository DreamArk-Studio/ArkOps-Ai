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
        return tools;
    }

    @Override
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        switch (toolName) {
            case "greet_player":
                String playerName = args.get("player_name").getAsString();
                return greetPlayer(playerName);
            default:
                return "Unknown tool: " + toolName;
        }
    }

    @Override
    public String getSystemPrompt() {
        return "=== My Skill Template ===\n" +
               "You have a simple greeting tool:\n" +
               "- greet_player: Send a greeting message to a player\n\n" +
               "Use this tool when players ask for a demonstration or want to be greeted.\n";
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        mainPlugin.getLogger().info("[MySkill] Player joined: " + event.getPlayer().getName());
    }
}
