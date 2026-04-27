# ArkOps-Ai

> 🏢 筑梦方舟网络科技工作室 (DreamArk Studio) 版权所有

**ArkOps-Ai** 是一个基于 AI 的 Minecraft 服务器智能运维管理插件，适用于 PurpurMC/PaperMC 服务端。通过自然语言交互，让服务器管理变得更加智能和高效。

---

## ✨ 核心特性

- **🤖 AI 驱动运维**：通过 `/ops` 命令使用自然语言管理服务器，支持复杂多步骤任务
- **💬 @ops 聊天功能**：在游戏聊天栏直接 `@ops` 与 AI 对话，全体玩家可见
- **🔌 Skill 扩展系统**：模块化插件架构，支持开发者开发外部 Skill 扩展 AI 能力
- **👤 玩家信息获取**：查看玩家手持物品、所在群系、面前方块等详细信息
- **🧠 上下文记忆**：智能维护与每位玩家的对话历史，支持配置化调整
- **🔐 四级权限系统**：从玩家到超级管理员的细粒度权限控制
- **🌍 国际化支持**：内置英文和中文语言包
- **📊 全面服务器管理**：重启、停止、重载、插件管理、玩家管理、世界管理等

---

## 📋 权限等级

| 等级 | 描述 | 能力范围 |
|------|------|----------|
| **DISABLED** | 无权限 | 无法使用任何功能 |
| **PLAYER** | 默认玩家 | 基础游戏查询、在线玩家列表 |
| **ADMIN** | 管理员 | 插件管理、玩家管理、世界管理、命令执行、游戏设置 |
| **SUPER_ADMIN** | 超级管理员 | 所有权限，包括服务器控制、玩家封禁、权限设置 |

---

## 🚀 快速开始

### 安装步骤

1. **下载插件**：从 [Releases](https://github.com/DreamArk-Studio/ArkOps-Ai-CN/releases) 页面下载最新的 `ArkOps-Ai-2.0.0.jar`
2. **部署插件**：将 jar 文件放入服务器的 `plugins` 文件夹
3. **启动服务器**：启动或重启 Minecraft 服务器
4. **配置插件**：编辑 `plugins/ArkOps-Ai/config.yml` 配置文件
5. **添加 API Key**：在配置文件中填入你的 AI API Key

### 环境要求

- **Minecraft 服务端**：PurpurMC / PaperMC 1.20+
- **Java 版本**：Java 21 或更高版本
- **AI API**：兼容 OpenAI 接口的服务（如 DeepSeek、阿里云百炼等）

---

## ⚙️ 配置说明

### config.yml

```yaml
# 语言设置 (en, zh)
language: "zh"

# OpenAI 兼容 API 配置
# 支持所有兼容 OpenAI 接口的服务:
#   - OpenAI: api-url: "https://api.openai.com/v1/chat/completions"
#   - DeepSeek: api-url: "https://api.deepseek.com/chat/completions"
#   - 阿里云百炼: api-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
openai:
  api-key: "YOUR_API_KEY_HERE"        # API Key
  model: "qwen3.5-plus"               # 模型名称
  api-url: "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
  timeout: 60                         # 超时时间 (秒)
  max-tokens: 2000                    # 最大响应 token 数
  temperature: 1.0                    # 温度参数 (0-2)

# 频率限制配置
rate-limit:
  enabled: true                       # 是否启用频率限制
  window-seconds: 60                  # 时间窗口 (秒)
  max-requests-player: 5              # 玩家最大请求次数
  max-requests-admin: 0               # 管理员最大请求次数 (0=不限制)
  max-requests-super-admin: 0         # 超级管理员最大请求次数
  max-requests-console: 0             # 控制台最大请求次数

# 上下文配置
context:
  enabled: true                       # 是否启用上下文
  max-messages-per-player: 5          # 每个玩家的消息数量

# 聊天触发器配置
chat-trigger:
  enabled: true                       # 是否启用聊天触发器
  trigger: "@ops"                     # 触发词
```

### permissions.yml

在 `plugins/ArkOps-Ai/permissions.yml` 中配置玩家权限：

```yaml
players:
  # 按 UUID 配置权限
  6f12f43f-150b-3437-9836-651e535176ec:
    name: "PlayerName"
    level: "SUPER_ADMIN"

# 按名称索引 (用于快速查找)
players_by_name:
  PlayerName: "6f12f43f-150b-3437-9836-651e535176ec"
```

---

## 🎮 使用指南

### 基础命令

```bash
/ops                                    # 显示帮助信息
/ops restart server                     # 重启服务器
/ops stop server                        # 停止服务器
/ops reload server                      # 重载服务器
/ops hot-reload Essentials              # 热重载插件
/ops hot-unload PluginName              # 热卸载插件
/ops hot-load PluginName                # 热加载插件
/ops list plugins                       # 列出所有插件及描述
/ops execute command <cmd>              # 执行任意服务器命令
/ops set game time to night             # 设置游戏时间
/ops set weather to clear               # 设置天气
/ops set my gamemode to creative        # 设置游戏模式
/ops server status                      # 获取服务器状态
/ops player info <name>                 # 获取玩家信息
/ops player detailed info <name>        # 获取玩家详细信息
/ops teleport <player> to <target>      # 传送玩家
/ops give item diamond 64 to <player>   # 给予物品
/ops kick player <name>                 # 踢出玩家
/ops ban player <name>                  # 封禁玩家 (仅 SUPER_ADMIN)
/ops set permission <player> <level>    # 设置玩家权限 (仅 SUPER_ADMIN)
```

### 聊天功能

在游戏聊天栏输入 `@ops 你的问题` 即可与 AI 对话，所有玩家都能看到对话内容和 AI 回复。

**示例**：
```
@ops 现在服务器上有几个玩家？
@ops 帮我查看一下 Steve 的详细信息
@ops 今天的天气怎么样？
```

### 自然语言示例

ArkOps-Ai 能够理解自然语言，因此你可以使用灵活的命令：

```bash
/ops restart the server please
/ops what plugins are installed?
/ops set the time to sunset
/ops give me 64 diamonds
/ops teleport me to the spawn
/ops who is online right now?
```

---

## 🔌 Skill 扩展系统

ArkOps-Ai 支持开发者通过 Skill 系统扩展 AI 能力。

### 快速开始

1. **复制模板**：复制 `skill-template` 目录作为你的 Skill 项目
2. **实现接口**：实现 `Skill` 接口
3. **编译部署**：编译并将 jar 放置到 `plugins/ArkOps-Ai/skills/` 目录
4. **重启生效**：重启服务器加载 Skill

### 开发文档

详细开发文档请查看 [docs/SKILL_DEVELOPMENT.md](docs/SKILL_DEVELOPMENT.md)

### 注意事项

- ⚠️ **Skill ≠ Bukkit Plugin**：注册事件必须使用主插件实例
- 📦 `onEnable(JavaPlugin mainPlugin)` 接收主插件实例
- 🔧 依赖作用域设为 `provided`

### 示例 Skill

项目包含两个示例 Skill：
- **经济管理系统**：展示如何集成经济插件
- **世界编辑工具**：展示如何操作世界和方块

---

## 🏗️ 从源码构建

### 环境要求

- **Java**：21 或更高版本
- **Maven**：3.6 或更高版本

### 构建步骤

```bash
git clone https://github.com/DreamArk-Studio/ArkOps-Ai-CN.git
cd ArkOps-Ai-CN
mvn clean package -DskipTests
```

编译后的 jar 文件将位于 `target/ArkOps-Ai-2.0.0.jar`。

---

## 📦 Maven 依赖

### GitHub Packages

ArkOps-Ai 已发布到 GitHub Packages，你可以在 Maven 项目中直接引用：

#### 1. 配置仓库

在你的 `pom.xml` 中添加 GitHub Packages 仓库：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DreamArk-Studio/ArkOps-Ai</url>
    </repository>
</repositories>
```

#### 2. 添加依赖

```xml
<dependencies>
    <dependency>
        <groupId>com.arkops</groupId>
        <artifactId>ArkOps-Ai</artifactId>
        <version>2.2.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### 3. 配置认证（本地开发）

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

> **注意**：GitHub Personal Access Token 需要 `read:packages` 权限。创建方法：
> GitHub Settings → Developer settings → Personal access tokens → Generate new token

### 开发 Skill 项目

如果你是 Skill 开发者，需要在项目中引用 ArkOps-Ai API：

```xml
<dependencies>
    <!-- ArkOps-Ai 核心 API -->
    <dependency>
        <groupId>com.arkops</groupId>
        <artifactId>ArkOps-Ai</artifactId>
        <version>2.2.0</version>
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

详细的 Skill 开发教程请查看 [docs/SKILL_DEVELOPMENT.md](docs/SKILL_DEVELOPMENT.md)

---

## 🏛️ 架构设计

### 核心组件

| 组件 | 说明 |
|------|------|
| `ArkOpsAi` | 主插件类，负责初始化和生命周期管理 |
| `LanguageManager` | 国际化支持，管理多语言配置 |
| `PermissionManager` | 权限管理系统，四级权限控制 |
| `OpenAiManager` | AI API 通信管理，处理请求和响应 |
| `ServerActionManager` | 服务器操作执行器 |
| `OpsCommandExecutor` | 命令入口点，解析用户输入 |
| `OpsCommandHandler` | AI Agent 循环和工具执行核心 |
| `ChatListener` | 聊天事件监听器（@ops 功能） |
| `SkillManager` | Skill 生命周期管理和权限校验 |
| `Skill` | Skill 接口定义 |
| `AISessionContext` | 会话级权限上下文，全链路传递用户身份 |

### 调用链路

```
用户输入 (/ops 或 @ops)
    │
    ▼
┌─────────────────┐
│  创建会话上下文   │
│  AISessionContext│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  构建工具列表    │
│  (权限过滤)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  AI Agent 循环   │
│  executeAgentLoop│
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌───────┐ ┌──────────┐
│ AI 响应│ │ 工具调用  │
└───────┘ └────┬─────┘
               │
               ▼
        ┌──────────────┐
        │ SkillManager  │
        │ 执行工具      │
        │ (权限校验)    │
        └──────────────┘
```

---



## 📄 许可证

本项目采用 [MIT 许可证](LICENSE) - 你可以自由使用、修改和分发本项目。

---

## 💬 支持与反馈

- **问题反馈**：在 [GitHub Issues](https://github.com/DreamArk-Studio/ArkOps-Ai-CN/issues) 提交 Issue
- **功能请求**：欢迎提交 Feature Request
- **贡献代码**：欢迎提交 Pull Request

---

## 🤝 贡献指南

我们欢迎社区贡献！如果你想参与开发：

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

**筑梦方舟网络科技工作室** - 让 Minecraft 服务器管理更智能 🚀
