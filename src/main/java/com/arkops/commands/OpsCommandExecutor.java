package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.PermissionManager;
import com.arkops.manager.ServerActionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OpsCommandExecutor implements CommandExecutor, TabCompleter {

    private final ArkOpsAi plugin;
    private final OpsCommandHandler handler;

    public OpsCommandExecutor(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.handler = new OpsCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e§l===== ArkOps-Ai 帮助 =====");
            sender.sendMessage("§7使用 /ops <操作> 来管理服务器");
            sender.sendMessage("§7示例:");
            sender.sendMessage("§7  /ops 重启服务器");
            sender.sendMessage("§7  /ops 关闭服务器");
            sender.sendMessage("§7  /ops 热重载 Essentials");
            sender.sendMessage("§7  /ops 列出插件");
            sender.sendMessage("§7  /ops 设置游戏时间为晚上");
            sender.sendMessage("§7  /ops 调整我的游戏模式为创造");
            sender.sendMessage("§7  /ops 执行命令 give @p diamond 64");
            sender.sendMessage("§7  /ops 服务器状态");
            sender.sendMessage("§7  /ops 玩家信息 <玩家名>");
            sender.sendMessage("");
            sender.sendMessage("§7权限等级: §c禁用 §7| §b玩家 §7| §a管理员 §7| §d超级管理员");
            return true;
        }

        String fullCommand = String.join(" ", args);
        handler.handleCommand(sender, fullCommand);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> suggestions = Arrays.asList(
                    "重启服务器", "关闭服务器", "热重载", "热卸载", "热加载",
                    "列出插件", "执行命令", "设置游戏时间", "设置天气",
                    "调整游戏模式", "服务器状态", "玩家信息", "传送",
                    "给予物品", "踢出玩家", "封禁玩家", "帮助"
            );
            String input = args[0].toLowerCase();
            completions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length >= 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("调整游戏模式") || firstArg.equals("玩家信息") ||
                    firstArg.equals("传送") || firstArg.equals("给予物品") ||
                    firstArg.equals("踢出玩家") || firstArg.equals("封禁玩家")) {
                completions = plugin.getServerActionManager().getOnlinePlayers().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (firstArg.equals("热重载") || firstArg.equals("热卸载") || firstArg.equals("热加载")) {
                completions = Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
