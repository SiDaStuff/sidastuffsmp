package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.auction.AuctionCategory;
import org.atrimilan.sidastuffsmp.auction.AuctionConfig;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.gui.AuctionBrowserGui;
import org.atrimilan.sidastuffsmp.gui.AuctionCreateGui;
import org.atrimilan.sidastuffsmp.gui.AuctionMyListingsGui;
import org.atrimilan.sidastuffsmp.teleport.PlayerSettings;
import org.atrimilan.sidastuffsmp.utils.AmountParser;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.entity.Player;

import java.util.Set;

public class AuctionCommand {

    public static final String DESCRIPTION = "Open the auction house";
    public static final Set<String> ALIASES = Set.of("auctionhouse", "ahouse");

    private static final String PERMISSION = "sidastuffsmp.auction.use";

    private AuctionCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("ah")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(AuctionCommand::runBrowser)
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(AuctionCommand::runSearchDirect))
                .then(Commands.literal("sell")
                        .then(Commands.argument("price", StringArgumentType.word())
                                .executes(AuctionCommand::runSell)))
                .then(Commands.literal("search")
                        .then(Commands.argument("term", StringArgumentType.greedyString())
                                .executes(AuctionCommand::runSearch)))
                .then(Commands.literal("my")
                        .executes(AuctionCommand::runMy))
                .then(Commands.literal("category")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .executes(AuctionCommand::runCategory)))
                .build();
    }

    private static int runBrowser(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        AuctionBrowserGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int runSell(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        double price;
        try {
            price = AmountParser.parse(StringArgumentType.getString(ctx, "price"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Chat.prefixed(e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (price <= 0) {
            player.sendMessage(Chat.prefixed("Price must be positive.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        // Determine whether to show the confirmation GUI based on both the global config
        // and the player's personal setting. This prevents the GUI from appearing if the
        // server admin has disabled it globally, even if a player has it enabled.
        boolean globalConfirm = org.atrimilan.sidastuffsmp.utils.ConfigManager.isListConfirmEnabled();
        boolean playerConfirm = PlayerSettings.get(player.getUniqueId()).isConfirmAuctionListing();
        if (globalConfirm && playerConfirm) {
            AuctionCreateGui.openCreateConfirm(player, price);
        } else {
            AuctionManager.ListResult result = AuctionManager.createListing(player, player.getInventory().getItemInMainHand(), price);
            player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (result.success()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int runSearch(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String term = StringArgumentType.getString(ctx, "term");
        AuctionBrowserGui.open(player, 0, org.atrimilan.sidastuffsmp.auction.AuctionSortMode.NEWEST, term);
        return Command.SINGLE_SUCCESS;
    }

    private static int runSearchDirect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String term = StringArgumentType.getString(ctx, "search");
        AuctionBrowserGui.open(player, 0, org.atrimilan.sidastuffsmp.auction.AuctionSortMode.NEWEST, term);
        return Command.SINGLE_SUCCESS;
    }

    private static int runMy(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        AuctionMyListingsGui.open(player, 0);
        return Command.SINGLE_SUCCESS;
    }

    private static int runCategory(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!AuctionConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Auction house is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage(Chat.prefixed("Categories have been removed. Use /ah search <term> instead.", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }
}
