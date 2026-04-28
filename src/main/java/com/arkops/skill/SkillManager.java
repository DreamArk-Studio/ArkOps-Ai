package com.arkops.skill;

import com.arkops.ArkOpsAi;
import com.arkops.session.AISessionContext;
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
    private final Map<String, String> skillToFileMap = new ConcurrentHashMap<>();

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

            // 移除文件映射
            skillToFileMap.remove(skillId);

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
     * 根据权限级别获取可用的工具定义
     * 只返回调用者有权限使用的工具
     * 
     * @param requiredLevel 调用者的权限级别
     * @return 过滤后的工具定义 JSON 数组
     */
    public JsonArray getToolsForPermissionLevel(String requiredLevel) {
        JsonArray allTools = new JsonArray();
        int callerLevel = getPermissionLevelValue(requiredLevel);
        
        for (Skill skill : registeredSkills.values()) {
            if (skill.isAvailable()) {
                for (JsonObject tool : skill.getTools()) {
                    String toolName = tool.getAsJsonObject("function").get("name").getAsString();
                    String toolRequiredLevel = skill.getToolPermissionLevel(toolName);
                    int toolLevel = getPermissionLevelValue(toolRequiredLevel);
                    
                    if (callerLevel >= toolLevel) {
                        allTools.add(tool);
                    }
                }
            }
        }
        return allTools;
    }

    /**
     * 获取权限级别的数值
     * 
     * @param level 权限级别字符串
     * @return 权限级别数值
     */
    private int getPermissionLevelValue(String level) {
        switch (level.toUpperCase()) {
            case "PLAYER":
                return 1;
            case "ADMIN":
                return 2;
            case "SUPER_ADMIN":
                return 3;
            case "CONSOLE":
                return 4;
            default:
                return 0;
        }
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
     * 检查调用者是否有权限使用指定工具
     * 
     * @param toolName 工具名称
     * @param callerLevel 调用者的权限级别
     * @return 如果有权限返回 true
     */
    public boolean hasToolPermission(String toolName, String callerLevel) {
        String skillId = toolToSkillMap.get(toolName);
        if (skillId == null) {
            return false;
        }

        Skill skill = registeredSkills.get(skillId);
        if (skill == null || !skill.isAvailable()) {
            return false;
        }

        int callerLevelValue = getPermissionLevelValue(callerLevel);
        int toolLevelValue = getPermissionLevelValue(skill.getToolPermissionLevel(toolName));
        
        return callerLevelValue >= toolLevelValue;
    }

    /**
     * 执行工具调用
     * 根据工具名称找到对应的 Skill 并执行
     * 
     * @param sender 命令发送者
     * @param toolName 工具名称
     * @param args 工具参数
     * @param callerLevel 调用者的权限级别
     * @return 执行结果
     */
    public String executeTool(CommandSender sender, String toolName, JsonObject args, String callerLevel) {
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

        int callerLevelValue = getPermissionLevelValue(callerLevel);
        int toolLevelValue = getPermissionLevelValue(skill.getToolPermissionLevel(toolName));
        
        if (callerLevelValue < toolLevelValue) {
            return "错误: 权限不足，无法使用工具 " + toolName + " (需要: " + skill.getToolPermissionLevel(toolName) + ")";
        }

        try {
            return skill.executeTool(sender, toolName, args);
        } catch (Exception e) {
            return "执行工具失败: " + toolName + " - " + e.getMessage();
        }
    }

    /**
     * 执行工具调用（基于会话上下文）
     * 使用 AISessionContext 进行权限控制
     * 
     * @param sender 命令发送者
     * @param toolName 工具名称
     * @param args 工具参数
     * @param context AI 会话上下文
     * @return 执行结果
     */
    public String executeTool(CommandSender sender, String toolName, JsonObject args, AISessionContext context) {
        if (context == null) {
            return "错误: 缺少会话上下文，无法执行工具 " + toolName;
        }

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

        if (!hasSufficientPermission(toolName, context.getPermissionLevel())) {
            return "权限不足：你的权限等级为 " + context.getPermissionLevel()
                    + "，该操作需要 " + skill.getToolPermissionLevel(toolName) + " 权限。";
        }

        try {
            return skill.executeTool(sender, toolName, args);
        } catch (Exception e) {
            return "执行工具失败: " + toolName + " - " + e.getMessage();
        }
    }

    /**
     * 检查调用者是否有足够权限使用指定工具
     * 
     * @param toolName 工具名称
     * @param callerLevel 调用者的权限级别字符串
     * @return 如果有足够权限返回 true
     */
    public boolean hasSufficientPermission(String toolName, String callerLevel) {
        return hasToolPermission(toolName, callerLevel);
    }

    /**
     * 根据权限级别过滤可用工具列表
     * 只返回调用者有权限使用的工具
     * 
     * @param permissionLevel 调用者的权限级别
     * @return 过滤后的工具定义 JSON 数组
     */
    public JsonArray filterToolsByPermission(String permissionLevel) {
        return getToolsForPermissionLevel(permissionLevel);
    }

    /**
     * 执行工具调用（旧版本，向后兼容）
     * 
     * @deprecated 请使用 executeTool(sender, toolName, args, context)
     */
    @Deprecated
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        return executeTool(sender, toolName, args, "ADMIN");
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
                                        skillToFileMap.put(skill.getId(), file.getAbsolutePath());
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
     * 自定义 ClassLoader，用于隔离 Skill 的类加载
     * 这样可以支持热重载时重新加载类
     */
    private static class IsolatedClassLoader extends java.net.URLClassLoader {
        static {
            // 注册为并行可加载的 ClassLoader
            registerAsParallelCapable();
        }

        public IsolatedClassLoader(java.net.URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // 检查类是否已经加载
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                // 如果是 Skill 接口或 Bukkit API，使用父 ClassLoader
                if (name.startsWith("com.arkops.skill.") || 
                    name.startsWith("org.bukkit.") || 
                    name.startsWith("com.google.gson.")) {
                    return super.loadClass(name, resolve);
                }
                
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name, resolve);
                }
            }
            
            if (resolve) {
                resolveClass(loadedClass);
            }
            
            return loadedClass;
        }
    }

    /**
     * 热重载指定 Skill
     * 
     * @param skillId Skill ID
     * @return 操作结果
     */
    public String reloadSkill(String skillId) {
        Skill skill = registeredSkills.get(skillId);
        if (skill == null) {
            return "错误: 未找到 Skill '" + skillId + "'";
        }

        String filePath = skillToFileMap.get(skillId);
        if (filePath == null) {
            return "错误: Skill '" + skillId + "' 不是从文件加载的，无法热重载";
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return "错误: Skill 文件不存在: " + filePath;
        }

        // 保存文件路径，因为 unregisterSkill 会删除它
        String savedFilePath = filePath;

        try {
            // 先注销旧 Skill
            unregisterSkill(skillId);
            plugin.getLogger().info("正在热重载 Skill: " + skillId + " 从 " + file.getName());

            // 使用自定义的 ClassLoader 来隔离类加载
            java.net.URL[] urls = new java.net.URL[]{file.toURI().toURL()};
            IsolatedClassLoader classLoader = new IsolatedClassLoader(
                    urls,
                    this.getClass().getClassLoader());

            java.util.jar.JarFile jarFile = new java.util.jar.JarFile(file);
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').replace('\\', '.').substring(0, entryName.length() - 6);

                    try {
                        Class<?> clazz = classLoader.loadClass(className);

                        if (Skill.class.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isEnum()) {
                            Skill newSkill = (Skill) clazz.getDeclaredConstructor().newInstance();

                            if (registerSkill(newSkill)) {
                                skillToFileMap.put(newSkill.getId(), savedFilePath);
                                jarFile.close();
                                classLoader.close();
                                return "Skill '" + newSkill.getName() + "' v" + newSkill.getVersion() + " 已热重载成功";
                            }
                        }
                    } catch (ClassNotFoundException | InstantiationException |
                           IllegalAccessException | java.lang.reflect.InvocationTargetException |
                           NoSuchMethodException e) {
                        // 忽略非 Skill 类
                    }
                }
            }

            jarFile.close();
            classLoader.close();

            // 如果重新注册失败，尝试恢复文件路径映射
            skillToFileMap.put(skillId, savedFilePath);
            return "错误: 未能在文件中找到 Skill '" + skillId + "' 的类";
        } catch (Exception e) {
            plugin.getLogger().severe("热重载 Skill 失败: " + skillId + " - " + e.getMessage());
            e.printStackTrace();
            // 恢复文件路径映射
            skillToFileMap.put(skillId, savedFilePath);
            return "热重载失败: " + e.getMessage();
        }
    }

    /**
     * 热加载所有新放入的 Skill 文件
     * 扫描 skills 文件夹，加载之前未被加载过的 Skill jar 文件
     * 
     * @param skillsFolder Skill 文件夹路径
     * @return 操作结果
     */
    public String loadNewSkills(String skillsFolder) {
        File folder = new File(skillsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            return "错误: Skill 文件夹不存在: " + skillsFolder;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            return "Skill 文件夹中没有 jar 文件";
        }

        // 获取已加载的文件路径集合
        java.util.Set<String> loadedFiles = new java.util.HashSet<>(skillToFileMap.values());

        int loadedCount = 0;
        int skippedCount = 0;
        StringBuilder result = new StringBuilder();

        for (File file : files) {
            String absolutePath = file.getAbsolutePath();
            
            // 跳过已经加载过的文件
            if (loadedFiles.contains(absolutePath)) {
                skippedCount++;
                continue;
            }

            try {
                plugin.getLogger().info("发现新 Skill 文件: " + file.getName());

                java.net.URL[] urls = new java.net.URL[]{file.toURI().toURL()};
                IsolatedClassLoader classLoader = new IsolatedClassLoader(
                        urls,
                        this.getClass().getClassLoader());

                java.util.jar.JarFile jarFile = new java.util.jar.JarFile(file);
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

                boolean found = false;
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    if (entryName.endsWith(".class")) {
                        String className = entryName.replace('/', '.').replace('\\', '.').substring(0, entryName.length() - 6);

                        try {
                            Class<?> clazz = classLoader.loadClass(className);

                            if (Skill.class.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isEnum()) {
                                Skill skill = (Skill) clazz.getDeclaredConstructor().newInstance();

                                if (registerSkill(skill)) {
                                    skillToFileMap.put(skill.getId(), absolutePath);
                                    loadedCount++;
                                    result.append("- 成功加载: ").append(skill.getName()).append(" v").append(skill.getVersion()).append("\n");
                                    found = true;
                                    break;
                                }
                            }
                        } catch (ClassNotFoundException | InstantiationException |
                               IllegalAccessException | java.lang.reflect.InvocationTargetException |
                               NoSuchMethodException e) {
                            // 忽略非 Skill 类
                        }
                    }
                }

                if (!found) {
                    classLoader.close();
                    result.append("- 跳过: ").append(file.getName()).append(" (未找到 Skill 类)\n");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("加载 Skill 失败: " + file.getName() + " - " + e.getMessage());
                result.append("- 失败: ").append(file.getName()).append(" (").append(e.getMessage()).append(")\n");
            }
        }

        if (loadedCount == 0 && skippedCount == files.length) {
            return "没有新的 Skill 文件需要加载（所有 " + skippedCount + " 个文件已加载）";
        }

        return "加载完成: 新加载 " + loadedCount + " 个, 跳过 " + skippedCount + " 个\n" + result.toString().trim();
    }

    /**
     * 热重载所有从文件加载的 Skill
     * 
     * @return 操作结果
     */
    public String reloadAllSkills() {
        List<String> skillsToReload = new ArrayList<>(skillToFileMap.keySet());
        if (skillsToReload.isEmpty()) {
            return "没有可热重载的 Skill（所有 Skill 均为内置）";
        }

        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int failCount = 0;

        for (String skillId : skillsToReload) {
            String res = reloadSkill(skillId);
            if (res.contains("成功")) {
                successCount++;
            } else {
                failCount++;
            }
            result.append("- ").append(skillId).append(": ").append(res).append("\n");
        }

        return "热重载完成: 成功 " + successCount + " 个, 失败 " + failCount + " 个\n" + result.toString().trim();
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
        skillToFileMap.clear();
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
