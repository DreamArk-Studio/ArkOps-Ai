package com.arkops.manager;

import com.arkops.ArkOpsAi;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerActionManager {

    private final ArkOpsAi plugin;

    public ServerActionManager(ArkOpsAi plugin) {
        this.plugin = plugin;
    }

    public String restartServer(CommandSender sender) {
        plugin.getLogger().info("服务器正在重启... 由 " + sender.getName() + " 触发");
        Bukkit.broadcastMessage("§c§l[服务器] 服务器正在重启...");
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
        return "服务器重启命令已执行";
    }

    public String stopServer(CommandSender sender) {
        plugin.getLogger().info("服务器正在关闭... 由 " + sender.getName() + " 触发");
        Bukkit.broadcastMessage("§c§l[服务器] 服务器正在关闭...");
        Bukkit.shutdown();
        return "服务器关闭命令已执行";
    }

    public String reloadServer(CommandSender sender) {
        Bukkit.reload();
        plugin.getLogger().info("服务器已重载 由 " + sender.getName() + " 触发");
        return "服务器已重载";
    }

    public String hotReloadPlugin(CommandSender sender, String pluginName) {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return "未找到插件: " + pluginName;
        }

        Plugin plugManX = Bukkit.getPluginManager().getPlugin("PlugManX");
        if (plugManX != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getServer().dispatchCommand(sender, "plugman reload " + pluginName);
            });
            return "使用 PlugManX 热重载插件: " + pluginName;
        }

        return "未安装 PlugManX，无法热重载插件。请安装 PlugManX 或使用 Plugman 指令手动操作。";
    }

    public String hotUnloadPlugin(CommandSender sender, String pluginName) {
        Plugin targetPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            return "未找到插件: " + pluginName;
        }

        Plugin plugManX = Bukkit.getPluginManager().getPlugin("PlugManX");
        if (plugManX != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getServer().dispatchCommand(sender, "plugman unload " + pluginName);
            });
            return "使用 PlugManX 热卸载插件: " + pluginName;
        }

        return "未安装 PlugManX，无法热卸载插件。请安装 PlugManX 或使用 Plugman 指令手动操作。";
    }

    public String hotLoadPlugin(CommandSender sender, String pluginName) {
        File pluginsFolder = new File("plugins");
        File pluginFile = new File(pluginsFolder, pluginName + ".jar");

        if (!pluginFile.exists()) {
            return "插件文件不存在: " + pluginName + ".jar";
        }

        Plugin plugManX = Bukkit.getPluginManager().getPlugin("PlugManX");
        if (plugManX != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getServer().dispatchCommand(sender, "plugman load " + pluginName);
            });
            return "使用 PlugManX 热加载插件: " + pluginName;
        }

        return "无法热加载插件，请安装 PlugManX 或重启服务器";
    }

    public String listPlugins() {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        StringBuilder sb = new StringBuilder();
        sb.append("服务器已安装 ").append(plugins.length).append(" 个插件:\n\n");

        for (Plugin p : plugins) {
            sb.append("§e").append(p.getName()).append(" §7v").append(p.getDescription().getVersion());
            sb.append(" - ").append(p.isEnabled() ? "§a已启用" : "§c已禁用");
            String desc = p.getDescription().getDescription();
            if (desc != null && !desc.isEmpty()) {
                sb.append("\n   §7").append(desc);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String executeCommand(CommandSender sender, String command) {
        try {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                return "命令执行成功: " + command;
            } else {
                java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                        future.complete(true);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
                
                future.get(30, java.util.concurrent.TimeUnit.SECONDS);
                return "命令执行成功: " + command;
            }
        } catch (java.util.concurrent.TimeoutException e) {
            return "命令执行超时: " + command;
        } catch (Exception e) {
            return "执行命令失败: " + e.getMessage();
        }
    }

    public String setGameMode(CommandSender sender, String targetPlayer, String gameMode) {
        Player player = Bukkit.getPlayerExact(targetPlayer);
        if (player == null) {
            return "未找到玩家: " + targetPlayer;
        }

        try {
            GameMode mode = GameMode.valueOf(gameMode.toUpperCase());
            player.setGameMode(mode);
            return "已将 " + targetPlayer + " 的游戏模式设置为 " + mode.toString();
        } catch (IllegalArgumentException e) {
            return "无效的游戏模式: " + gameMode + " (可用: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR)";
        }
    }

    public String setGameMode(CommandSender sender, String targetPlayer, int gameMode) {
        switch (gameMode) {
            case 0: return setGameMode(sender, targetPlayer, "SURVIVAL");
            case 1: return setGameMode(sender, targetPlayer, "CREATIVE");
            case 2: return setGameMode(sender, targetPlayer, "ADVENTURE");
            case 3: return setGameMode(sender, targetPlayer, "SPECTATOR");
            default: return "无效的游戏模式数字: " + gameMode;
        }
    }

    public String setTime(CommandSender sender, String time) {
        World world = null;
        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        long ticks;
        switch (time.toLowerCase()) {
            case "day":
            case "白天":
                ticks = 1000;
                break;
            case "night":
            case "晚上":
                ticks = 13000;
                break;
            case "noon":
            case "中午":
                ticks = 6000;
                break;
            case "midnight":
            case "午夜":
                ticks = 18000;
                break;
            case "sunrise":
            case "日出":
                ticks = 23000;
                break;
            case "sunset":
            case "日落":
                ticks = 12000;
                break;
            default:
                try {
                    ticks = Long.parseLong(time);
                } catch (NumberFormatException e) {
                    return "无效的时间设置: " + time + " (可用: day/白天, night/晚上, noon/中午, midnight/午夜, sunrise/日出, sunset/日落, 或数字)";
                }
        }

        world.setTime(ticks);
        return "已将世界时间设置为: " + time;
    }

    public String setWeather(CommandSender sender, String weather) {
        World world = null;
        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
        }

        switch (weather.toLowerCase()) {
            case "clear":
            case "晴天":
                world.setStorm(false);
                world.setThundering(false);
                return "天气已设置为晴天";
            case "rain":
            case "雨天":
                world.setStorm(true);
                world.setThundering(false);
                return "天气已设置为雨天";
            case "thunder":
            case "雷暴":
                world.setStorm(true);
                world.setThundering(true);
                return "天气已设置为雷暴";
            default:
                return "无效的天气: " + weather + " (可用: clear/晴天, rain/雨天, thunder/雷暴)";
        }
    }

    public String giveItem(CommandSender sender, String targetPlayer, String itemName, int amount) {
        Player player = Bukkit.getPlayerExact(targetPlayer);
        if (player == null) {
            return "未找到玩家: " + targetPlayer;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        "give " + targetPlayer + " " + itemName + " " + amount);
            });
            return "已给予 " + targetPlayer + " " + amount + " 个 " + itemName;
        } catch (Exception e) {
            return "给予物品失败: " + e.getMessage();
        }
    }

    public String kickPlayer(CommandSender sender, String targetPlayer, String reason) {
        Player player = Bukkit.getPlayerExact(targetPlayer);
        if (player == null) {
            return "未找到玩家: " + targetPlayer;
        }

        player.kickPlayer(reason != null ? reason : "被管理员踢出服务器");
        return "已踢出玩家: " + targetPlayer;
    }

    public String banPlayer(CommandSender sender, String targetPlayer, String reason) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetPlayer);
        if (offlinePlayer == null) {
            return "未找到玩家: " + targetPlayer;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        "ban " + targetPlayer + (reason != null ? " " + reason : ""));
            });
            return "已封禁玩家: " + targetPlayer;
        } catch (Exception e) {
            return "封禁玩家失败: " + e.getMessage();
        }
    }

    public String getPlayerInfo(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "未找到玩家: " + playerName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§e玩家信息: ").append(playerName).append("\n");
        sb.append("§7游戏模式: ").append(player.getGameMode()).append("\n");
        sb.append("§7生命值: ").append(player.getHealth()).append("/").append(player.getMaxHealth()).append("\n");
        sb.append("§7饥饿值: ").append(player.getFoodLevel()).append("\n");
        sb.append("§7等级: ").append(player.getLevel()).append("\n");
        sb.append("§7坐标: ").append(player.getLocation().getBlockX()).append(", ")
                .append(player.getLocation().getBlockY()).append(", ")
                .append(player.getLocation().getBlockZ()).append("\n");
        sb.append("§7世界: ").append(player.getWorld().getName()).append("\n");
        sb.append("§8游戏时间: ").append(player.getPlayerTime()).append(" ticks");

        return sb.toString();
    }

    public String getServerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e服务器信息:\n");
        sb.append("§7服务器版本: ").append(Bukkit.getVersion()).append("\n");
        sb.append("§7Minecraft 版本: ").append(Bukkit.getMinecraftVersion()).append("\n");
        sb.append("§7在线玩家: ").append(Bukkit.getOnlinePlayers().size()).append("/")
                .append(Bukkit.getMaxPlayers()).append("\n");
        sb.append("§7TPS: ").append(getTPS()).append("\n");
        sb.append("§7已用内存: ").append(getUsedMemory()).append(" MB\n");
        sb.append("§7可用内存: ").append(getFreeMemory()).append(" MB\n");
        sb.append("§7世界数量: ").append(Bukkit.getWorlds().size()).append("\n");
        sb.append("§7插件数量: ").append(Bukkit.getPluginManager().getPlugins().length);

        return sb.toString();
    }

    private String getTPS() {
        try {
            Object spigot = Bukkit.class.getMethod("getSpigot").invoke(null);
            double[] tps = (double[]) spigot.getClass().getMethod("getTPS").invoke(spigot);
            return String.format("%.2f, %.2f, %.2f", tps[0], tps[1], tps[2]);
        } catch (Exception e) {
            return "未知";
        }
    }

    private long getUsedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
    }

    private long getFreeMemory() {
        return Runtime.getRuntime().freeMemory() / 1024 / 1024;
    }

    public String teleportPlayer(CommandSender sender, String targetPlayer, String destPlayer) {
        Player target = Bukkit.getPlayerExact(targetPlayer);
        Player destination = Bukkit.getPlayerExact(destPlayer);

        if (target == null) {
            return "未找到玩家: " + targetPlayer;
        }
        if (destination == null) {
            return "未找到目标玩家: " + destPlayer;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                target.teleport(destination.getLocation());
            });
            return "已将 " + targetPlayer + " 传送到 " + destPlayer;
        } catch (Exception e) {
            return "传送失败: " + e.getMessage();
        }
    }

    public String teleportToCoords(CommandSender sender, String targetPlayer, double x, double y, double z) {
        Player target = Bukkit.getPlayerExact(targetPlayer);
        if (target == null) {
            return "未找到玩家: " + targetPlayer;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                target.teleport(new org.bukkit.Location(target.getWorld(), x, y, z));
            });
            return "已将 " + targetPlayer + " 传送到坐标 (" + x + ", " + y + ", " + z + ")";
        } catch (Exception e) {
            return "传送失败: " + e.getMessage();
        }
    }

    public List<String> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public String getPlayerHeldItem(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "未找到玩家: " + playerName;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return playerName + " 手中没有物品";
        }

        return playerName + " 手中持有: " + item.getType().name() + " x" + item.getAmount();
    }

    public String getPlayerBiome(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "未找到玩家: " + playerName;
        }

        World world = player.getWorld();
        org.bukkit.block.Biome biome = world.getBiome(player.getLocation());
        
        return playerName + " 当前所在群系: " + biome.name();
    }

    public String getPlayerLookingAtBlock(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "未找到玩家: " + playerName;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            return playerName + " 面前没有方块（5格范围内）";
        }

        org.bukkit.Location loc = targetBlock.getLocation();
        return playerName + " 面前的方块: " + targetBlock.getType().name() + 
               " 坐标: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    public String getPlayerDetailedInfo(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return "未找到玩家: " + playerName;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§e玩家详细信息: ").append(playerName).append("\n");
        sb.append("§7游戏模式: ").append(player.getGameMode()).append("\n");
        sb.append("§7生命值: ").append(player.getHealth()).append("/").append(player.getMaxHealth()).append("\n");
        sb.append("§7饥饿值: ").append(player.getFoodLevel()).append("\n");
        sb.append("§7等级: ").append(player.getLevel()).append("\n");
        sb.append("§7坐标: ").append(player.getLocation().getBlockX()).append(", ")
                .append(player.getLocation().getBlockY()).append(", ")
                .append(player.getLocation().getBlockZ()).append("\n");
        sb.append("§7世界: ").append(player.getWorld().getName()).append("\n");
        
        sb.append("§7群系: ").append(player.getWorld().getBiome(player.getLocation()).name()).append("\n");
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sb.append("§7手持物品: 无\n");
        } else {
            sb.append("§7手持物品: ").append(item.getType().name()).append(" x").append(item.getAmount()).append("\n");
        }
        
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            sb.append("§7面前方块: 5格范围内无方块\n");
        } else {
            org.bukkit.Location loc = targetBlock.getLocation();
            sb.append("§7面前方块: ").append(targetBlock.getType().name())
              .append(" 坐标: (").append(loc.getBlockX()).append(", ")
              .append(loc.getBlockY()).append(", ").append(loc.getBlockZ()).append(")\n");
        }

        return sb.toString();
    }
}
