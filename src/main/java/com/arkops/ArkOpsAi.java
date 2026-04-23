package com.arkops;

import com.arkops.commands.OpsCommandExecutor;
import com.arkops.manager.LanguageManager;
import com.arkops.manager.OpenAiManager;
import com.arkops.manager.PermissionManager;
import com.arkops.manager.ServerActionManager;
import com.arkops.util.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArkOpsAi extends JavaPlugin {

    private static ArkOpsAi instance;
    private OpenAiManager openAiManager;
    private PermissionManager permissionManager;
    private ServerActionManager serverActionManager;
    private LanguageManager languageManager;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        this.logger = new Logger(this);
        this.languageManager = new LanguageManager(this);

        this.logger.info("ArkOps-Ai 正在初始化...");

        this.permissionManager = new PermissionManager(this);
        this.openAiManager = new OpenAiManager(this);
        this.serverActionManager = new ServerActionManager(this);

        getCommand("ops").setExecutor(new OpsCommandExecutor(this));

        this.logger.info("ArkOps-Ai 已成功启用!");
        this.logger.info("使用 /ops 命令开始 ArkOpsAI 运维管理");
    }

    @Override
    public void onDisable() {
        if (this.logger != null) {
            this.logger.info("ArkOps-Ai 正在关闭...");
        }
        if (this.openAiManager != null) {
            this.openAiManager.shutdown();
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
}
