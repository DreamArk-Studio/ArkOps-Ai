package com.arkops.commands;

import com.arkops.ArkOpsAi;
import com.arkops.manager.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OpsGuiCommand implements Listener {

    private final ArkOpsAi plugin;
    private final OpsCommandHandler handler;
    private final Map<UUID, SignSession> sessions = new HashMap<>();

    public OpsGuiCommand(ArkOpsAi plugin, OpsCommandHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    public void openGui(Player player) {
        PermissionManager.PermissionLevel level = plugin.getPermissionManager().getPermissionLevel(player.getUniqueId());

        if (level == PermissionManager.PermissionLevel.DISABLED) {
            player.sendMessage("§c你没有权限使用 ArkOps-Ai");
            return;
        }

        Location loc = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
        Block targetBlock = loc.getBlock();

        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR && targetBlock.getType() != Material.VOID_AIR) {
            loc = loc.add(0, 1, 0);
            targetBlock = loc.getBlock();
        }

        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR && targetBlock.getType() != Material.VOID_AIR) {
            player.sendMessage("§c无法找到合适的位置放置告示牌，请确保周围有空间");
            return;
        }

        targetBlock.setType(Material.OAK_SIGN);
        Sign sign = (Sign) targetBlock.getState();
        sign.setLine(0, "§6§lArkOps-Ai");
        sign.setLine(1, "§f请输入问题");
        sign.setLine(2, "§7编辑此告示牌");
        sign.setLine(3, "§7完成后离开");
        sign.update();

        sessions.put(player.getUniqueId(), new SignSession(level, targetBlock.getLocation()));

        player.sendMessage("§7[§eArkOps-Ai§7] §f请编辑告示牌输入问题，完成后§a离开告示牌§f即可提交");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.openSign(sign);
        }, 1L);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        SignSession session = sessions.remove(playerId);
        if (session == null) return;

        if (!event.getBlock().getLocation().equals(session.signLocation())) return;

        StringBuilder input = new StringBuilder();
        for (String line : event.getLines()) {
            if (line != null && !line.isEmpty()) {
                if (input.length() > 0) {
                    input.append(" ");
                }
                input.append(line);
            }
        }

        String question = input.toString().trim();
        if (question.isEmpty()) {
            player.sendMessage("§c输入内容不能为空");
            return;
        }

        player.sendMessage("§7[§eArkOps-Ai§7] §f正在处理: §e" + question);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String response = handler.handleQQMessageWithResponse(
                    playerId.toString(),
                    question,
                    session.level().name()
            );

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage("§7[§eArkOps-Ai§7] §f" + response);
            });
        });

        event.getBlock().setType(Material.AIR);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SignSession session = sessions.remove(playerId);
        if (session != null) {
            Block block = session.signLocation().getBlock();
            if (block.getType() == Material.OAK_SIGN) {
                block.setType(Material.AIR);
            }
        }
    }

    private record SignSession(PermissionManager.PermissionLevel level, Location signLocation) {}
}
