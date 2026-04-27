# ArkOps-Ai Skill 开发文档

> **文档版本**: v2.0 | **最后更新**: 2026-04-27 | **适用版本**: ArkOps-Ai 2.0.0+

---

## 目录

1. [概述](#1-概述)
2. [架构设计](#2-架构设计)
3. [快速开始](#3-快速开始)
4. [核心概念：Skill ≠ Plugin](#4-核心概念skill--plugin)
5. [会话级权限上下文 (AISessionContext)](#5-会话级权限上下文-aisessioncontext)
6. [Skill 接口详解](#6-skill-接口详解)
7. [工具定义与实现](#7-工具定义与实现)
8. [系统提示词编写指南](#8-系统提示词编写指南)
9. [完整示例：经济管理系统](#9-完整示例经济管理系统)
10. [完整示例：世界编辑工具](#10-完整示例世界编辑工具)
11. [最佳实践](#11-最佳实践)
12. [常见问题 (FAQ)](#12-常见问题-faq)
13. [版本兼容性](#13-版本兼容性)
14. [API 参考](#14-api-参考)

---

## 1. 概述

### 1.1 什么是 Skill？

Skill 是 ArkOps-Ai 的模块化扩展系统，允许开发者为 AI 添加自定义能力。每个 Skill 是一组相关功能的集合，通过定义工具（Tools）让 AI 能够调用这些功能。

### 1.2 设计目标

- **模块化**：每个 Skill 独立开发、独立部署、互不干扰
- **动态注册**：运行时注册，支持热插拔
- **AI 友好**：自动集成到 AI 的工具调用系统
- **权限控制**：继承 ArkOps-Ai 的四级权限系统
- **易于开发**：只需实现一个接口即可

### 1.3 使用场景

| 场景 | 说明 |
|------|------|
| 经济插件集成 | 连接 Vault 等经济插件，提供余额查询、转账等功能 |
| 世界编辑 | 添加方块操作、区域填充、实体生成等功能 |
| 自定义命令 | 实现服务器特定的业务逻辑 |
| 外部 API 连接 | 集成 Discord、Webhook、数据库等外部服务 |
| 数据分析 | 提供服务器统计、玩家行为分析等功能 |

---

## 2. 架构设计

### 2.1 系统架构图

```
┌────────────────────────────────────────────────────────────┐
│                    ArkOps-Ai 核心系统                        │
│                                                            │
│  ┌──────────────────────────────────────────────────┐     │
│  │              AISessionContext                     │     │
│  │  - 用户身份 (QQ / 玩家 / 控制台)                  │     │
│  │  - 权限级别 (PLAYER/ADMIN/SUPER_ADMIN/CONSOLE)   │     │
│  │  - 不可变对象，全链路传递                         │     │
│  └────────────────────┬─────────────────────────────┘     │
│                       │                                    │
│  ┌────────────────────▼─────────────────────────────┐     │
│  │              OpsCommandHandler                    │     │
│  │  - 构建工具列表 (filterToolsByPermission)         │     │
│  │  - 执行工具调用 (携带 AISessionContext)           │     │
│  │  - 管理系统提示词                                 │     │
│  │  - QQ 消息入口                                    │     │
│  └────────────────────┬─────────────────────────────┘     │
│                       │                                    │
│  ┌────────────────────▼─────────────────────────────┐     │
│  │                SkillManager                       │     │
│  │  - 注册/注销 Skill                                │     │
│  │  - 工具映射管理                                   │     │
│  │  - 权限校验 (hasSufficientPermission)             │     │
│  │  - 双重权限校验                                   │     │
│  └────────────────────┬─────────────────────────────┘     │
│                       │                                    │
│  ┌────────────────────▼─────────────────────────────┐     │
│  │                 Skill 实例                         │     │
│  │  - EconomySkill                                   │     │
│  │  - KnowledgeBaseSkill                             │     │
│  │  - YourCustomSkill                                │     │
│  └──────────────────────────────────────────────────┘     │
└────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件 | 职责 | 关键方法 |
|------|------|----------|
| `AISessionContext` | 会话级权限上下文，携带用户身份和权限级别 | `console()`, `player()`, `qqUser()` |
| `Skill` 接口 | 定义 Skill 的标准接口 | `getTools()`, `executeTool()`, `getSystemPrompt()` |
| `SkillManager` | 管理所有 Skill 的生命周期和权限校验 | `registerSkill()`, `executeTool()` |
| `OpsCommandHandler` | 集成 Skill 到 AI 系统，处理消息入口 | `executeAgentLoop()`, `buildTools()` |

### 2.3 调用链路

```
用户输入 (命令/聊天)
    │
    ▼
┌─────────────────────┐
│  创建 AISessionContext │  ← 识别用户身份和权限
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  构建工具列表        │  ← 根据权限过滤可用工具
│  (权限过滤)          │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  AI Agent 循环       │  ← 递归处理，支持多步工具调用
│  executeAgentLoop    │
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌───────┐   ┌──────────┐
│ AI 响应│   │ 工具调用  │
└───────┘   └────┬─────┘
                 │
                 ▼
          ┌──────────────┐
          │ SkillManager  │  ← 双重权限校验
          │ executeTool() │
          └──────┬───────┘
                 │
                 ▼
          ┌──────────────┐
          │ Skill 实例    │  ← 执行具体业务逻辑
          │ executeTool() │
          └──────────────┘
```

---

## 3. 快速开始

### 3.1 创建项目

在你的 Maven 项目中添加依赖：

#### 方式一：使用 GitHub Packages（推荐）

1. **配置仓库和认证**

在你的 `pom.xml` 中添加 GitHub Packages 仓库：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DreamArk-Studio/ArkOps-Ai</url>
    </repository>
</repositories>
```

2. **添加依赖**

```xml
<dependencies>
    <!-- ArkOps-Ai 核心 -->
    <dependency>
        <groupId>com.arkops</groupId>
        <artifactId>ArkOps-Ai</artifactId>
        <version>2.2.1</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Bukkit API -->
    <dependency>
        <groupId>org.purpurmc.purpur</groupId>
        <artifactId>purpur-api</artifactId>
        <version>1.21.11-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

3. **配置本地认证**

在 `~/.m2/settings.xml` 中添加 GitHub 认证：

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的 GitHub 用户名</username>
            <password>你的 GitHub Personal Access Token</password>
        </server>
    </servers>
</settings>
```

> **提示**：GitHub Personal Access Token 需要 `read:packages` 权限。
> 创建方法：GitHub Settings → Developer settings → Personal access tokens → Generate new token

#### 方式二：从源码构建

如果你需要修改 ArkOps-Ai 源码或使用最新版本：

```bash
git clone https://github.com/DreamArk-Studio/ArkOps-Ai-CN.git
cd ArkOps-Ai-CN
mvn clean install -DskipTests
```

然后在你的项目中使用相同的依赖配置（版本号保持一致）。

### 3.2 实现 Skill 接口

```java
package com.example.myskill;

import com.arkops.skill.Skill;
import com.google.gson.JsonObject;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.ArrayList;

public class MyFirstSkill implements Skill {
    
    @Override
    public String getId() {
        return "my_first_skill";
    }
    
    @Override
    public String getName() {
        return "My First Skill";
    }
    
    @Override
    public String getDescription() {
        return "This is my first Skill for ArkOps-Ai. " +
               "Invoke when user asks for a simple demonstration.";
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
        // 添加工具定义
        return tools;
    }
    
    @Override
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        // 执行工具逻辑
        return "Hello from MyFirstSkill!";
    }
    
    @Override
    public String getSystemPrompt() {
        return "=== My First Skill ===\n" +
               "You have a simple demonstration tool.\n";
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        // 初始化逻辑
    }
    
    @Override
    public void onDisable() {
        // 清理逻辑
    }
}
```

### 3.3 注册 Skill

在你的插件主类中注册：

```java
// 获取 SkillManager
SkillManager skillManager = ArkOpsAi.getInstance().getSkillManager();

// 注册你的 Skill
skillManager.registerSkill(new MyFirstSkill());
```

### 3.4 编译并测试

```bash
mvn clean package
```

将生成的 jar 文件放入服务器 `plugins/ArkOps-Ai/skills/` 目录，重启服务器。

---

## 4. 核心概念：Skill ≠ Plugin

### 4.1 核心差异

**这是开发 Skill 时最重要的概念**。Skill 不是独立的 Bukkit Plugin，而是 ArkOps-Ai 的内部组件。

| 特性 | Bukkit Plugin | ArkOps-Ai Skill |
|------|---------------|-----------------|
| 继承 | `JavaPlugin` | `Skill` 接口 |
| 生命周期管理 | Bukkit | SkillManager |
| 事件注册 | `registerEvents(this, this)` | `registerEvents(this, mainPlugin)` |
| 日志记录 | `getLogger()` | `mainPlugin.getLogger()` |
| 数据目录 | `getDataFolder()` | `mainPlugin.getDataFolder()` |
| 配置文件 | 自行管理 | 使用主插件数据目录 |

### 4.2 常见错误

```java
// ❌ 错误：Skill 不是 Plugin，不能使用 this
Bukkit.getPluginManager().registerEvents(this, this);

// ❌ 错误：Skill 没有 getLogger() 方法
getLogger().info("message");

// ❌ 错误：Skill 没有 getDataFolder() 方法
new File(getDataFolder(), "config.yml");
```

### 4.3 正确做法

```java
public class MySkill implements Skill, Listener {
    
    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        // ✅ 正确：使用 mainPlugin 注册事件
        Bukkit.getPluginManager().registerEvents(this, mainPlugin);
        
        // ✅ 正确：使用 mainPlugin 记录日志
        mainPlugin.getLogger().info("MySkill enabled");
        
        // ✅ 正确：使用 mainPlugin 获取数据目录
        File config = new File(mainPlugin.getDataFolder(), "skills/myconfig.yml");
    }
}
```

### 4.4 设计原因

Bukkit 的设计是 **一个 jar = 一个 Plugin**。Skill 是 ArkOps-Ai 内部的模块/组件，不是独立的 Bukkit 插件。所有 Skill 共享 ArkOps-Ai 的 Plugin 实例，这样可以：

- 简化部署流程（只需一个主插件）
- 统一管理生命周期
- 共享配置和日志系统
- 降低资源占用

---

## 5. 会话级权限上下文 (AISessionContext)

### 5.1 什么是 AISessionContext？

`AISessionContext` 是一个**不可变、线程安全**的会话上下文对象，用于在 AI 调用链路中携带用户身份和权限信息。

**解决的核心问题**：

| 问题 | 解决方案 |
|------|----------|
| 身份识别 | 区分请求来源是 QQ 用户、游戏内玩家还是控制台 |
| 权限隔离 | AI 调用工具时使用真实用户的权限，而非默认的 CONSOLE 权限 |
| 防止越权 | 每个工具调用都经过双重权限校验 |

### 5.2 类定义

```java
package com.arkops.session;

public final class AISessionContext {
    // 私有字段
    private final String qqUserId;          // QQ用户ID (null = 非QQ用户)
    private final String permissionLevel;   // 权限级别: PLAYER, ADMIN, SUPER_ADMIN, CONSOLE
    private final String displayName;       // 显示名称

    // 工厂方法
    public static AISessionContext console()                        // 控制台
    public static AISessionContext player(String name, String level) // 游戏内玩家
    public static AISessionContext qqUser(String qqId, String name, String level) // QQ用户

    // Getter 方法
    public String getQqUserId()
    public String getPermissionLevel()
    public String getDisplayName()
    public boolean isQQUser()
}
```

### 5.3 使用场景

#### 场景 1: QQ 机器人入口（异步广播模式）

```java
// QQ 机器人收到消息时调用，回复广播到游戏内
public void handleQQMessage(String qqUserId, String command, String permissionLevel) {
    // 创建 QQ 用户的会话上下文
    AISessionContext sessionContext = AISessionContext.qqUser(
        qqUserId,
        "QQ:" + qqUserId,
        permissionLevel   // 来自权限系统的真实权限级别
    );

    // 构建工具列表时使用 QQ 用户的权限过滤
    JsonArray tools = buildTools(PermissionLevel.fromString(permissionLevel));

    // 全链路传递 context
    executeAgentLoop(sender, playerName, null, command, messages, tools, 0, false, sessionContext);
}
```

#### 场景 2: QQ 机器人入口（同步返回模式，推荐）

```java
// QQ 机器人收到消息时调用，直接返回 AI 回复
public String handleQQMessageWithResponse(String qqUserId, String message, String permissionLevel) {
    AISessionContext sessionContext = AISessionContext.qqUser(
        qqUserId,
        "QQ:" + qqUserId,
        permissionLevel
    );

    JsonArray tools = buildTools(PermissionLevel.fromString(permissionLevel));

    // 构建 messages...

    // 同步执行，直接返回 AI 回复
    return executeAgentLoopWithResponse(playerName, null, message, messages, tools, 0, sessionContext);
}
```

> **推荐使用 `handleQQMessageWithResponse`**：不依赖日志文件读取，直接获取 AI 回复，100% 准确，性能更好。

#### 场景 3: 游戏内玩家

```java
public void handleCommand(CommandSender sender, String command) {
    PermissionLevel level = permissionManager.getPermissionLevel(playerId);

    // 创建玩家会话上下文
    AISessionContext sessionContext = (sender instanceof Player)
            ? AISessionContext.player(playerName, level.name())
            : AISessionContext.console();

    executeAgentLoop(sender, playerName, playerId, command, messages, tools, 0, false, sessionContext);
}
```

### 5.4 双重权限校验

系统在两条路径上同时进行权限控制，确保安全：

| 校验层 | 方法 | 位置 | 作用 |
|--------|------|------|------|
| ① 工具列表过滤 | `SkillManager.filterToolsByPermission(level)` | `buildTools()` | AI 只能看到用户有权使用的工具 |
| ② 执行时校验 | `SkillManager.executeTool(context)` | 工具执行入口 | 即使绕过①，执行时也会再次检查 |

```java
// SkillManager 内部
public String executeTool(CommandSender sender, String toolName, JsonObject args, AISessionContext context) {
    if (context == null) {
        return "错误: 缺少会话上下文，无法执行工具";
    }

    // ② 执行时权限校验
    if (!hasSufficientPermission(toolName, context.getPermissionLevel())) {
        return "权限不足：你的权限等级为 " + context.getPermissionLevel()
                + "，该操作需要 " + skill.getToolPermissionLevel(toolName) + " 权限。";
    }

    return skill.executeTool(sender, toolName, args);
}
```

### 5.5 Skill 开发者视角

Skill 开发者**不需要**直接操作 `AISessionContext`。只需正确实现 `getToolPermissionLevel()` 方法，框架会自动处理权限校验：

```java
@Override
public String getToolPermissionLevel(String toolName) {
    switch (toolName) {
        case "query_knowledge":
            return "PLAYER";       // 所有用户可查询
        case "upload_knowledge":
            return "ADMIN";        // 仅管理员可上传
        case "delete_knowledge":
            return "SUPER_ADMIN";  // 仅超级管理员可删除
        default:
            return "ADMIN";
    }
}
```

### 5.6 安全原则

1. **禁止信任 AI 传入的权限参数** — 权限始终来自服务器系统（QQ用户级别/游戏内权限）
2. **禁止绕过 executeTool 校验** — context 为空时立即拒绝
3. **禁止通过字符串拼接执行命令** — 所有操作经过权限校验
4. **不使用 ThreadLocal** — `AISessionContext` 作为方法参数显式传递

### 5.7 原生工具权限校验

`OpsCommandHandler.executeToolCall()` 中每个原生工具调用前都通过 `requirePermission()` 进行校验，形成**第三层权限控制**：

```java
private void requirePermission(PermissionManager.PermissionLevel callerLevel, 
                               PermissionManager.PermissionLevel requiredLevel, 
                               String toolName) {
    if (callerLevel.getLevel() < requiredLevel.getLevel()) {
        throw new SecurityException("权限不足：你的权限等级为 " + callerLevel.getDisplayName()
                + "，该操作需要 " + requiredLevel.getDisplayName() + " 权限。");
    }
}
```

**权限映射表**：

| 工具 | 需要权限 | 工具 | 需要权限 |
|------|----------|------|----------|
| `restart_server` | SUPER_ADMIN | `get_server_info` | ADMIN |
| `stop_server` | SUPER_ADMIN | `get_player_info` | ADMIN |
| `reload_server` | SUPER_ADMIN | `teleport_player` | ADMIN |
| `ban_player` | SUPER_ADMIN | `give_item` | ADMIN |
| `set_permission` | SUPER_ADMIN | `kick_player` | ADMIN |
| `hot_reload_plugin` | ADMIN | `get_player_held_item` | ADMIN |
| `hot_unload_plugin` | ADMIN | `get_player_biome` | ADMIN |
| `hot_load_plugin` | ADMIN | `get_player_looking_at` | ADMIN |
| `list_plugins` | ADMIN | `get_player_detailed_info` | ADMIN |
| `execute_command` | ADMIN | `get_online_players` | 无限制 |
| `set_game_time` | ADMIN | `check_permission` | 无限制 |
| `set_weather` | ADMIN | | |
| `set_game_mode` | ADMIN | | |

---

## 6. Skill 接口详解

### 6.1 接口方法总览

| 方法 | 返回类型 | 必填 | 说明 |
|------|----------|------|------|
| `getId()` | String | 是 | Skill 唯一标识符 |
| `getName()` | String | 是 | Skill 人类可读名称 |
| `getDescription()` | String | 是 | Skill 功能描述 |
| `getVersion()` | String | 是 | Skill 版本号 |
| `getAuthor()` | String | 是 | Skill 作者 |
| `getTools()` | List\<JsonObject\> | 是 | 工具定义列表 |
| `executeTool()` | String | 是 | 工具执行逻辑 |
| `getSystemPrompt()` | String | 是 | AI 系统提示词 |
| `isAvailable()` | boolean | 是 | Skill 可用性检查 |
| `onEnable(JavaPlugin)` | void | 是 | 启用时调用，接收主插件实例 |
| `onDisable()` | void | 是 | 禁用时调用 |

### 6.2 方法详细说明

#### getId()

返回 Skill 的唯一标识符。必须是全局唯一的字符串。

```java
@Override
public String getId() {
    return "economy_manager";  // 推荐：小写字母+下划线
}
```

**命名规范**：
- 使用小写字母
- 单词间用下划线分隔
- 避免特殊字符
- 保持简洁明了

#### getDescription()

描述 Skill 的功能和使用场景。这个描述会被发送给 AI。

```java
@Override
public String getDescription() {
    return "Provides economy management tools including balance checking, " +
           "money transfer, deposit and withdrawal. " +
           "Use these tools when players ask about their balance, " +
           "want to transfer money, or need economy-related operations.";
}
```

**编写要点**：
- 说明 Skill 提供什么功能
- 说明何时应该使用这些工具
- 使用清晰的英文描述
- 控制在 200 字符以内（用于工具列表显示）

#### getTools()

返回此 Skill 提供的所有工具定义。每个工具定义是一个 JSON 对象。

```java
@Override
public List<JsonObject> getTools() {
    List<JsonObject> tools = new ArrayList<>();
    
    tools.add(createTool(
        "get_balance",
        "Get a player's current balance",
        createPropsBuilder()
            .add("player", "string", "Player name", true)
            .build()
    ));
    
    return tools;
}
```

#### executeTool()

执行 AI 调用的工具。根据工具名称执行相应的逻辑。

```java
@Override
public String executeTool(CommandSender sender, String toolName, JsonObject args) {
    switch (toolName) {
        case "get_balance":
            String playerName = args.get("player").getAsString();
            return getBalance(playerName);
        case "transfer_money":
            String from = args.get("from").getAsString();
            String to = args.get("to").getAsString();
            double amount = args.get("amount").getAsDouble();
            return transferMoney(from, to, amount);
        default:
            return "Unknown tool: " + toolName;
    }
}
```

#### getSystemPrompt()

返回 AI 系统提示词，告诉 AI 如何使用你的工具。

```java
@Override
public String getSystemPrompt() {
    return "=== Economy Skill ===\n" +
           "You have access to economy management tools:\n" +
           "- get_balance: Check a player's current balance\n" +
           "- transfer_money: Transfer money between players\n\n" +
           "Use these tools when players ask about money or balance.\n" +
           "Always verify player names exist before performing operations.";
}
```

#### isAvailable()

检查 Skill 是否可用。可用于检查依赖是否满足。

```java
@Override
public boolean isAvailable() {
    // 检查 Vault 是否安装
    return Bukkit.getPluginManager().getPlugin("Vault") != null;
}
```

#### onEnable(JavaPlugin mainPlugin)

Skill 启用时调用。接收 ArkOps-Ai 的主插件实例。

```java
@Override
public void onEnable(JavaPlugin mainPlugin) {
    // 注册事件监听器
    Bukkit.getPluginManager().registerEvents(this, mainPlugin);
    
    // 记录日志
    mainPlugin.getLogger().info("EconomySkill enabled");
    
    // 加载配置
    loadConfig(mainPlugin.getDataFolder());
}
```

#### onDisable()

Skill 禁用时调用。用于清理资源。

```java
@Override
public void onDisable() {
    // 清理数据
    cache.clear();
    
    // 保存状态
    saveState();
}
```

---

## 7. 工具定义与实现

### 7.1 工具定义结构

每个工具定义是一个 JSON 对象，符合 OpenAI Function Calling 规范：

```json
{
  "type": "function",
  "function": {
    "name": "tool_name",
    "description": "Tool description for AI",
    "parameters": {
      "type": "object",
      "properties": {
        "param1": {
          "type": "string",
          "description": "Parameter description"
        },
        "param2": {
          "type": "number",
          "description": "Another parameter"
        }
      },
      "required": ["param1"]
    }
  }
}
```

### 7.2 参数类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `string` | 字符串 | 玩家名称、消息 |
| `number` | 数字 | 数量、坐标 |
| `boolean` | 布尔值 | true/false |
| `array` | 数组 | 玩家列表 |
| `object` | 对象 | 复杂数据结构 |

### 7.3 创建工具的辅助类

使用 `PropsBuilder` 简化参数定义：

```java
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
```

### 7.4 工具定义示例

#### 简单工具（无参数）

```java
tools.add(createTool(
    "get_server_time",
    "Get the current server time",
    createPropsBuilder().build()
));
```

#### 单参数工具

```java
tools.add(createTool(
    "get_player_balance",
    "Get a player's balance",
    createPropsBuilder()
        .add("player", "string", "Player name to check", true)
        .build()
));
```

#### 多参数工具

```java
tools.add(createTool(
    "transfer_money",
    "Transfer money between players",
    createPropsBuilder()
        .add("from", "string", "Source player", true)
        .add("to", "string", "Target player", true)
        .add("amount", "number", "Amount to transfer", true)
        .add("message", "string", "Optional transfer message", false)
        .build()
));
```

### 7.5 工具权限设置

为每个工具设置所需的最低权限级别：

```java
@Override
public String getToolPermissionLevel(String toolName) {
    switch (toolName) {
        case "get_balance":
            return "PLAYER";       // 所有用户可查询
        case "transfer_money":
            return "ADMIN";        // 仅管理员可转账
        case "set_balance":
            return "SUPER_ADMIN";  // 仅超级管理员可设置
        default:
            return "ADMIN";
    }
}
```

---

## 8. 系统提示词编写指南

### 8.1 目的

系统提示词告诉 AI：
1. 有哪些工具可用
2. 每个工具的作用
3. 何时使用这些工具
4. 使用时的注意事项

### 8.2 结构模板

```
=== [Skill Name] ===
You have access to [category] tools:
- tool1: Description
- tool2: Description
- tool3: Description

Use these tools when [trigger conditions].
Always [best practices].
Never [restrictions].
```

### 8.3 示例：经济系统

```
=== Economy Skill ===
You have access to economy management tools:
- get_balance: Check a player's current balance
- transfer_money: Transfer money from one player to another
- deposit_money: Add money to a player's account
- withdraw_money: Remove money from a player's account
- set_balance: Set a player's balance to a specific amount

Use these tools when players ask about money, balance, economy, or want to transfer funds.
Always verify player names exist before performing operations.
Never allow negative amounts or transfers that would result in negative balances.
Always confirm large transfers with the user before executing.
```

### 8.4 示例：世界编辑

```
=== World Edit Skill ===
You have access to world manipulation tools:
- set_block: Set a block at specific coordinates
- fill_area: Fill a rectangular area with a block
- spawn_entity: Spawn an entity at a location
- change_weather: Change the weather in a world

Use these tools when players ask to modify the world, place blocks, or change environment.
Always verify coordinates are within world bounds.
Never allow operations that could crash the server.
Always warn users about potentially lag-causing operations.
```

### 8.5 最佳实践

1. **清晰明了**：使用简洁的英文描述
2. **列出所有工具**：确保 AI 知道所有可用工具
3. **说明使用场景**：告诉 AI 何时使用工具
4. **添加限制条件**：说明不能做什么
5. **提供最佳实践**：指导 AI 正确使用工具

---

## 9. 完整示例：经济管理系统

### 9.1 创建 Skill 类

```java
package com.arkops.skill.example;

import com.arkops.skill.Skill;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 示例 Skill: 经济管理系统
 * 
 * 提供经济相关的工具：查询余额、转账、存款、取款
 */
public class EconomySkill implements Skill {

    // 模拟经济数据（实际开发中应该连接真实的经济插件如 Vault）
    private final Map<String, Double> playerBalances = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "economy";
    }

    @Override
    public String getName() {
        return "Economy Management";
    }

    @Override
    public String getDescription() {
        return "Provides economy management tools including balance checking, money transfer, deposit and withdrawal. " +
               "Use these tools when players ask about their balance, want to transfer money, or need economy-related operations.";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "ArkOps Team";
    }

    @Override
    public List<JsonObject> getTools() {
        List<JsonObject> tools = new ArrayList<>();

        // 工具 1: 查询余额
        tools.add(createTool(
                "get_balance",
                "Get a player's current balance",
                createPropsBuilder()
                        .add("player", "string", "Player name to check balance for", true)
                        .build()
        ));

        // 工具 2: 转账
        tools.add(createTool(
                "transfer_money",
                "Transfer money from one player to another",
                createPropsBuilder()
                        .add("from", "string", "Player name to transfer from", true)
                        .add("to", "string", "Player name to transfer to", true)
                        .add("amount", "number", "Amount of money to transfer", true)
                        .build()
        ));

        // 工具 3: 存款
        tools.add(createTool(
                "deposit_money",
                "Deposit money to a player's account",
                createPropsBuilder()
                        .add("player", "string", "Player name to deposit to", true)
                        .add("amount", "number", "Amount of money to deposit", true)
                        .build()
        ));

        // 工具 4: 取款
        tools.add(createTool(
                "withdraw_money",
                "Withdraw money from a player's account",
                createPropsBuilder()
                        .add("player", "string", "Player name to withdraw from", true)
                        .add("amount", "number", "Amount of money to withdraw", true)
                        .build()
        ));

        // 工具 5: 设置余额
        tools.add(createTool(
                "set_balance",
                "Set a player's balance to a specific amount",
                createPropsBuilder()
                        .add("player", "string", "Player name to set balance for", true)
                        .add("amount", "number", "New balance amount", true)
                        .build()
        ));

        return tools;
    }

    @Override
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        try {
            switch (toolName) {
                case "get_balance":
                    return getBalance(args.get("player").getAsString());
                case "transfer_money":
                    return transferMoney(
                            args.get("from").getAsString(),
                            args.get("to").getAsString(),
                            args.get("amount").getAsDouble()
                    );
                case "deposit_money":
                    return depositMoney(
                            args.get("player").getAsString(),
                            args.get("amount").getAsDouble()
                    );
                case "withdraw_money":
                    return withdrawMoney(
                            args.get("player").getAsString(),
                            args.get("amount").getAsDouble()
                    );
                case "set_balance":
                    return setBalance(
                            args.get("player").getAsString(),
                            args.get("amount").getAsDouble()
                    );
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }

    @Override
    public String getSystemPrompt() {
        return "=== Economy Skill ===\n" +
               "You have access to economy management tools:\n" +
               "- get_balance: Check a player's current balance\n" +
               "- transfer_money: Transfer money between players\n" +
               "- deposit_money: Add money to a player's account\n" +
               "- withdraw_money: Remove money from a player's account\n" +
               "- set_balance: Set a player's balance to a specific amount\n\n" +
               "Use these tools when players ask about money, balance, economy, or want to transfer funds.\n" +
               "Always verify player names exist before performing operations.\n" +
               "Never allow negative amounts or transfers that would result in negative balances.";
    }

    @Override
    public String getToolPermissionLevel(String toolName) {
        switch (toolName) {
            case "get_balance":
                return "PLAYER";
            case "transfer_money":
            case "deposit_money":
            case "withdraw_money":
                return "ADMIN";
            case "set_balance":
                return "SUPER_ADMIN";
            default:
                return "ADMIN";
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        // 初始化一些默认余额
        playerBalances.put("Steve", 1000.0);
        playerBalances.put("Alex", 500.0);
    }

    @Override
    public void onDisable() {
        // 清理数据
        playerBalances.clear();
    }

    // ========== 经济操作实现 ==========

    private String getBalance(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        double balance = playerBalances.getOrDefault(playerName, 0.0);
        return playerName + "'s current balance: $" + String.format("%.2f", balance);
    }

    private String transferMoney(String from, String to, double amount) {
        if (amount <= 0) {
            return "Transfer amount must be greater than 0";
        }

        Player fromPlayer = Bukkit.getPlayerExact(from);
        Player toPlayer = Bukkit.getPlayerExact(to);

        if (fromPlayer == null) {
            return "Player not found: " + from;
        }
        if (toPlayer == null) {
            return "Player not found: " + to;
        }

        double fromBalance = playerBalances.getOrDefault(from, 0.0);
        if (fromBalance < amount) {
            return from + " has insufficient funds. Current balance: $" + String.format("%.2f", fromBalance);
        }

        playerBalances.put(from, fromBalance - amount);
        double toBalance = playerBalances.getOrDefault(to, 0.0);
        playerBalances.put(to, toBalance + amount);

        return "Successfully transferred: $" + String.format("%.2f", amount) + " from " + from + " to " + to;
    }

    private String depositMoney(String playerName, double amount) {
        if (amount <= 0) {
            return "Deposit amount must be greater than 0";
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        double balance = playerBalances.getOrDefault(playerName, 0.0);
        playerBalances.put(playerName, balance + amount);

        return "Successfully deposited: $" + String.format("%.2f", amount) + " to " + playerName + "'s account";
    }

    private String withdrawMoney(String playerName, double amount) {
        if (amount <= 0) {
            return "Withdrawal amount must be greater than 0";
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        double balance = playerBalances.getOrDefault(playerName, 0.0);
        if (balance < amount) {
            return playerName + " has insufficient funds. Current balance: $" + String.format("%.2f", balance);
        }

        playerBalances.put(playerName, balance - amount);
        return "Successfully withdrew: $" + String.format("%.2f", amount) + " from " + playerName + "'s account";
    }

    private String setBalance(String playerName, double amount) {
        if (amount < 0) {
            return "Balance cannot be negative";
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        playerBalances.put(playerName, amount);
        return "Set " + playerName + "'s balance to: $" + String.format("%.2f", amount);
    }

    // ========== 工具创建辅助方法 ==========

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
```

### 9.2 注册 Skill

```java
// 在插件主类的 onEnable() 中
SkillManager skillManager = new SkillManager(this);
skillManager.registerSkill(new EconomySkill());
```

### 9.3 使用示例

玩家在游戏中说：
```
@ops 查看 Steve 的余额
```

AI 会调用 `get_balance` 工具，返回：
```
Steve's current balance: $1000.00
```

玩家说：
```
@ops 从 Steve 转账 100 给 Alex
```

AI 会调用 `transfer_money` 工具，返回：
```
Successfully transferred: $100.00 from Steve to Alex
```

---

## 10. 完整示例：世界编辑工具

### 10.1 创建 WorldEditSkill

```java
package com.arkops.skill.example;

import com.arkops.skill.Skill;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例 Skill: 世界编辑工具
 * 
 * 提供世界编辑相关的功能：放置方块、填充区域、生成实体
 */
public class WorldEditSkill implements Skill {

    @Override
    public String getId() {
        return "world_edit";
    }

    @Override
    public String getName() {
        return "World Edit Tools";
    }

    @Override
    public String getDescription() {
        return "Provides world manipulation tools including placing blocks, " +
               "filling areas, and spawning entities. " +
               "Use when players ask to modify the world, build structures, " +
               "or change the environment.";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "ArkOps Team";
    }

    @Override
    public List<JsonObject> getTools() {
        List<JsonObject> tools = new ArrayList<>();

        // 工具 1: 放置方块
        tools.add(createTool(
                "set_block",
                "Place a block at specific coordinates",
                createPropsBuilder()
                        .add("world", "string", "World name", true)
                        .add("x", "number", "X coordinate", true)
                        .add("y", "number", "Y coordinate", true)
                        .add("z", "number", "Z coordinate", true)
                        .add("block_type", "string", "Block material name (e.g., STONE, GRASS_BLOCK)", true)
                        .build()
        ));

        // 工具 2: 填充区域
        tools.add(createTool(
                "fill_area",
                "Fill a rectangular area with a block type",
                createPropsBuilder()
                        .add("world", "string", "World name", true)
                        .add("x1", "number", "First corner X", true)
                        .add("y1", "number", "First corner Y", true)
                        .add("z1", "number", "First corner Z", true)
                        .add("x2", "number", "Second corner X", true)
                        .add("y2", "number", "Second corner Y", true)
                        .add("z2", "number", "Second corner Z", true)
                        .add("block_type", "string", "Block material name", true)
                        .build()
        ));

        // 工具 3: 获取玩家位置
        tools.add(createTool(
                "get_player_location",
                "Get a player's current location",
                createPropsBuilder()
                        .add("player", "string", "Player name", true)
                        .build()
        ));

        // 工具 4: 传送玩家
        tools.add(createTool(
                "teleport_to_coordinates",
                "Teleport a player to specific coordinates",
                createPropsBuilder()
                        .add("player", "string", "Player name", true)
                        .add("world", "string", "World name", true)
                        .add("x", "number", "X coordinate", true)
                        .add("y", "number", "Y coordinate", true)
                        .add("z", "number", "Z coordinate", true)
                        .build()
        ));

        return tools;
    }

    @Override
    public String executeTool(CommandSender sender, String toolName, JsonObject args) {
        try {
            switch (toolName) {
                case "set_block":
                    return setBlock(
                            args.get("world").getAsString(),
                            args.get("x").getAsInt(),
                            args.get("y").getAsInt(),
                            args.get("z").getAsInt(),
                            args.get("block_type").getAsString()
                    );
                case "fill_area":
                    return fillArea(
                            args.get("world").getAsString(),
                            args.get("x1").getAsInt(),
                            args.get("y1").getAsInt(),
                            args.get("z1").getAsInt(),
                            args.get("x2").getAsInt(),
                            args.get("y2").getAsInt(),
                            args.get("z2").getAsInt(),
                            args.get("block_type").getAsString()
                    );
                case "get_player_location":
                    return getPlayerLocation(args.get("player").getAsString());
                case "teleport_to_coordinates":
                    return teleportToCoordinates(
                            args.get("player").getAsString(),
                            args.get("world").getAsString(),
                            args.get("x").getAsDouble(),
                            args.get("y").getAsDouble(),
                            args.get("z").getAsDouble()
                    );
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }

    @Override
    public String getSystemPrompt() {
        return "=== World Edit Skill ===\n" +
               "You have access to world manipulation tools:\n" +
               "- set_block: Place a block at specific coordinates\n" +
               "- fill_area: Fill a rectangular area with a block type\n" +
               "- get_player_location: Get a player's current location\n" +
               "- teleport_to_coordinates: Teleport a player to specific coordinates\n\n" +
               "Use these tools when players ask to modify the world, place blocks, or change environment.\n" +
               "Always verify coordinates are within world bounds.\n" +
               "Never allow operations that could crash the server.";
    }

    @Override
    public String getToolPermissionLevel(String toolName) {
        switch (toolName) {
            case "get_player_location":
                return "PLAYER";
            case "set_block":
            case "fill_area":
            case "teleport_to_coordinates":
                return "ADMIN";
            default:
                return "ADMIN";
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        mainPlugin.getLogger().info("WorldEditSkill enabled");
    }

    @Override
    public void onDisable() {
        // 清理逻辑
    }

    // ========== 世界编辑操作实现 ==========

    private String setBlock(String worldName, int x, int y, int z, String blockType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return "World not found: " + worldName;
        }

        try {
            Material material = Material.valueOf(blockType.toUpperCase());
            Location location = new Location(world, x, y, z);
            Block block = world.getBlockAt(location);
            block.setType(material);
            return "Successfully placed " + blockType + " at (" + x + ", " + y + ", " + z + ") in " + worldName;
        } catch (IllegalArgumentException e) {
            return "Invalid block type: " + blockType;
        }
    }

    private String fillArea(String worldName, int x1, int y1, int z1, 
                           int x2, int y2, int z2, String blockType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return "World not found: " + worldName;
        }

        try {
            Material material = Material.valueOf(blockType.toUpperCase());
            
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            int blockCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        world.getBlockAt(x, y, z).setType(material);
                        blockCount++;
                    }
                }
            }

            return "Successfully filled " + blockCount + " blocks with " + blockType + 
                   " in " + worldName + " from (" + x1 + "," + y1 + "," + z1 + 
                   ") to (" + x2 + "," + y2 + "," + z2 + ")";
        } catch (IllegalArgumentException e) {
            return "Invalid block type: " + blockType;
        }
    }

    private String getPlayerLocation(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        Location loc = player.getLocation();
        return playerName + " is at (" + 
               String.format("%.1f", loc.getX()) + ", " + 
               String.format("%.1f", loc.getY()) + ", " + 
               String.format("%.1f", loc.getZ()) + ") in world " + 
               loc.getWorld().getName();
    }

    private String teleportToCoordinates(String playerName, String worldName, 
                                        double x, double y, double z) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return "World not found: " + worldName;
        }

        Location location = new Location(world, x, y, z);
        player.teleport(location);
        return "Successfully teleported " + playerName + " to (" + 
               String.format("%.1f", x) + ", " + 
               String.format("%.1f", y) + ", " + 
               String.format("%.1f", z) + ") in " + worldName;
    }

    // ========== 工具创建辅助方法 ==========

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
```

---

## 11. 最佳实践

### 11.1 代码组织

```
com.example.myskill/
├── MySkill.java              # Skill 主类
├── tools/                    # 工具实现
│   ├── Tool1.java
│   └── Tool2.java
├── util/                     # 工具类
│   └── PropsBuilder.java
└── config/                   # 配置相关
    └── SkillConfig.java
```

### 11.2 错误处理

```java
@Override
public String executeTool(CommandSender sender, String toolName, JsonObject args) {
    try {
        // 参数验证
        validateArgs(args);
        
        // 执行逻辑
        return executeLogic(toolName, args);
    } catch (MissingArgumentException e) {
        return "Missing required parameter: " + e.getParameterName();
    } catch (InvalidValueException e) {
        return "Invalid value for parameter: " + e.getParameterName();
    } catch (PermissionException e) {
        return "Permission denied: " + e.getMessage();
    } catch (Exception e) {
        // 记录详细错误日志
        mainPlugin.getLogger().severe("Error executing tool " + toolName + ": " + e.getMessage());
        e.printStackTrace();
        return "Internal error occurred. Please contact an administrator.";
    }
}
```

### 11.3 性能优化

1. **异步操作**：耗时操作使用 Bukkit 的异步任务
2. **缓存数据**：频繁查询的数据使用缓存
3. **批量处理**：批量操作减少 API 调用次数
4. **资源释放**：在 `onDisable()` 中释放所有资源

```java
// 异步执行示例
Bukkit.getScheduler().runTaskAsynchronously(mainPlugin, () -> {
    // 耗时操作
    String result = performHeavyOperation(args);
    
    // 返回结果（需要在主线程）
    Bukkit.getScheduler().runTask(mainPlugin, () -> {
        // 处理结果
    });
});
```

### 11.4 安全建议

1. **始终验证输入**：不要信任 AI 传入的参数
2. **权限校验**：正确实现 `getToolPermissionLevel()`
3. **限制操作范围**：防止恶意操作（如超大区域填充）
4. **日志记录**：记录所有重要操作
5. **异常处理**：优雅处理错误，不暴露敏感信息

### 11.5 测试建议

1. **单元测试**：测试工具逻辑
2. **集成测试**：测试与 ArkOps-Ai 的集成
3. **权限测试**：验证各权限级别的访问控制
4. **边界测试**：测试极端情况和错误输入

---

## 12. 常见问题 (FAQ)

### Q1: Skill 和 Plugin 有什么区别？

**A**: Skill 是 ArkOps-Ai 的内部组件，不是独立的 Bukkit Plugin。Skill 共享 ArkOps-Ai 的 Plugin 实例，生命周期由 SkillManager 管理。

### Q2: 如何注册事件监听器？

**A**: 使用 `mainPlugin` 注册：
```java
Bukkit.getPluginManager().registerEvents(this, mainPlugin);
```

### Q3: 如何记录日志？

**A**: 使用 `mainPlugin.getLogger()`：
```java
mainPlugin.getLogger().info("My message");
```

### Q4: 如何获取配置文件路径？

**A**: 使用 `mainPlugin.getDataFolder()`：
```java
File config = new File(mainPlugin.getDataFolder(), "skills/myconfig.yml");
```

### Q5: 如何检查 Skill 是否可用？

**A**: 实现 `isAvailable()` 方法：
```java
@Override
public boolean isAvailable() {
    return Bukkit.getPluginManager().getPlugin("Vault") != null;
}
```

### Q6: 工具调用失败怎么办？

**A**: 返回错误信息字符串，AI 会向用户展示：
```java
return "Error: Player not found";
```

### Q7: 如何设置工具权限？

**A**: 实现 `getToolPermissionLevel()` 方法：
```java
@Override
public String getToolPermissionLevel(String toolName) {
    return "ADMIN";  // 返回 PLAYER, ADMIN, SUPER_ADMIN
}
```

### Q8: Skill 支持热重载吗？

**A**: 支持。将新的 jar 放入 `plugins/ArkOps-Ai/skills/` 目录，使用 `/ops reload` 命令重载。

---

## 13. 版本兼容性

### 13.1 API 版本历史

| ArkOps-Ai 版本 | Skill 接口版本 | onEnable 签名 | 发布日期 |
|----------------|----------------|---------------|----------|
| 2.0.0+ | v2 | `onEnable(JavaPlugin mainPlugin)` | 2026-04-24 |
| 1.x.x | v1 | `onEnable()` | 2026-04-23 |

### 13.2 如何升级 Skill 到新版接口

如果你的 Skill 是为旧版 ArkOps-Ai 开发的，需要修改 `onEnable` 方法：

```java
// 旧版（v1）
@Override
public void onEnable() {
    // 旧代码
}

// 新版（v2）
@Override
public void onEnable(JavaPlugin mainPlugin) {
    // 保存 mainPlugin 供后续使用
    this.mainPlugin = mainPlugin;
    
    // 使用 mainPlugin 替换原来的插件引用
    Bukkit.getPluginManager().registerEvents(this, mainPlugin);
    mainPlugin.getLogger().info("Skill enabled");
}
```

### 13.3 检查 Skill 兼容性

在编译 Skill 时，确保：
1. `pom.xml` 中的 ArkOps-Ai 版本与服务器一致
2. 实现了所有接口方法（IDE 会提示缺失的方法）
3. 使用 `mvn clean package` 重新编译

---

## 14. API 参考

### 14.1 Skill 接口

```java
public interface Skill {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    String getAuthor();
    List<JsonObject> getTools();
    String executeTool(CommandSender sender, String toolName, JsonObject args);
    String getSystemPrompt();
    String getToolPermissionLevel(String toolName);  // 可选，默认 ADMIN
    boolean isAvailable();
    void onEnable(JavaPlugin mainPlugin);
    void onDisable();
}
```

### 14.2 SkillManager 类

```java
public class SkillManager {
    // 注册 Skill
    public boolean registerSkill(Skill skill);
    
    // 注销 Skill
    public boolean unregisterSkill(String skillId);
    
    // 获取 Skill
    public Skill getSkill(String skillId);
    
    // 获取所有 Skill
    public List<Skill> getAllSkills();
    
    // 执行工具（携带权限校验）
    public String executeTool(CommandSender sender, String toolName, 
                             JsonObject args, AISessionContext context);
    
    // 根据权限过滤工具
    public JsonArray filterToolsByPermission(String permissionLevel);
    
    // 检查权限
    public boolean hasSufficientPermission(String toolName, String permissionLevel);
}
```

### 14.3 AISessionContext 类

```java
public final class AISessionContext {
    // 工厂方法
    public static AISessionContext console();
    public static AISessionContext player(String name, String level);
    public static AISessionContext qqUser(String qqId, String name, String level);
    
    // Getter
    public String getQqUserId();
    public String getPermissionLevel();
    public String getDisplayName();
    public boolean isQQUser();
}
```

---

## 总结

通过 Skill 系统，你可以为 ArkOps-Ai 添加无限扩展的能力。只需实现 `Skill` 接口，定义工具和系统提示词，就能让 AI 使用你的功能。

### 关键步骤回顾

1. **实现 Skill 接口**：提供基本信息、工具定义、执行逻辑
2. **定义工具**：使用 JSON 定义工具名称、描述和参数
3. **编写系统提示**：告诉 AI 如何使用你的工具
4. **设置权限**：为每个工具设置合适的权限级别
5. **注册 Skill**：通过 SkillManager 注册你的 Skill
6. **测试验证**：在游戏中测试功能是否正常

### 下一步

- 查看示例代码：`src/main/java/com/arkops/skill/example/`
- 参考接口定义：`src/main/java/com/arkops/skill/Skill.java`
- 查看管理器实现：`src/main/java/com/arkops/manager/SkillManager.java`

**祝你开发愉快！** 如有问题，请参考本文档或联系开发团队。