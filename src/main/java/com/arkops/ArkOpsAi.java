package com.arkops;

import com.arkops.commands.OpsCommandExecutor;
import com.arkops.commands.OpsCommandHandler;
import com.arkops.commands.OpsGuiCommand;
import com.arkops.listener.ChatListener;
import com.arkops.manager.LanguageManager;
import com.arkops.manager.OpenAiManager;
import com.arkops.manager.PermissionManager;
import com.arkops.manager.ServerActionManager;
import com.arkops.skill.SkillManager;
import com.arkops.util.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ArkOpsAi extends JavaPlugin {

    private static ArkOpsAi instance;
    private OpenAiManager openAiManager;
    private PermissionManager permissionManager;
    private ServerActionManager serverActionManager;
    private LanguageManager languageManager;
    private Logger logger;
    private OpsCommandHandler opsCommandHandler;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        this.logger = new Logger(this);
        this.languageManager = new LanguageManager(this);

        this.logger.info("🏢筑梦方舟网络科技工作室版权所有");
        this.logger.info("ArkOps-Ai 正在初始化...");

        this.permissionManager = new PermissionManager(this);
        this.openAiManager = new OpenAiManager(this);
        this.serverActionManager = new ServerActionManager(this);
        this.skillManager = new SkillManager(this);

        // 自动创建 Skill 文件夹
        File skillsDir = new File(getDataFolder(), "skills");
        if (!skillsDir.exists()) {
            if (skillsDir.mkdirs()) {
                this.logger.info("已自动创建 Skill 文件夹: " + skillsDir.getAbsolutePath());
            }
        } else {
            this.logger.info("Skill 文件夹已存在: " + skillsDir.getAbsolutePath());
        }

        // 从 skills 文件夹加载外部 Skill
        this.skillManager.loadSkillsFromFolder(skillsDir.getAbsolutePath());

        this.opsCommandHandler = new OpsCommandHandler(this);
        getCommand("ops").setExecutor(new OpsCommandExecutor(this, this.opsCommandHandler));

        OpsGuiCommand opsGuiCommand = new OpsGuiCommand(this, this.opsCommandHandler);
        getCommand("opsgui").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("§c该命令只能由玩家使用");
                return true;
            }
            opsGuiCommand.openGui((org.bukkit.entity.Player) sender);
            return true;
        });
        getServer().getPluginManager().registerEvents(opsGuiCommand, this);

        getServer().getPluginManager().registerEvents(new ChatListener(this, this.opsCommandHandler), this);

        this.logger.info("ArkOps-Ai 已成功启用!");
        this.logger.info("使用 /ops 命令开始 ArkOpsAI 运维管理");
        this.logger.info("使用 @ops 在聊天中直接与 AI 对话");
        this.logger.info("已加载 " + this.skillManager.getSkillCount() + " 个 Skill, " + this.skillManager.getToolCount() + " 个工具");
    }

    @Override
    public void onDisable() {
        if (this.logger != null) {
            this.logger.info("ArkOps-Ai 正在关闭...");
        }
        if (this.openAiManager != null) {
            this.openAiManager.shutdown();
        }
        if (this.skillManager != null) {
            this.skillManager.shutdown();
        }
        getLogger().info("ArkOps-Ai 已关闭");
    }

    public static ArkOpsAi getInstance() {
        return instance;
    }

    public OpenAiManager getOpenAiManager() {
        return openAiManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public ServerActionManager getServerActionManager() {
        return serverActionManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public Logger getArkOpsLogger() {
        return logger;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public OpsCommandHandler getOpsCommandHandler() {
        return opsCommandHandler;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (languageManager != null) {
            languageManager.reload();
        }
        if (permissionManager != null) {
            permissionManager.reloadPermissions();
        }
        logger.info("ArkOps-Ai 配置文件已热重载");
    }
}
