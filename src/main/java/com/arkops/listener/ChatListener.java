package com.arkops.listener;

import com.arkops.ArkOpsAi;
import com.arkops.commands.OpsCommandHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final ArkOpsAi plugin;
    private final OpsCommandHandler commandHandler;

    public ChatListener(ArkOpsAi plugin, OpsCommandHandler commandHandler) {
        this.plugin = plugin;
        this.commandHandler = commandHandler;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-trigger.enabled", true)) {
            return;
        }

        String message = event.getMessage();
        String trigger = plugin.getConfig().getString("chat-trigger.trigger", "@ops");

        if (message.startsWith(trigger + " ") || message.equals(trigger)) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            String command = message.substring(trigger.length()).trim();

            if (command.isEmpty()) {
                player.sendMessage("§e§l===== ArkOps-Ai Chat Trigger =====");
                player.sendMessage("§7Usage: " + trigger + " <your question or command>");
                player.sendMessage("§7Example: " + trigger + " server status");
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                commandHandler.handleBroadcastCommand(player, command);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        commandHandler.cleanupPlayerData(event.getPlayer().getUniqueId());
    }
}
