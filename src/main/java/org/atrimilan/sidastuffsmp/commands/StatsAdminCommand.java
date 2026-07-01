package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.stats.PlayerStatsManager;
import org.atrimilan.sidastuffsmp.sync.StatsFirebaseSync;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class StatsAdminCommand {

    public static final String DESCRIPTION = "Admin commands for player statistics";
    public static final Set<String> ALIASES = Set.of();
    private static final String PERMISSION = "sidastuffsmp.statsadmin";

    private StatsAdminCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("statsadmin")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("sync")
                        .then(Commands.literal("now")
                                .executes(StatsAdminCommand::syncNow)))
                .then(Commands.literal("status")
                        .executes(StatsAdminCommand::status))
                .then(Commands.literal("wipe")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(StatsAdminCommand::wipePlayer)))
                .build();
    }

    private static int syncNow(CommandContext<CommandSourceStack> ctx) {
        StatsFirebaseSync.performSync();
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Chat.prefixed("Stats sync initiated.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = StatsFirebaseSync.isEnabled();
        String lastSync = StatsFirebaseSync.getLastSyncTime();
        int pushCount = StatsFirebaseSync.getLastPushCount();
        String lastError = StatsFirebaseSync.getLastError();
        CommandSender sender = ctx.getSource().getSender();

        sender.sendMessage(Chat.prefixed("Stats Firebase Sync Status:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(" Enabled: ").color(NamedTextColor.GRAY)
                .append(Component.text(enabled ? "Yes" : "No")
                        .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text(" Last Sync: ").color(NamedTextColor.GRAY)
                .append(Component.text(lastSync).color(NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text(" Last Push Count: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(pushCount)).color(NamedTextColor.YELLOW)));
        if (lastError != null) {
            sender.sendMessage(Component.text(" Last Error: ").color(NamedTextColor.GRAY)
                    .append(Component.text(lastError).color(NamedTextColor.RED)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int wipePlayer(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        CommandSender sender = ctx.getSource().getSender();

        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerName, NamedTextColor.RED));
            return 0;
        }
        PlayerStatsManager.wipeStats(offlinePlayer.getUniqueId());
        sender.sendMessage(Chat.prefixed("Wiped stats for player: " + playerName, NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}