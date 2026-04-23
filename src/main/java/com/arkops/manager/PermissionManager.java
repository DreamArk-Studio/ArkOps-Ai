package com.arkops.manager;

import com.arkops.ArkOpsAi;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PermissionManager {

    private final ArkOpsAi plugin;
    private File permissionFile;
    private FileConfiguration permissionConfig;
    private final Map<UUID, PermissionLevel> playerPermissions = new HashMap<>();

    public enum PermissionLevel {
        DISABLED(0, "禁用"),
        PLAYER(1, "玩家"),
        ADMIN(2, "管理员"),
        SUPER_ADMIN(3, "超级管理员"),
        CONSOLE(4, "控制台");

        private final int level;
        private final String displayName;

        PermissionLevel(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static PermissionLevel fromString(String text) {
            try {
                return valueOf(text.toUpperCase());
            } catch (IllegalArgumentException e) {
                return PLAYER;
            }
        }
    }

    public PermissionManager(ArkOpsAi plugin) {
        this.plugin = plugin;
        loadPermissionFile();
        loadAllPermissions();
    }

    private void loadPermissionFile() {
        permissionFile = new File(plugin.getDataFolder(), "permissions.yml");
        if (!permissionFile.exists()) {
            plugin.saveResource("permissions.yml", false);
        }
        permissionConfig = YamlConfiguration.loadConfiguration(permissionFile);
    }

    private void loadAllPermissions() {
        if (permissionConfig.contains("players")) {
            for (String uuidStr : permissionConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String levelStr = permissionConfig.getString("players." + uuidStr + ".level", "PLAYER");
                    playerPermissions.put(uuid, PermissionLevel.fromString(levelStr));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的 UUID: " + uuidStr);
                }
            }
        }
    }

    public PermissionLevel getPermissionLevel(UUID playerId) {
        if (playerId == null) {
            return PermissionLevel.CONSOLE;
        }
        return playerPermissions.getOrDefault(playerId, PermissionLevel.PLAYER);
    }

    public PermissionLevel getPermissionLevel(String playerName) {
        if (permissionConfig.contains("players_by_name." + playerName)) {
            String uuidStr = permissionConfig.getString("players_by_name." + playerName);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                return getPermissionLevel(uuid);
            } catch (IllegalArgumentException e) {
                return PermissionLevel.PLAYER;
            }
        }
        return PermissionLevel.PLAYER;
    }

    public void setPermissionLevel(UUID playerId, String playerName, PermissionLevel level) {
        playerPermissions.put(playerId, level);

        String path = "players." + playerId.toString();
        permissionConfig.set(path + ".name", playerName);
        permissionConfig.set(path + ".level", level.name());

        permissionConfig.set("players_by_name." + playerName, playerId.toString());

        savePermissionFile();
    }

    public boolean hasPermission(UUID playerId, PermissionLevel requiredLevel) {
        return getPermissionLevel(playerId).getLevel() >= requiredLevel.getLevel();
    }

    public boolean canAnswerQuestions(UUID playerId) {
        return getPermissionLevel(playerId).getLevel() >= PermissionLevel.PLAYER.getLevel();
    }

    public boolean canExecuteAdminCommands(UUID playerId) {
        return getPermissionLevel(playerId).getLevel() >= PermissionLevel.ADMIN.getLevel();
    }

    public boolean canExecuteSuperAdminCommands(UUID playerId) {
        return getPermissionLevel(playerId).getLevel() >= PermissionLevel.SUPER_ADMIN.getLevel();
    }

    private void savePermissionFile() {
        try {
            permissionConfig.save(permissionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存权限文件: " + e.getMessage());
        }
    }

    public void reloadPermissions() {
        permissionConfig = YamlConfiguration.loadConfiguration(permissionFile);
        playerPermissions.clear();
        loadAllPermissions();
    }
}
