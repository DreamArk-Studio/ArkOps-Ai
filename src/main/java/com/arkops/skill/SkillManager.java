package com.arkops.skill;

import com.arkops.ArkOpsAi;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 管理器 - 负责注册、管理和执行所有 Skill
 */
public class SkillManager {

    private final ArkOpsAi plugin;
    private final Map<String, Skill> registeredSkills = new ConcurrentHashMap<>();
    private final Map<String, String> toolToSkillMap = new ConcurrentHashMap<>();

    public SkillManager(ArkOpsAi plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册一个 Skill
     * 
     * @param skill 要注册的 Skill 实例
     * @return 是否注册成功
     */
    public boolean registerSkill(Skill skill) {
        if (skill == null) {
            plugin.getLogger().warning("尝试注册 null Skill");
            return false;
        }

        String id = skill.getId();
        if (id == null || id.isEmpty()) {
            plugin.getLogger().warning("尝试注册 ID 为空的 Skill");
            return false;
        }

        if (registeredSkills.containsKey(id)) {
            plugin.getLogger().warning("Skill 已存在: " + id);
            return false;
        }

        try {
            skill.onEnable(plugin);
            registeredSkills.put(id, skill);

            // 建立工具到 Skill 的映射
            for (JsonObject tool : skill.getTools()) {
                String toolName = tool.getAsJsonObject("function").get("name").getAsString();
                toolToSkillMap.put(toolName, id);
            }

            plugin.getLogger().info("已注册 Skill: " + skill.getName() + " v" + skill.getVersion() + " by " + skill.getAuthor());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("注册 Skill 失败: " + id + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 注销一个 Skill
     * 
     * @param skillId 要注销的 Skill ID
     * @return 是否注销成功
     */
    public boolean unregisterSkill(String skillId) {
        Skill skill = registeredSkills.remove(skillId);
        if (skill == null) {
            return false;
        }

        try {
            skill.onDisable();

            // 移除工具映射
            for (JsonObject tool : skill.getTools()) {
                String toolName = tool.getAsJsonObject("function").get("name").getAsString();
                toolToSkillMap.remove(toolName);
            }

            plugin.getLogger().info("已注销 Skill: " + skill.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("注销 Skill 失败: " + skillId + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取已注册的 Skill
     * 
     * @param skillId Skill ID
     * @return Skill 实例，如果不存在返回 null
     */
    public Skill getSkill(String skillId) {
        return registeredSkills.get(skillId);
    }

    /**
     * 获取所有已注册的 Skill
     * 
     * @return 所有 Skill 的列表
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(registeredSkills.values());
    }

    /**
     * 获取所有可用的工具定义
     * 这些工具会被添加到 AI 的 tool calling 系统中
     * 
     * @return 所有工具定义的 JSON 数组
     */
    public JsonArray getAllTools() {
        JsonArray allTools = new JsonArray();
        for (Skill skill : registeredSkills.values()) {
            if (skill.isAvailable()) {
                for (JsonObject tool : skill.getTools()) {
                    allTools.add(tool);
                }
            }
        }
        return allTools;
    }

    /**
     * 获取所有 Skill 的系统提示词
     * 
     * @return 合并后的系统提示词
     */
    public String getAllSystemPrompts() {
        StringBuilder sb = new StringBuilder();
        for (Skill skill : registeredSkills.values()) {
            if (skill.isAvailable()) {
                String prompt = skill.getSystemPrompt();
                if (prompt != null && !prompt.isEmpty()) {
                    sb.append(prompt).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 执行工具调用
     * 根据工具名称找到对应的 Skill 并执行
     * 
     * @param sender 命令发送者
     * @param toolName 工具名称
     * @param args 工具参数
     * @return 执行结果
     */
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        String skillId = toolToSkillMap.get(toolName);
        if (skillId == null) {
            return "错误: 未找到工具 " + toolName;
        }

        Skill skill = registeredSkills.get(skillId);
        if (skill == null) {
            return "错误: Skill 不存在 " + skillId;
        }

        if (!skill.isAvailable()) {
            return "错误: Skill 不可用 " + skill.getName();
        }

        try {
            return skill.executeTool(sender, toolName, args);
        } catch (Exception e) {
            return "执行工具失败: " + toolName + " - " + e.getMessage();
        }
    }

    /**
     * 检查工具是否存在
     * 
     * @param toolName 工具名称
     * @return 如果工具存在返回 true
     */
    public boolean hasTool(String toolName) {
        return toolToSkillMap.containsKey(toolName);
    }

    /**
     * 获取工具对应的 Skill ID
     * 
     * @param toolName 工具名称
     * @return Skill ID，如果不存在返回 null
     */
    public String getSkillIdForTool(String toolName) {
        return toolToSkillMap.get(toolName);
    }

    /**
     * 从文件夹加载所有 Skill
     * 开发者可以将 Skill 的 jar 文件放在 skills 文件夹中
     * 
     * @param skillsFolder Skill 文件夹路径
     */
    public void loadSkillsFromFolder(String skillsFolder) {
        File folder = new File(skillsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            plugin.getLogger().info("Skill 文件夹不存在: " + skillsFolder);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("Skill 文件夹中没有 jar 文件");
            return;
        }

        plugin.getLogger().info("正在从 " + skillsFolder + " 加载 Skill...");
        int loadedCount = 0;

        for (File file : files) {
            try {
                plugin.getLogger().info("发现 Skill 文件: " + file.getName());

                // 创建 URLClassLoader 加载 jar 文件
                java.net.URL[] urls = new java.net.URL[]{file.toURI().toURL()};
                try (java.net.URLClassLoader classLoader = new java.net.URLClassLoader(
                        urls,
                        this.getClass().getClassLoader())) {

                    // 扫描 jar 中的所有类
                    java.util.jar.JarFile jarFile = new java.util.jar.JarFile(file);
                    java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

                    while (entries.hasMoreElements()) {
                        java.util.jar.JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();

                        // 只处理 .class 文件
                        if (entryName.endsWith(".class")) {
                            // 将路径转换为类名
                            String className = entryName.replace('/', '.').replace('\\', '.').substring(0, entryName.length() - 6);

                            try {
                                // 加载类
                                Class<?> clazz = classLoader.loadClass(className);

                                // 检查是否实现了 Skill 接口
                                if (Skill.class.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isEnum()) {
                                    // 实例化 Skill
                                    Skill skill = (Skill) clazz.getDeclaredConstructor().newInstance();

                                    // 注册 Skill
                                    if (registerSkill(skill)) {
                                        loadedCount++;
                                    }
                                }
                            } catch (ClassNotFoundException | InstantiationException | 
                                   IllegalAccessException | java.lang.reflect.InvocationTargetException | 
                                   NoSuchMethodException e) {
                                // 忽略无法加载的类
                            }
                        }
                    }

                    jarFile.close();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("加载 Skill 失败: " + file.getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("成功加载 " + loadedCount + " 个外部 Skill");
    }

    /**
     * 关闭所有 Skill
     */
    public void shutdown() {
        plugin.getLogger().info("正在关闭所有 Skill...");
        for (Skill skill : registeredSkills.values()) {
            try {
                skill.onDisable();
            } catch (Exception e) {
                plugin.getLogger().severe("关闭 Skill 失败: " + skill.getId() + " - " + e.getMessage());
            }
        }
        registeredSkills.clear();
        toolToSkillMap.clear();
    }

    /**
     * 获取已注册的 Skill 数量
     * 
     * @return Skill 数量
     */
    public int getSkillCount() {
        return registeredSkills.size();
    }

    /**
     * 获取可用的工具数量
     * 
     * @return 工具数量
     */
    public int getToolCount() {
        return toolToSkillMap.size();
    }
}
