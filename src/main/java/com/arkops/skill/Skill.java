package com.arkops.skill;

import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Skill 接口 - 开发者实现此接口来扩展 ArkOps-Ai 的能力
 * 
 * 每个 Skill 代表一组特定的功能，可以被 AI 调用执行操作
 * 
 * 重要说明：
 * - Skill ≠ Bukkit Plugin，不能当作 Plugin 使用
 * - 注册事件时必须使用主插件实例：Bukkit.getPluginManager().registerEvents(this, mainPlugin)
 * - 可以通过 onEnable(JavaPlugin plugin) 获取主插件实例
 */
public interface Skill {

    /**
     * 获取 Skill 的唯一标识符
     * 例如: "economy", "world_edit", "custom_commands"
     * 
     * @return Skill ID
     */
    String getId();

    /**
     * 获取 Skill 的名称（人类可读）
     * 例如: "Economy Management", "World Edit Tools"
     * 
     * @return Skill 名称
     */
    String getName();

    /**
     * 获取 Skill 的描述
     * 这个描述会被发送给 AI，帮助 AI 理解何时使用这个 Skill
     * 
     * @return Skill 描述
     */
    String getDescription();

    /**
     * 获取 Skill 的版本
     * 
     * @return 版本号
     */
    String getVersion();

    /**
     * 获取 Skill 的作者
     * 
     * @return 作者名称
     */
    String getAuthor();

    /**
     * 获取此 Skill 提供的所有工具定义
     * 这些工具会被注册到 AI 的 tool calling 系统中
     * 
     * @return 工具定义列表（JSON 格式）
     */
    List<JsonObject> getTools();

    /**
     * 执行工具调用
     * 当 AI 决定调用某个工具时，此方法会被执行
     * 
     * @param sender 命令发送者
     * @param toolName 工具名称
     * @param args 工具参数
     * @return 执行结果（字符串）
     */
    String executeTool(CommandSender sender, String toolName, JsonObject args);

    /**
     * 获取此 Skill 的系统提示词
     * 这些提示词会被添加到 AI 的系统提示中，帮助 AI 理解如何使用这个 Skill
     * 
     * @return 系统提示词
     */
    String getSystemPrompt();

    /**
     * 检查此 Skill 是否已正确初始化并可用
     * 
     * @return 如果 Skill 可用返回 true
     */
    boolean isAvailable();

    /**
     * 当 Skill 被启用时调用
     * 可以在此处进行初始化操作
     * 
     * @param mainPlugin ArkOps-Ai 主插件实例
     *                   注册事件时必须使用此实例：
     *                   Bukkit.getPluginManager().registerEvents(this, mainPlugin)
     */
    void onEnable(JavaPlugin mainPlugin);

    /**
     * 当 Skill 被禁用时调用
     * 可以在此处进行清理操作
     */
    void onDisable();
}
