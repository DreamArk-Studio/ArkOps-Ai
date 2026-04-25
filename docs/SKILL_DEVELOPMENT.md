# ArkOps-Ai Skill 开发文档

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [快速开始](#快速开始)
4. [⚠️ 重要：Skill ≠ Plugin](#重要skill--plugin)
5. [会话级权限上下文 (AISessionContext)](#会话级权限上下文-aisessioncontext)
6. [Skill 接口详解](#skill-接口详解)
7. [工具定义详解](#工具定义详解)
8. [系统提示词编写指南](#系统提示词编写指南)
9. [完整示例：经济管理系统](#完整示例经济管理系统)
10. [完整示例：世界编辑工具](#完整示例世界编辑工具)
11. [最佳实践](#最佳实践)
12. [常见问题](#常见问题)
13. [版本兼容性](#版本兼容性)

---

## 概述

### 什么是 Skill？

Skill 是 ArkOps-Ai 的扩展系统，允许开发者为 AI 添加新的能力。每个 Skill 是一组相关功能的集合，通过定义工具（Tools）让 AI 能够调用这些功能。

### Skill 的优势

- **模块化设计**：每个 Skill 独立开发、独立部署
- **动态注册**：运行时注册，支持热插拔
- **AI 友好**：自动集成到 AI 的工具调用系统
- **权限控制**：继承 ArkOps-Ai 的权限系统
- **易于开发**：只需实现一个接口

### 使用场景

- 集成经济插件（如 Vault）
- 添加世界编辑功能
- 实现自定义命令系统
- 连接外部 API（如 Discord、Webhook）
- 添加数据分析功能

---

## 架构设计

```
┌──────────────────────────────────────────────────┐
│              ArkOps-Ai 核心系统                    │
│  ┌────────────────────────────────────────┐     │
│  │        AISessionContext                 │     │
│  │  - 用户身份 (QQ / 玩家 / 控制台)        │     │
│  │  - 权限级别                             │     │
│  │  - 不可变对象，全链路传递                │     │
│  └────────────────┬───────────────────────┘     │
│                   │                              │
│  ┌────────────────▼──────────────────────┐     │
│  │        OpsCommandHandler              │     │
│  │  - 构建工具列表 (filterToolsByPermission)│   │
│  │  - 执行工具调用 (携带 AISessionContext) │     │
│  │  - 管理系统提示                        │     │
│  │  - QQ消息入口 (handleQQMessage / handleQQMessageWithResponse)│
│  └────────────────┬──────────────────────┘     │
│                   │                              │
│  ┌────────────────▼──────────────────────┐     │
│  │          SkillManager                 │     │
│  │  - 注册/注销 Skill                    │     │
│  │  - 工具映射管理                        │     │
│  │  - 权限校验 (hasSufficientPermission)  │     │
│  │  - 双重权限校验                        │     │
│  └────────────────┬──────────────────────┘     │
│                   │                              │
│  ┌────────────────▼──────────────────────┐     │
│  │            Skill 实例                  │     │
│  │  - EconomySkill                       │     │
│  │  - KnowledgeBaseSkill                 │     │
│  │  - YourCustomSkill                    │     │
│  └────────────────────────────────────────┘     │
└──────────────────────────────────────────────────┘
```

### 核心组件

| 组件 | 说明 |
|------|------|
| `AISessionContext` | 会话级权限上下文，携带用户身份和权限级别，全链路传递 |
| `Skill` 接口 | 定义 Skill 的标准接口 |
| `SkillManager` | 管理所有 Skill 的生命周期，权限校验 |
| `OpsCommandHandler` | 集成 Skill 到 AI 系统，QQ消息入口 |

---

## 快速开始

### 1. 创建项目

在你的 Maven 项目中添加依赖：

```xml
<dependencies>
    <!-- ArkOps-Ai 核心 -->
    <dependency>
        <groupId>com.arkops</groupId>
        <artifactId>ArkOps-Ai</artifactId>
        <version>2.0.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Bukkit API -->
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.20.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 2. 实现 Skill 接口

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
        // 如果需要注册事件，使用 mainPlugin：
        // Bukkit.getPluginManager().registerEvents(this, mainPlugin);
    }
    
    @Override
    public void onDisable() {
        // 清理逻辑
    }
}
```

### 3. 注册 Skill

在你的插件主类中注册：

```java
// 获取 SkillManager
SkillManager skillManager = ArkOpsAi.getInstance().getSkillManager();

// 注册你的 Skill
skillManager.registerSkill(new MyFirstSkill());
```

### 4. 编译并测试

```bash
mvn clean package
```

将生成的 jar 文件放入服务器 `plugins` 文件夹。

---

## ⚠️ 重要：Skill ≠ Plugin

### 核心概念

**Skill 不是 Bukkit Plugin**，这是开发 Skill 时最重要的概念。

| 特性 | Bukkit Plugin | ArkOps-Ai Skill |
|------|---------------|-----------------|
| 继承 | `JavaPlugin` | `Skill` 接口 |
| 生命周期 | Bukkit 管理 | SkillManager 管理 |
| 事件注册 | `registerEvents(this, this)` | `registerEvents(this, mainPlugin)` |
| 日志 | `getLogger()` | `mainPlugin.getLogger()` |
| 数据目录 | `getDataFolder()` | `mainPlugin.getDataFolder()` |

### 常见错误

```java
// ❌ 错误：Skill 不是 Plugin，不能使用 this
Bukkit.getPluginManager().registerEvents(this, this);

// ❌ 错误：Skill 没有 getLogger() 方法
getLogger().info("message");

// ❌ 错误：Skill 没有 getDataFolder() 方法
new File(getDataFolder(), "config.yml");
```

### 正确做法

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

### 为什么这样设计？

Bukkit 的设计是 **一个 jar = 一个 Plugin**。Skill 是 ArkOps-Ai 内部的模块/组件，不是独立的 Bukkit 插件。所有 Skill 共享 ArkOps-Ai 的 Plugin 实例。

---

## 会话级权限上下文 (AISessionContext)

### 什么是 AISessionContext？

`AISessionContext` 是一个**不可变、线程安全**的会话上下文对象，用于在 AI 调用链路中携带用户身份和权限信息。它解决了以下核心安全问题：

- **身份识别**：区分请求来源是 QQ 用户、游戏内玩家还是控制台
- **权限隔离**：AI 调用工具时使用真实用户的权限，而非默认的 CONSOLE 权限
- **防止越权**：每个工具调用都经过双重权限校验

### 调用链路

```
QQ消息 / 游戏内命令 / 控制台命令
        │
        ▼
┌──────────────────────┐
│  创建 AISessionContext │
│  .qqUser(...)         │  ← QQ机器人入口
│  .player(...)         │  ← 游戏内玩家入口
│  .console()           │  ← 控制台入口
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   executeAgentLoop    │
│   (携带 context)       │  ← 递归调用中保持不变
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   executeToolCall     │
│   (携带 context)       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ SkillManager          │
│ .executeTool(context) │  ← 双重权限校验
│  ① hasSufficientPermission │
│  ② skill.executeTool()     │
└──────────────────────┘
```

### 类定义

```java
package com.arkops.session;

public final class AISessionContext {
    private final String qqUserId;       // QQ用户ID (null = 非QQ用户)
    private final String permissionLevel; // 权限级别: PLAYER, ADMIN, SUPER_ADMIN, CONSOLE
    private final String displayName;     // 显示名称

    // 工厂方法
    public static AISessionContext console()                        // 控制台
    public static AISessionContext player(name, level)              // 游戏内玩家
    public static AISessionContext qqUser(qqId, name, level)       // QQ用户

    // Getter
    public String getQqUserId()
    public String getPermissionLevel()
    public String getDisplayName()
    public boolean isQQUser()
}
```

### 使用场景

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

#### 场景 1.5: QQ 机器人入口（同步返回模式，推荐）

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

#### 场景 2: 游戏内玩家

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

### 双重权限校验

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

### Skill 开发者视角

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

### 安全原则

1. **禁止信任 AI 传入的权限参数** — 权限始终来自服务器系统（QQ用户级别/游戏内权限）
2. **禁止绕过 executeTool 校验** — context 为空时立即拒绝
3. **禁止通过字符串拼接执行命令** — 所有操作经过权限校验
4. **不使用 ThreadLocal** — `AISessionContext` 作为方法参数显式传递

### 原生工具权限校验

`OpsCommandHandler.executeToolCall()` 中每个原生工具调用前都通过 `requirePermission()` 进行校验，形成**第三层权限控制**：

```java
private void requirePermission(PermissionManager.PermissionLevel callerLevel, PermissionManager.PermissionLevel requiredLevel, String toolName) {
    if (callerLevel.getLevel() < requiredLevel.getLevel()) {
        throw new SecurityException("权限不足：你的权限等级为 " + callerLevel.getDisplayName()
                + "，该操作需要 " + requiredLevel.getDisplayName() + " 权限。");
    }
}
```

`executeToolCall` 方法入口处先将上下文中的权限字符串解析为枚举，每个工具调用前自动拦截：

```java
String permissionLevel = context != null ? context.getPermissionLevel() : "PLAYER";
PermissionManager.PermissionLevel callerLevel = PermissionManager.PermissionLevel.fromString(permissionLevel);

switch (toolName) {
    case "restart_server":
        requirePermission(callerLevel, PermissionManager.PermissionLevel.SUPER_ADMIN, toolName);
        return actionManager.restartServer(sender);
    case "hot_reload_plugin":
    case "hot_unload_plugin":
    case "hot_load_plugin":
        requirePermission(callerLevel, PermissionManager.PermissionLevel.ADMIN, toolName);
        // ...
}
```

**权限映射表**：

| 工具 | 需要权限 |
|------|----------|
| `restart_server` | SUPER_ADMIN |
| `stop_server` | SUPER_ADMIN |
| `reload_server` | SUPER_ADMIN |
| `ban_player` | SUPER_ADMIN |
| `set_permission` | SUPER_ADMIN |
| `hot_reload_plugin` | ADMIN |
| `hot_unload_plugin` | ADMIN |
| `hot_load_plugin` | ADMIN |
| `list_plugins` | ADMIN |
| `execute_command` | ADMIN |
| `set_game_time` | ADMIN |
| `set_weather` | ADMIN |
| `set_game_mode` | ADMIN |
| `get_server_info` | ADMIN |
| `get_player_info` | ADMIN |
| `teleport_player` | ADMIN |
| `give_item` | ADMIN |
| `kick_player` | ADMIN |
| `get_player_held_item` | ADMIN |
| `get_player_biome` | ADMIN |
| `get_player_looking_at` | ADMIN |
| `get_player_detailed_info` | ADMIN |
| `get_online_players` | 无限制 |
| `check_permission` | 无限制 |

---

## Skill 接口详解

### 接口方法总览

| 方法 | 返回类型 | 必填 | 说明 |
|------|----------|------|------|
| `getId()` | String | 是 | Skill 唯一标识符 |
| `getName()` | String | 是 | Skill 人类可读名称 |
| `getDescription()` | String | 是 | Skill 功能描述 |
| `getVersion()` | String | 是 | Skill 版本号 |
| `getAuthor()` | String | 是 | Skill 作者 |
| `getTools()` | List<JsonObject> | 是 | 工具定义列表 |
| `executeTool()` | String | 是 | 工具执行逻辑 |
| `getSystemPrompt()` | String | 是 | AI 系统提示词 |
| `isAvailable()` | boolean | 是 | Skill 可用性检查 |
| `onEnable(JavaPlugin)` | void | 是 | 启用时调用，接收主插件实例 |
| `onDisable()` | void | 是 | 禁用时调用 |

### 方法详细说明

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

**参数说明**：
- `sender`: 命令发送者（玩家或控制台）
- `toolName`: AI 决定调用的工具名称
- `args`: 工具参数（JSON 对象）

**返回值**：
- 返回执行结果的字符串
- 这个结果会被 AI 读取并展示给用户

#### getSystemPrompt()

返回 AI 的系统提示词。告诉 AI 如何使用这个 Skill 的工具。

```java
@Override
public String getSystemPrompt() {
    return "=== Economy Skill ===\n" +
           "You have access to economy management tools:\n" +
           "- get_balance: Check a player's current balance\n" +
           "- transfer_money: Transfer money between players\n" +
           "- deposit_money: Add money to a player's account\n" +
           "- withdraw_money: Remove money from a player's account\n\n" +
           "Use these tools when players ask about money, balance, economy, " +
           "or want to transfer funds.\n" +
           "Always verify player names exist before performing operations.\n" +
           "Never allow negative amounts or transfers that would result " +
           "in negative balances.";
}
```

**编写要点**：
- 列出所有可用工具
- 说明每个工具的作用
- 说明使用场景
- 添加使用注意事项
- 说明限制条件

#### isAvailable()

检查 Skill 是否可用。可以检查依赖是否满足。

```java
@Override
public boolean isAvailable() {
    // 检查是否安装了 Vault 插件
    return Bukkit.getPluginManager().getPlugin("Vault") != null;
}
```

#### onEnable(JavaPlugin) / onDisable()

Skill 启用和禁用时的生命周期方法。

**重要：Skill ≠ Bukkit Plugin**

`onEnable` 方法会接收 ArkOps-Ai 的主插件实例。如果你需要注册事件监听器，**必须使用这个主插件实例**，而不是 `this`。

```java
@Override
public void onEnable(JavaPlugin mainPlugin) {
    // 初始化数据库连接
    // 加载配置文件
    
    // ✅ 正确：使用 mainPlugin 注册事件
    Bukkit.getPluginManager().registerEvents(this, mainPlugin);
    
    // ❌ 错误：Skill 不是 Plugin，不能使用 this
    // Bukkit.getPluginManager().registerEvents(this, this);
}

@Override
public void onDisable() {
    // 关闭数据库连接
    // 保存数据
    // 清理资源
}
```

---

## 工具定义详解

### 工具结构

每个工具是一个 JSON 对象，结构如下：

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

### 参数类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `string` | 字符串 | 玩家名称、消息 |
| `number` | 数字 | 数量、坐标 |
| `boolean` | 布尔值 | true/false |
| `array` | 数组 | 玩家列表 |
| `object` | 对象 | 复杂数据结构 |

### 创建工具的辅助类

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

### 工具定义示例

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

---

## 系统提示词编写指南

### 目的

系统提示词告诉 AI：
1. 有哪些工具可用
2. 每个工具的作用
3. 何时使用这些工具
4. 使用时的注意事项

### 结构模板

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

### 示例：经济系统

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

### 示例：世界编辑

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

### 最佳实践

1. **清晰明了**：使用简洁的英文描述
2. **列出所有工具**：确保 AI 知道所有可用工具
3. **说明使用场景**：告诉 AI 何时使用工具
4. **添加限制条件**：说明不能做什么
5. **提供最佳实践**：指导 AI 正确使用工具

---

## 完整示例：经济管理系统

### 1. 创建 Skill 类

```java
package com.arkops.skill.example;

import com.arkops.skill.Skill;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

### 2. 注册 Skill

```java
// 在插件主类的 onEnable() 中
SkillManager skillManager = new SkillManager(this);
skillManager.registerSkill(new EconomySkill());
```

### 3. 使用示例

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

## 完整示例：世界编辑工具

### 1. 创建 WorldEditSkill

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
                            args.get("x").getAsInt(),
                            args.get("y").getAsInt(),
                            args.get("z").getAsInt()
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
               "Use these tools when players ask to modify the world, place blocks, " +
               "build structures, or change the environment.\n" +
               "Always verify coordinates are within world bounds (-30,000,000 to 30,000,000).\n" +
               "Never allow operations that could crash the server (e.g., filling millions of blocks).\n" +
               "Always warn users about potentially lag-causing operations.\n" +
               "Block types must be valid Minecraft material names (e.g., STONE, GRASS_BLOCK, DIAMOND_BLOCK).";
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

    // ========== 世界编辑操作实现 ==========

    private String setBlock(String worldName, int x, int y, int z, String blockType) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return "World not found: " + worldName;
        }

        try {
            Material material = Material.valueOf(blockType.toUpperCase());
            Location loc = new Location(world, x, y, z);
            Block block = loc.getBlock();
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
            
            // 计算区域大小
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            
            int blockCount = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            
            // 限制操作大小
            if (blockCount > 10000) {
                return "Operation too large: " + blockCount + " blocks. Maximum is 10000 blocks.";
            }
            
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location loc = new Location(world, x, y, z);
                        loc.getBlock().setType(material);
                    }
                }
            }
            
            return "Successfully filled area with " + blockType + " (" + blockCount + " blocks) in " + worldName;
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
                                         int x, int y, int z) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "Player not found: " + playerName;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return "World not found: " + worldName;
        }

        Location loc = new Location(world, x, y, z);
        player.teleport(loc);
        return "Teleported " + playerName + " to (" + x + ", " + y + ", " + z + ") in " + worldName;
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

## 最佳实践

### 1. 错误处理

始终处理可能的异常情况：

```java
@Override
public String executeTool(CommandSender sender, String toolName, JsonObject args) {
    try {
        // 你的逻辑
        return result;
    } catch (NullPointerException e) {
        return "Missing required parameter";
    } catch (IllegalArgumentException e) {
        return "Invalid parameter: " + e.getMessage();
    } catch (Exception e) {
        return "Internal error: " + e.getMessage();
    }
}
```

### 2. 输入验证

验证所有输入参数：

```java
private String transferMoney(String from, String to, double amount) {
    // 验证金额
    if (amount <= 0) {
        return "Amount must be positive";
    }
    
    // 验证玩家
    if (from == null || from.isEmpty()) {
        return "Source player is required";
    }
    if (to == null || to.isEmpty()) {
        return "Target player is required";
    }
    
    // 执行逻辑
    // ...
}
```

### 3. 权限检查

ArkOps-Ai 提供了两层权限控制机制：

#### 3.1 工具级别权限声明

通过实现 `getToolPermissionLevel()` 方法，为每个工具声明所需的最低权限级别：

```java
@Override
public String getToolPermissionLevel(String toolName) {
    switch (toolName) {
        case "get_balance":
            return "PLAYER";        // 所有玩家都可以使用
        case "transfer_money":
            return "PLAYER";        // 所有玩家都可以使用
        case "set_balance":
            return "ADMIN";         // 需要管理员权限
        case "delete_account":
            return "SUPER_ADMIN";   // 需要超级管理员权限
        default:
            return "ADMIN";         // 默认为管理员权限
    }
}
```

**权限级别说明：**
- `PLAYER` (1): 所有玩家都可以使用
- `ADMIN` (2): 需要管理员权限
- `SUPER_ADMIN` (3): 需要超级管理员权限
- `CONSOLE` (4): 仅控制台可用

#### 3.2 权限过滤机制（工具列表过滤）

系统通过 `SkillManager.filterToolsByPermission()` 自动根据用户的权限级别过滤可用工具：

```
用户请求 → buildTools(level) → filterToolsByPermission(level) → AI 只能看到有权限的工具
```

**过滤逻辑**：
1. 获取调用者的权限级别数值（PLAYER=1, ADMIN=2, SUPER_ADMIN=3, CONSOLE=4）
2. 遍历所有已注册的 Skill 工具
3. 只返回 `调用者级别 >= 工具所需级别` 的工具

这意味着：
- PLAYER 用户看不到 ADMIN 级别的工具
- QQ 用户使用自己的权限级别，而非 CONSOLE

#### 3.3 执行时权限校验（AISessionContext 双重校验）

即使 AI 在工具列表中被限制了可见工具，系统在**真正执行工具时**还会进行第二次校验。

调用链路中 `AISessionContext` 全程携带用户身份：

```java
// SkillManager.executeTool(context) 内部
public String executeTool(CommandSender sender, String toolName, JsonObject args, AISessionContext context) {
    // context 为空 → 直接拒绝
    if (context == null) {
        return "错误: 缺少会话上下文，无法执行工具";
    }

    // 执行时再校验一次权限
    if (!hasSufficientPermission(toolName, context.getPermissionLevel())) {
        return "权限不足：你的权限等级为 " + context.getPermissionLevel()
                + "，该操作需要 " + skill.getToolPermissionLevel(toolName) + " 权限。";
    }

    return skill.executeTool(sender, toolName, args);
}
```

**Skill 开发者无需关心 AISessionContext**，只需要正确声明 `getToolPermissionLevel()` 即可。框架自动完成双重校验。

#### 3.4 传统权限检查

在执行操作前，仍然可以检查 Bukkit 权限：

```java
@Override
public String executeTool(CommandSender sender, String toolName, JsonObject args) {
    // 检查发送者是否是玩家
    if (!(sender instanceof Player)) {
        return "This command can only be used by players";
    }
    
    Player player = (Player) sender;
    
    // 检查 Bukkit 权限
    if (!player.hasPermission("myskill.use")) {
        return "You don't have permission to use this skill";
    }
    
    // 执行逻辑
    // ...
}
```

### 4. 性能优化

避免阻塞操作：

```java
// 不好的做法：同步大量方块操作
for (int i = 0; i < 100000; i++) {
    block.setType(material);  // 会导致服务器卡顿
}

// 好的做法：限制操作大小
if (blockCount > 10000) {
    return "Operation too large";
}
```

### 5. 日志记录

记录重要操作：

```java
@Override
public void onEnable(JavaPlugin mainPlugin) {
    mainPlugin.getLogger().info("[MySkill] Skill enabled successfully");
}

private String transferMoney(String from, String to, double amount) {
    Bukkit.getLogger().info("[MySkill] " + from + " transferred $" + amount + " to " + to);
    return result;
}
```

### 6. 配置支持

支持配置文件：

```java
public class MySkill implements Skill {
    private File configFile;
    private FileConfiguration config;
    private JavaPlugin mainPlugin;
    
    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        this.mainPlugin = mainPlugin;
        configFile = new File(mainPlugin.getDataFolder(), "skills/myskill.yml");
        if (!configFile.exists()) {
            // 创建默认配置
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    private String getMaxTransferAmount() {
        return config.getString("max_transfer_amount", "1000");
    }
}
```

### 7. 文档完善

为你的 Skill 编写清晰的文档：

```java
/**
 * Economy Management Skill
 * 
 * Provides tools for managing player economies including:
 * - Balance checking
 * - Money transfers
 * - Deposits and withdrawals
 * 
 * @author Your Name
 * @version 1.0.0
 * @since 2024-01-01
 */
public class EconomySkill implements Skill {
    // ...
}
```

---

## 常见问题

### Q1: 如何调试 Skill？

**A**: 使用 Bukkit 的日志系统：

```java
Bukkit.getLogger().info("[MySkill] Debug: " + message);
```

或者在控制台查看服务器日志。

### Q2: Skill 可以使用外部库吗？

**A**: 可以。将依赖添加到你的 pom.xml 中，使用 Maven Shade Plugin 打包：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <configuration>
                <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Q3: 如何与其他插件集成？

**A**: 通过 Bukkit 的插件管理器获取其他插件实例：

```java
Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
if (vaultPlugin != null) {
    // 使用 Vault API
}
```

### Q4: Skill 可以存储数据吗？

**A**: 可以。使用文件、数据库或内存存储：

```java
// 文件存储
private File dataFile = new File("plugins/ArkOps-Ai/skills/mydata.yml");

// 数据库存储（需要添加数据库依赖）
// 内存存储
private Map<String, Object> data = new ConcurrentHashMap<>();
```

### Q5: 如何处理异步操作？

**A**: 使用 Bukkit 的调度器：

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // 异步执行耗时操作
    String result = performHeavyOperation();
    
    // 回到主线程更新游戏状态
    Bukkit.getScheduler().runTask(plugin, () -> {
        // 主线程操作
    });
});
```

### Q6: Skill 可以注册命令吗？

**A**: 可以。在 `onEnable(JavaPlugin mainPlugin)` 中注册：

```java
@Override
public void onEnable(JavaPlugin mainPlugin) {
    PluginCommand command = mainPlugin.getCommand("myskill");
    if (command != null) {
        command.setExecutor(new MyCommandExecutor());
    }
}
```

### Q7: 如何测试 Skill？

**A**: 
1. 本地测试：使用本地 Minecraft 服务器
2. 单元测试：使用 Mockito 模拟 Bukkit API
3. 集成测试：在测试服务器上验证功能

### Q8: Skill 可以监听事件吗？

**A**: 可以。注册事件监听器时必须使用主插件实例：

```java
@Override
public void onEnable(JavaPlugin mainPlugin) {
    Bukkit.getPluginManager().registerEvents(new MyEventListener(), mainPlugin);
}

public class MyEventListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 处理玩家加入事件
    }
}
```

### Q9: 为什么会出现 AbstractMethodError？

**A**: 这通常是因为 Skill 编译时使用的 `Skill` 接口版本与运行时不一致。

**常见原因：**
- Skill 使用旧版接口编译（`onEnable()` 无参数）
- ArkOps-Ai 使用新版接口运行（`onEnable(JavaPlugin)` 有参数）

**解决方法：**
1. 确保 Skill 项目的 `pom.xml` 中引用了正确版本的 ArkOps-Ai
2. 重新编译 Skill 项目
3. 替换服务器中的 Skill jar 文件

```xml
<!-- 确保版本一致 -->
<dependency>
    <groupId>com.arkops</groupId>
    <artifactId>ArkOps-Ai</artifactId>
    <version>2.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Q10: 如何管理 Skill 的依赖？

**A**: 有两种方式：

**方式一：打包进 Skill jar（推荐）**
使用 maven-shade-plugin 将依赖打包进 Skill jar：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**方式二：放到服务器 libs 文件夹**
将依赖 jar 放到服务器的 `libs` 文件夹中（不推荐，容易造成冲突）。

### Q11: 如何获取 ArkOps-Ai 的依赖？

**A**: 有以下几种方式：

**方式一：本地安装（推荐用于开发）**
```bash
cd ArkOps-Ai-CN
mvn clean install -DskipTests
```

**方式二：使用 systemPath（简单但不推荐用于发布）**
```xml
<dependency>
    <groupId>com.arkops</groupId>
    <artifactId>ArkOps-Ai</artifactId>
    <version>2.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/ArkOps-Ai-2.0.0.jar</systemPath>
</dependency>
```

**方式三：使用 JitPack（推荐用于开源项目）**
1. 将 ArkOps-Ai 推送到 GitHub
2. 在 [jitpack.io](https://jitpack.io) 添加仓库
3. Skill 项目引用：
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.YourUsername</groupId>
        <artifactId>ArkOps-Ai-CN</artifactId>
        <version>2.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 版本兼容性

### API 版本历史

| ArkOps-Ai 版本 | Skill 接口版本 | onEnable 签名 | 发布日期 |
|----------------|----------------|---------------|----------|
| 2.0.0+ | v2 | `onEnable(JavaPlugin mainPlugin)` | 2026-04-24 |
| 1.x.x | v1 | `onEnable()` | 2026-04-23 |

### 如何升级 Skill 到新版接口

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

### 检查 Skill 兼容性

在编译 Skill 时，确保：
1. `pom.xml` 中的 ArkOps-Ai 版本与服务器一致
2. 实现了所有接口方法（IDE 会提示缺失的方法）
3. 使用 `mvn clean package` 重新编译

---

## 总结

通过 Skill 系统，你可以为 ArkOps-Ai 添加无限扩展的能力。只需实现 `Skill` 接口，定义工具和系统提示词，就能让 AI 使用你的功能。

### 关键步骤回顾

1. **实现 Skill 接口**：提供基本信息、工具定义、执行逻辑
2. **定义工具**：使用 JSON 定义工具名称、描述和参数
3. **编写系统提示**：告诉 AI 如何使用你的工具
4. **注册 Skill**：通过 SkillManager 注册你的 Skill
5. **测试验证**：在游戏中测试功能是否正常

### 下一步

- 查看示例代码：`src/main/java/com/arkops/skill/example/`
- 参考接口定义：`src/main/java/com/arkops/skill/Skill.java`
- 查看管理器实现：`src/main/java/com/arkops/skill/SkillManager.java`

祝你开发愉快！如有问题，请参考本文档或联系开发团队。
