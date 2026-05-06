package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.LanguageManager;
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

    public OpsCommandExecutor(ArkOpsAi plugin, OpsCommandHandler handler) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.handler = handler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(lang.getMessage("command.help_header"));
            sender.sendMessage(lang.getMessage("command.help_usage"));
            sender.sendMessage(lang.getMessage("command.help_examples"));
            sender.sendMessage(lang.getMessage("command.help_example_restart"));
            sender.sendMessage(lang.getMessage("command.help_example_stop"));
            sender.sendMessage(lang.getMessage("command.help_example_reload"));
            sender.sendMessage(lang.getMessage("command.help_example_plugins"));
            sender.sendMessage(lang.getMessage("command.help_example_time"));
            sender.sendMessage(lang.getMessage("command.help_example_gamemode"));
            sender.sendMessage(lang.getMessage("command.help_example_command"));
            sender.sendMessage(lang.getMessage("command.help_example_status"));
            sender.sendMessage(lang.getMessage("command.help_example_player"));
            sender.sendMessage("");
            sender.sendMessage(lang.getMessage("command.help_permissions"));
            return true;
        }

        String fullCommand = String.join(" ", args);
        handler.handleCommand(sender, fullCommand);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String typed = String.join(" ", args).toLowerCase();

        if (args.length <= 1) {
            List<String> suggestions = Arrays.asList(
                    "restart server", "stop server", "hot-reload", "hot-unload", "hot-load",
                    "list plugins", "execute command", "set game time", "set weather",
                    "set gamemode", "server status", "player info", "teleport",
                    "give item", "kick player", "ban player", "help"
            );
            completions = suggestions.stream()
                    .filter(s -> s.startsWith(typed))
                    .collect(Collectors.toList());
        } else {
            if (typed.startsWith("set gamemode ") || typed.startsWith("player info ") ||
                    typed.startsWith("teleport ") || typed.startsWith("give item ") ||
                    typed.startsWith("kick player ") || typed.startsWith("ban player ")) {
                completions = plugin.getServerActionManager().getOnlinePlayers().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (typed.startsWith("hot-reload ") || typed.startsWith("hot-unload ") || typed.startsWith("hot-load ")) {
                completions = Arrays.stream(plugin.getServer().getPluginManager().getPlugins())
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
