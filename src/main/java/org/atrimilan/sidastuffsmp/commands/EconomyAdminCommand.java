package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.economy.EconomyConfig;
import org.atrimilan.sidastuffsmp.economy.EconomyDatabase;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.utils.AmountParser;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.UUID;

public class EconomyAdminCommand {

    public static final String DESCRIPTION = "Admin commands for the economy system";
    public static final Set<String> ALIASES = Set.of();

    private static final String PERMISSION = "sidastuffsmp.economy.admin";
    private static final String RELOAD_PERMISSION = "sidastuffsmp.economy.reload";

    private EconomyAdminCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("economy")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.word())
                                        .executes(EconomyAdminCommand::runSet))))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.word())
                                        .executes(EconomyAdminCommand::runGive))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.word())
                                        .executes(EconomyAdminCommand::runTake))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(EconomyAdminCommand::runReset)))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createReloadCommand() {
        return Commands.literal("economyreload")
                .requires(sender -> sender.getSender().hasPermission(RELOAD_PERMISSION))
                .executes(EconomyAdminCommand::runReload)
                .build();
    }

    private static int runSet(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        double amount;
        try {
            amount = AmountParser.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Chat.prefixed(e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUuid = EconomyManager.resolveUuid(playerArg);
        if (targetUuid == null || !EconomyManager.hasAccount(targetUuid)) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        EconomyManager.setBalance(targetUuid, amount);
        EconomyDatabase.addTransaction(targetUuid, "ADMIN_SET", amount, null, sender.getName(), "Balance set by " + sender.getName());

        sender.sendMessage(Chat.prefixed("Set " + name + "'s balance to " + EconomyManager.formatBalanceWithSymbol(amount) + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runGive(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        double amount;
        try {
            amount = AmountParser.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Chat.prefixed(e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUuid = EconomyManager.resolveUuid(playerArg);
        if (targetUuid == null || !EconomyManager.hasAccount(targetUuid)) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        EconomyManager.EconomyResult result = EconomyManager.deposit(targetUuid, amount, "ADMIN_GIVE", null, sender.getName(), "Given by " + sender.getName());
        if (!result.success()) {
            sender.sendMessage(Chat.prefixed("Failed: " + result.errorMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Chat.prefixed("Gave " + EconomyManager.formatBalanceWithSymbol(amount) + " to " + name + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runTake(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        double amount;
        try {
            amount = AmountParser.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Chat.prefixed(e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        UUID targetUuid = EconomyManager.resolveUuid(playerArg);
        if (targetUuid == null || !EconomyManager.hasAccount(targetUuid)) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        EconomyManager.EconomyResult result = EconomyManager.withdraw(targetUuid, amount, "ADMIN_TAKE", null, sender.getName(), "Taken by " + sender.getName());
        if (!result.success()) {
            sender.sendMessage(Chat.prefixed("Failed: " + result.errorMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Chat.prefixed("Took " + EconomyManager.formatBalanceWithSymbol(amount) + " from " + name + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runReset(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");

        UUID targetUuid = EconomyManager.resolveUuid(playerArg);
        if (targetUuid == null || !EconomyManager.hasAccount(targetUuid)) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        double startingBalance = EconomyConfig.startingBalance();
        EconomyDatabase.setBalance(targetUuid, startingBalance);
        EconomyManager.invalidateCache(targetUuid);
        EconomyDatabase.addTransaction(targetUuid, "ADMIN_RESET", startingBalance, null, sender.getName(), "Balance reset by " + sender.getName());

        sender.sendMessage(Chat.prefixed("Reset " + name + "'s balance to " + EconomyManager.formatBalanceWithSymbol(startingBalance) + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        EconomyConfig.reload();
        sender.sendMessage(Chat.prefixed("Economy config reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
