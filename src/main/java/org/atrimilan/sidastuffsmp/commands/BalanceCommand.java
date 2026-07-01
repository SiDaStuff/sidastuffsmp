package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class BalanceCommand {

    public static final String DESCRIPTION = "Show your or another player's balance";
    public static final Set<String> ALIASES = Set.of("bal", "money");

    private static final String PERMISSION = "sidastuffsmp.economy.balance";

    private BalanceCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("balance")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(BalanceCommand::runSelfBalance)
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(BalanceCommand::runOtherBalance))
                .build();
    }

    private static int runSelfBalance(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!EconomyManager.hasAccount(player.getUniqueId())) {
            player.sendMessage(Chat.prefixed("You do not have an economy account.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        double balance = EconomyManager.getBalance(player.getUniqueId());
        player.sendMessage(Chat.prefixed("Your balance: " + EconomyManager.formatBalanceWithSymbol(balance), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runOtherBalance(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");

        UUID targetUuid = EconomyManager.resolveUuid(playerArg);
        if (targetUuid == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!EconomyManager.hasAccount(targetUuid)) {
            sender.sendMessage(Chat.prefixed(playerArg + " does not have an economy account.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        double balance = EconomyManager.getBalance(targetUuid);
        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        sender.sendMessage(Chat.prefixed(name + "'s balance: " + EconomyManager.formatBalanceWithSymbol(balance), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
