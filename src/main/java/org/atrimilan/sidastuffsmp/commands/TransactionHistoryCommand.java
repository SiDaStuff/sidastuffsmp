package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.economy.TransactionRecord;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TransactionHistoryCommand {

    public static final String DESCRIPTION = "Show transaction history";
    public static final Set<String> ALIASES = Set.of("hist");

    private static final String PERMISSION = "sidastuffsmp.economy.history";
    private static final String ADMIN_PERMISSION = "sidastuffsmp.economy.admin";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private TransactionHistoryCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("history")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(ctx -> runSelfHistory(ctx, 10))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> runOtherHistory(ctx, 10))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> runOtherHistory(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))
                .build();
    }

    private static int runSelfHistory(CommandContext<CommandSourceStack> ctx, int amount) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        sendHistory(ctx.getSource().getSender(), player.getUniqueId(), player.getName(), amount);
        return Command.SINGLE_SUCCESS;
    }

    private static int runOtherHistory(CommandContext<CommandSourceStack> ctx, int amount) {
        CommandSender sender = ctx.getSource().getSender();
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Chat.prefixed("You don't have permission to view other players' history.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

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

        String name = EconomyManager.getPlayerName(targetUuid);
        if (name == null) name = playerArg;

        sendHistory(sender, targetUuid, name, amount);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendHistory(CommandSender sender, UUID uuid, String name, int amount) {
        List<TransactionRecord> transactions = EconomyManager.getTransactions(uuid, amount);
        sender.sendMessage(Chat.prefixed("Transaction history for " + name + " (showing " + transactions.size() + "):", NamedTextColor.AQUA));

        if (transactions.isEmpty()) {
            sender.sendMessage(Chat.prefixed("No transactions found.", NamedTextColor.GRAY));
            return;
        }

        for (TransactionRecord record : transactions) {
            String date = DATE_FORMAT.format(new Date(record.timestamp()));
            String type = record.type();
            String formattedAmount = EconomyManager.formatBalanceWithSymbol(record.amount());
            String target = record.targetName() != null ? " → " + record.targetName() : "";
            String desc = record.description() != null ? " — " + record.description() : "";

            NamedTextColor color = switch (type) {
                case "PAYMENT_SENT", "WITHDRAW", "PAYMENT_ADMIN_TAKE", "AUCTION_LISTING_FEE", "AUCTION_PURCHASE" -> NamedTextColor.RED;
                case "PAYMENT_RECEIVED", "DEPOSIT", "PAYMENT_ADMIN_GIVE", "AUCTION_SOLD" -> NamedTextColor.GREEN;
                default -> NamedTextColor.WHITE;
            };

            boolean isCredit = record.amount() > 0 && (type.equals("DEPOSIT") || type.equals("PAYMENT_RECEIVED") || type.equals("PAYMENT_ADMIN_GIVE") || type.equals("AUCTION_SOLD") || type.equals("REFUND"));
            String prefix = isCredit ? "+" : "-";

            sender.sendMessage(Component.text("[" + date + "] " + type + target + ": " + prefix + formattedAmount + desc, color));
        }
    }
}
