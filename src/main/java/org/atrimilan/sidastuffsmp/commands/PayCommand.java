package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.economy.EconomyDatabase;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.utils.AmountParser;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public class PayCommand {

    public static final String DESCRIPTION = "Pay another player";
    public static final Set<String> ALIASES = Set.of("send");

    private static final String PERMISSION = "sidastuffsmp.economy.pay";

    private PayCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("pay")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(PayCommand::runPay)))
                .build();
    }

    private static int runPay(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player sender)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetArg = StringArgumentType.getString(ctx, "player");
        double amount;
        try {
            amount = AmountParser.parse(StringArgumentType.getString(ctx, "amount"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Chat.prefixed(e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (amount <= 0) {
            sender.sendMessage(Chat.prefixed("Amount must be positive.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = EconomyManager.resolveUuid(targetArg);
        if (targetUuid == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + targetArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (targetUuid.equals(senderUuid)) {
            sender.sendMessage(Chat.prefixed("You cannot pay yourself.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!EconomyManager.hasAccount(targetUuid)) {
            String targetName = EconomyManager.getPlayerName(targetUuid);
            if (targetName == null) targetName = targetArg;
            EconomyManager.createAccount(targetUuid, targetName);
        }

        Connection conn = EconomyDatabase.getConnection();
        boolean transactionOk = false;
        try {
            if (conn != null && !conn.isClosed()) {
                conn.setAutoCommit(false);
            }
        } catch (SQLException ignored) {}

        EconomyManager.EconomyResult withdrawResult = EconomyManager.withdraw(senderUuid, amount, "PAYMENT_SENT", targetUuid, targetArg, "Payment to " + targetArg);
        if (!withdrawResult.success()) {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignored) {}
            sender.sendMessage(Chat.prefixed(withdrawResult.errorMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        EconomyManager.EconomyResult depositResult = EconomyManager.deposit(targetUuid, amount, "PAYMENT_RECEIVED", senderUuid, sender.getName(), "Payment from " + sender.getName());
        if (!depositResult.success()) {
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ignored) {}
            EconomyManager.invalidateCache(senderUuid);
            EconomyManager.invalidateCache(targetUuid);
            sender.sendMessage(Chat.prefixed("Payment failed: " + depositResult.errorMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        try {
            if (conn != null) {
                conn.commit();
                conn.setAutoCommit(true);
            }
            transactionOk = true;
        } catch (SQLException e) {
            try { if (conn != null) { conn.rollback(); conn.setAutoCommit(true); } } catch (SQLException ignored) {}
            EconomyManager.invalidateCache(senderUuid);
            EconomyManager.invalidateCache(targetUuid);
            sender.sendMessage(Chat.prefixed("Payment failed due to a database error. Contact an admin.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = EconomyManager.getPlayerName(targetUuid);
        if (targetName == null) targetName = targetArg;

        sender.sendMessage(Chat.prefixed("Paid " + EconomyManager.formatBalanceWithSymbol(amount) + " to " + targetName + ".", NamedTextColor.GREEN));
        sender.sendMessage(Chat.prefixed("New balance: " + EconomyManager.formatBalanceWithSymbol(withdrawResult.newBalance()), NamedTextColor.GRAY));

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(Chat.prefixed("Received " + EconomyManager.formatBalanceWithSymbol(amount) + " from " + sender.getName() + ".", NamedTextColor.GREEN));
            targetPlayer.sendMessage(Chat.prefixed("New balance: " + EconomyManager.formatBalanceWithSymbol(depositResult.newBalance()), NamedTextColor.GRAY));
        }

        return Command.SINGLE_SUCCESS;
    }
}
