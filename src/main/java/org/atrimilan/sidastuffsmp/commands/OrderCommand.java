package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.gui.CreateOrderGui;
import org.atrimilan.sidastuffsmp.gui.OrderBrowserGui;
import org.atrimilan.sidastuffsmp.gui.OrderGuiHolder;
import org.atrimilan.sidastuffsmp.gui.OrderMyOrdersGui;
import org.atrimilan.sidastuffsmp.order.OrderConfig;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.order.OrderSortMode;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.entity.Player;

import java.util.Set;

public class OrderCommand {

    public static final String DESCRIPTION = "Open the orders marketplace";
    public static final Set<String> ALIASES = Set.of("order");

    private static final String PERMISSION = "sidastuffsmp.orders.use";

    private OrderCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("orders")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(OrderCommand::runBrowser)
                .then(Commands.literal("new")
                        .executes(OrderCommand::runNew))
                .then(Commands.literal("create")
                        .executes(OrderCommand::runNew))
                .then(Commands.literal("search")
                        .then(Commands.argument("search", StringArgumentType.greedyString())
                                .executes(OrderCommand::runSearch)))
                .then(Commands.literal("my")
                        .executes(OrderCommand::runMy))
                .build();
    }

    private static int runBrowser(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Orders marketplace is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        OrderBrowserGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int runNew(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Orders marketplace is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        // Initialize the new order state and open create order GUI
        OrderGuiHolder.NewOrderState state = OrderGuiHolder.getNewOrderState(player.getUniqueId());
        state.setStep(0);
        state.setSelectedMaterial(null);
        state.setQuantity(0);
        state.setPricePerUnit(0);
        CreateOrderGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int runSearch(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Orders marketplace is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String term = StringArgumentType.getString(ctx, "search");
        OrderBrowserGui.open(player, 0, OrderSortMode.NEWEST, term);
        return Command.SINGLE_SUCCESS;
    }

    private static int runMy(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!OrderConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Orders marketplace is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        OrderMyOrdersGui.open(player, 0);
        return Command.SINGLE_SUCCESS;
    }
}
