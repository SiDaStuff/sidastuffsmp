package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.WarnManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;

public class WarnCommand {

    public static final String DESCRIPTION = "Warn a player (online or offline)";
    public static final Set<String> ALIASES = Set.of();
    private static final String PERMISSION = "sidastuffsmp.warn";

    private WarnCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("warn")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(WarnCommand::runWarn)))
                .build();
    }

    private static int runWarn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player").trim();
        String message = StringArgumentType.getString(ctx, "message").trim();

        if (message.isBlank()) {
            sender.sendMessage(Chat.prefixed("Warning message cannot be empty.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Player online = Bukkit.getPlayerExact(playerArg);
        if (online != null) {
            showWarningPopup(online, message);
            sender.sendMessage(Chat.prefixed("Warned " + online.getName() + " (online).", NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }

        OfflinePlayer target = resolveOfflinePlayer(playerArg);
        WarnManager.addWarning(target, message);
        String displayName = target.getName() != null ? target.getName() : playerArg;
        sender.sendMessage(Chat.prefixed("Queued warning for " + displayName + ". It will pop up on next join.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    public static void showWarningPopup(Player player, String warningMessage) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("Warning", NamedTextColor.RED),
                Component.text(warningMessage, NamedTextColor.YELLOW),
                net.kyori.adventure.title.Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(400))
        ));
        player.sendMessage(Chat.prefixed(Component.text("[Warning] ", NamedTextColor.RED)
                .append(Component.text(warningMessage, NamedTextColor.YELLOW))));
    }

    private static OfflinePlayer resolveOfflinePlayer(String playerName) {
        for (OfflinePlayer cached : Bukkit.getOfflinePlayers()) {
            if (cached.getName() != null && cached.getName().equalsIgnoreCase(playerName)) {
                return cached;
            }
        }
        return Bukkit.getOfflinePlayer(playerName);
    }
}
