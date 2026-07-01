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
import org.atrimilan.sidastuffsmp.order.OrderListing;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.sync.OrderFirebaseSync;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;

public class OrderAdminCommand {

    public static final String DESCRIPTION = "Admin commands for the orders marketplace";
    public static final Set<String> ALIASES = Set.of("ordersadmin");

    private static final String PERMISSION = "sidastuffsmp.orders.admin";

    private OrderAdminCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("ordersadmin")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("reload")
                        .executes(OrderAdminCommand::runReload))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(OrderAdminCommand::runCancel)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(OrderAdminCommand::runDelete)))
                .then(Commands.literal("expireall")
                        .executes(OrderAdminCommand::runExpireAll))
                .then(Commands.literal("list")
                        .executes(OrderAdminCommand::runListAll)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(OrderAdminCommand::runListPlayer)))
                .then(Commands.literal("stats")
                        .executes(OrderAdminCommand::runStats))
                .then(Commands.literal("sync")
                        .then(Commands.literal("now")
                                .executes(OrderAdminCommand::runSyncNow))
                        .then(Commands.literal("status")
                                .executes(OrderAdminCommand::runSyncStatus)))
                .build();
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        org.atrimilan.sidastuffsmp.order.OrderConfig.reload();
        try {
            OrderFirebaseSync.reload();
        } catch (Exception e) {
            sender.sendMessage(Chat.prefixed("Orders config reloaded. Firebase sync skipped: " + e.getMessage(), NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(Chat.prefixed("Orders config and Firebase config reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runCancel(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int id = IntegerArgumentType.getInteger(ctx, "id");

        OrderManager.CancelResult result = OrderManager.adminCancelOrder(id);
        if (result.success()) {
            sender.sendMessage(Chat.prefixed(result.message(), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDelete(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int id = IntegerArgumentType.getInteger(ctx, "id");

        boolean deleted = OrderManager.adminDeleteOrder(id);
        if (deleted) {
            sender.sendMessage(Chat.prefixed("Deleted order " + id + ".", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Chat.prefixed("Order " + id + " not found.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runExpireAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = OrderManager.adminExpireAll();
        if (count >= 0) {
            sender.sendMessage(Chat.prefixed("Force-expired " + count + " active order(s).", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Chat.prefixed("Failed to expire orders.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<OrderListing> orders = OrderManager.getAdminOrders(null, 20);
        sendOrderList(sender, orders, "Recent orders");
        return Command.SINGLE_SUCCESS;
    }

    private static int runListPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerName = StringArgumentType.getString(ctx, "player");
        List<OrderListing> orders = OrderManager.getAdminOrders(playerName, 20);
        sendOrderList(sender, orders, "Orders for " + playerName);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendOrderList(CommandSender sender, List<OrderListing> orders, String title) {
        sender.sendMessage(Chat.prefixed(title + ":", NamedTextColor.AQUA));
        if (orders.isEmpty()) {
            sender.sendMessage(Chat.prefixed("No orders found.", NamedTextColor.GRAY));
            return;
        }
        for (OrderListing o : orders) {
            String status = o.status();
            String price = OrderManager.formatPrice(o.pricePerUnit());
            String buyer = o.buyerName();
            String material = OrderManager.formatMaterialName(o.materialName());
            sender.sendMessage(Component.text(
                    "ID:" + o.id() + " | " + status + " | " + buyer + " | " + material + " | " + price + "/ea | " + o.filledQuantity() + "/" + o.quantity(),
                    status.equals("ACTIVE") ? NamedTextColor.GREEN : status.equals("COMPLETED") ? NamedTextColor.YELLOW : NamedTextColor.GRAY
            ));
        }
    }

    private static int runStats(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OrderManager.StatsResult stats = OrderManager.getStats();
        sender.sendMessage(Chat.prefixed("Orders Stats:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Total: " + stats.total(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Active: " + stats.active(), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Completed: " + stats.completed(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Expired: " + stats.expired(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Cancelled: " + stats.cancelled(), NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSyncNow(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!OrderFirebaseSync.isEnabled()) {
            sender.sendMessage(Chat.prefixed("Firebase sync is disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        OrderFirebaseSync.forceSync();
        sender.sendMessage(Chat.prefixed("Firebase orders sync triggered.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSyncStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Chat.prefixed("Orders Firebase sync: " + (OrderFirebaseSync.isEnabled() ? "Enabled" : "Disabled"),
                OrderFirebaseSync.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (OrderFirebaseSync.isEnabled()) {
            sender.sendMessage(Component.text("Last sync: " + OrderFirebaseSync.getLastSyncTime(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Last push count: " + OrderFirebaseSync.getLastPushCount(), NamedTextColor.WHITE));
            sender.sendMessage(Component.text("Last pull count: " + OrderFirebaseSync.getLastPullCount(), NamedTextColor.WHITE));
            if (OrderFirebaseSync.getLastError() != null) {
                sender.sendMessage(Chat.prefixed("Last error: " + OrderFirebaseSync.getLastError(), NamedTextColor.RED));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
