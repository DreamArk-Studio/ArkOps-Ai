# 方舟终端 (ArkOps-Ai)

> 🏢 筑梦方舟网络科技工作室版权所有

方舟终端是一个基于 AI 的 Minecraft 服务器运维管理插件，适用于 PurpurMC/PaperMC。它使服务器管理员能够通过自然语言命令来管理服务器，由 DeepSeek 大语言模型提供驱动。

## 功能特性

- **AI 驱动运维**：通过 `/ops` 命令使用自然语言管理服务器
- **@ops 聊天功能**：在聊天栏直接 @ops 与 AI 对话，全体玩家可见
- **Skill 扩展系统**：支持开发者开发外部 Skill 扩展 AI 能力
- **玩家信息获取**：查看玩家手持物品、所在群系、面前方块
- **上下文记忆**：记住与每位玩家的对话历史，配置可调
- **多步骤 Agent 系统**：AI 可执行复杂的多步骤任务
- **基于权限的访问控制**：四级权限系统确保操作安全
- **国际化支持**：支持英文和中文语言
- **全面的服务器管理**：重启、停止、重载、插件管理、玩家管理、世界管理等

## 权限等级

| 等级 | 描述 | 能力 |
|-------|-------------|--------------|
| **DISABLED** | 无权限 | 无法使用任何功能 |
| **PLAYER** | 默认 | 只能询问游戏相关问题 |
| **ADMIN** | 管理员 | 插件管理、玩家管理、世界管理、命令执行 |
| **SUPER_ADMIN** | 超级管理员 | 所有权限，包括服务器控制、玩家封禁、权限设置 |

## 安装步骤

1. 从 [Releases](https://github.com/DreamArk-Studio/ArkOps-Ai-CN/releases) 页面下载最新的 `ArkOps-Ai-2.0.0.jar`
2. 将 jar 文件放入服务器的 `plugins` 文件夹
3. 启动或重启服务器
4. 在 `plugins/ArkOps-Ai/config.yml` 中配置插件
5. 在配置文件中添加你的 DeepSeek API Key

## 配置说明

### config.yml

```yaml
# 语言设置 (en, zh)
language: "zh"

# DeepSeek API 配置
deepseek:
  # DeepSeek API Key
  api-key: "YOUR_API_KEY_HERE"
  # 使用的模型
  model: "deepseek-chat"
  # API 超时时间 (秒)
  timeout: 60
  # 最大响应 token 数
  max-tokens: 2000
  # 温度参数 (创造性, 0-2)
  temperature: 1.0

# 上下文设置
context:
  # 每个玩家保留的对话历史条数
  max-messages: 5

# 聊天触发设置
chat:
  # 触发 AI 聊天的关键词
  trigger: "@ops"
```

### permissions.yml

在 `plugins/ArkOps-Ai/permissions.yml` 中配置玩家权限等级：

```yaml
players:
  # 示例：
  # 6f12f43f-150b-3437-9836-651e535176ec:
  #   name: "PlayerName"
  #   level: "SUPER_ADMIN"
```

## 使用方法

### 基础命令

```
/ops                          # 显示帮助
/ops restart server           # 重启服务器
/ops stop server              # 停止服务器
/ops hot-reload Essentials    # 热重载插件
/ops hot-unload PluginName    # 热卸载插件
/ops hot-load PluginName      # 热加载插件
/ops list plugins             # 列出所有插件及描述
/ops execute command <cmd>    # 执行任意服务器命令
/ops set game time to night   # 设置游戏时间
/ops set weather to clear     # 设置天气
/ops set my gamemode to creative  # 设置游戏模式
/ops server status            # 获取服务器状态
/ops player info <name>       # 获取玩家信息
/ops player detailed info <name>  # 获取玩家详细信息（手持物品、群系、面前方块）
/ops teleport <player> to <target>  # 传送玩家
/ops give item diamond 64 to <player>  # 给予物品
/ops kick player <name>       # 踢出玩家
/ops ban player <name>        # 封禁玩家 (仅 SUPER_ADMIN)
```

### 聊天功能

在聊天栏输入 `@ops 你的问题` 即可与 AI 对话，所有玩家都能看到对话内容和 AI 回复。

### 自然语言示例

ArkOpsAI 能够理解自然语言，因此你可以使用灵活的命令：

- `/ops restart the server please`
- `/ops what plugins are installed?`
- `/ops set the time to sunset`
- `/ops give me 64 diamonds`
- `/ops teleport me to the spawn`

## Skill 扩展系统

方舟终端支持开发者开发外部 Skill 来扩展 AI 能力。

### 快速开始

1. 复制 `skill-template` 目录作为你的 Skill 项目
2. 实现 `Skill` 接口
3. 编译并放置 jar 到 `plugins/ArkOps-Ai/skills/` 目录
4. 重启服务器

### 开发文档

详细开发文档请查看 [docs/SKILL_DEVELOPMENT.md](docs/SKILL_DEVELOPMENT.md)

### 注意事项

- Skill ≠ Bukkit Plugin，注册事件必须使用主插件实例
- `onEnable(JavaPlugin mainPlugin)` 接收主插件实例
- 依赖作用域设为 `provided`

## 从源码构建

### 环境要求

- Java 21
- Maven 3.6+

### 构建步骤

```bash
git clone https://github.com/DreamArk-Studio/ArkOps-Ai-CN.git
cd ArkOps-Ai-CN
mvn clean package -DskipTests
```

编译后的 jar 文件将位于 `target/ArkOps-Ai-2.0.0.jar`。

## 架构设计

- **ArkOpsAi**: 主插件类
- **LanguageManager**: 处理国际化
- **PermissionManager**: 管理玩家权限等级
- **OpenAiManager**: 处理 DeepSeek API 通信
- **ServerActionManager**: 执行服务器操作
- **OpsCommandExecutor**: 命令入口点
- **OpsCommandHandler**: AI Agent 循环和工具执行
- **ChatListener**: 聊天事件监听器（@ops 功能）
- **SkillManager**: Skill 管理器
- **Skill**: Skill 接口定义

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 2.0.0 | 2026-04-24 | Skill系统、@ops聊天、玩家信息获取、上下文管理 |
| 1.1.0 | 2026-04-23 | 热加载插件、列出插件、执行命令 |
| 1.0.0 | 2026-04-22 | 初始版本 |

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 支持与反馈

如有问题、功能请求或疑问，请在 [GitHub](https://github.com/DreamArk-Studio/ArkOps-Ai-CN/issues) 上提交 Issue。
