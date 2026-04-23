package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.LanguageManager;
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
    private final LanguageManager lang;
    private final OpsCommandHandler handler;

    public OpsCommandExecutor(ArkOpsAi plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.handler = new OpsCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e§l===== ArkOps-Ai Help =====");
            sender.sendMessage("§7Use /ops <action> to manage the server");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("§7  /ops restart server");
            sender.sendMessage("§7  /ops stop server");
            sender.sendMessage("§7  /ops hot-reload Essentials");
            sender.sendMessage("§7  /ops list plugins");
            sender.sendMessage("§7  /ops set game time to night");
            sender.sendMessage("§7  /ops set my gamemode to creative");
            sender.sendMessage("§7  /ops execute command give @p diamond 64");
            sender.sendMessage("§7  /ops server status");
            sender.sendMessage("§7  /ops player info <player>");
            sender.sendMessage("");
            sender.sendMessage("§7Permission levels: §cDisabled §7| §bPlayer §7| §aAdmin §7| §dSuper Admin");
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
                    "restart server", "stop server", "hot-reload", "hot-unload", "hot-load",
                    "list plugins", "execute command", "set game time", "set weather",
                    "set gamemode", "server status", "player info", "teleport",
                    "give item", "kick player", "ban player", "help"
            );
            String input = args[0].toLowerCase();
            completions = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length >= 2) {
            String firstArg = args[0].toLowerCase();

            if (firstArg.equals("set gamemode") || firstArg.equals("player info") ||
                    firstArg.equals("teleport") || firstArg.equals("give item") ||
                    firstArg.equals("kick player") || firstArg.equals("ban player")) {
                completions = plugin.getServerActionManager().getOnlinePlayers().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (firstArg.equals("hot-reload") || firstArg.equals("hot-unload") || firstArg.equals("hot-load")) {
                completions = Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
