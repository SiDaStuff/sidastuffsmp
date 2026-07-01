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
import org.atrimilan.sidastuffsmp.auction.AuctionConfig;
import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.sync.AuctionFirebaseSync;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;

public class AuctionAdminCommand {

    public static final String DESCRIPTION = "Admin commands for the auction house";
    public static final Set<String> ALIASES = Set.of("ahadmin");

    private static final String PERMISSION = "sidastuffsmp.auction.admin";

    private AuctionAdminCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("ahadmin")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("reload")
                        .executes(AuctionAdminCommand::runReload))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(AuctionAdminCommand::runCancel)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(AuctionAdminCommand::runDelete)))
                .then(Commands.literal("expireall")
                        .executes(AuctionAdminCommand::runExpireAll))
                .then(Commands.literal("list")
                        .executes(AuctionAdminCommand::runListAll)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(AuctionAdminCommand::runListPlayer)))
                .then(Commands.literal("stats")
                        .executes(AuctionAdminCommand::runStats))
                .then(Commands.literal("sync")
                        .then(Commands.literal("now")
                                .executes(AuctionAdminCommand::runSyncNow))
                        .then(Commands.literal("status")
                                .executes(AuctionAdminCommand::runSyncStatus)))
                .build();
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        AuctionConfig.reload();
        try {
            AuctionFirebaseSync.reload();
        } catch (Exception e) {
            sender.sendMessage(Chat.prefixed("Auction config reloaded. Firebase sync skipped: " + e.getMessage(), NamedTextColor.YELLOW));
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(Chat.prefixed("Auction config and Firebase config reloaded.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runCancel(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int id = IntegerArgumentType.getInteger(ctx, "id");

        AuctionManager.CancelResult result = AuctionManager.adminCancelListing(id);
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

        boolean deleted = AuctionManager.adminDeleteListing(id);
        if (deleted) {
            sender.sendMessage(Chat.prefixed("Deleted listing " + id + ".", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Chat.prefixed("Listing " + id + " not found.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runExpireAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        int count = AuctionManager.adminExpireAll();
        if (count >= 0) {
            sender.sendMessage(Chat.prefixed("Force-expired " + count + " active listing(s).", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Chat.prefixed("Failed to expire listings.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        List<AuctionListing> listings = AuctionManager.getAdminListings(null, 20);
        sendListingList(sender, listings, "Recent listings");
        return Command.SINGLE_SUCCESS;
    }

    private static int runListPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerName = StringArgumentType.getString(ctx, "player");
        List<AuctionListing> listings = AuctionManager.getAdminListings(playerName, 20);
        sendListingList(sender, listings, "Listings for " + playerName);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendListingList(CommandSender sender, List<AuctionListing> listings, String title) {
        sender.sendMessage(Chat.prefixed(title + ":", NamedTextColor.AQUA));
        if (listings.isEmpty()) {
            sender.sendMessage(Chat.prefixed("No listings found.", NamedTextColor.GRAY));
            return;
        }
        for (AuctionListing l : listings) {
            String status = l.status();
            String price = AuctionManager.formatPrice(l.price());
            String seller = l.sellerName();
            sender.sendMessage(Component.text(
                    "ID:" + l.id() + " | " + status + " | " + seller + " | " + price,
                    status.equals("ACTIVE") ? NamedTextColor.GREEN : status.equals("SOLD") ? NamedTextColor.YELLOW : NamedTextColor.GRAY
            ));
        }
    }

    private static int runStats(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        AuctionManager.StatsResult stats = AuctionManager.getStats();
        sender.sendMessage(Chat.prefixed("Auction Stats:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Total: " + stats.total(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Active: " + stats.active(), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Sold: " + stats.sold(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Expired: " + stats.expired(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Cancelled: " + stats.cancelled(), NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSyncNow(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!AuctionFirebaseSync.isEnabled()) {
            sender.sendMessage(Chat.prefixed("Firebase sync is disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        AuctionFirebaseSync.forceSync();
        sender.sendMessage(Chat.prefixed("Firebase sync triggered.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSyncStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Chat.prefixed("Firebase sync: " + (AuctionFirebaseSync.isEnabled() ? "Enabled" : "Disabled"),
                AuctionFirebaseSync.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (AuctionFirebaseSync.isEnabled()) {
            sender.sendMessage(Component.text("Last sync: " + AuctionFirebaseSync.getLastSyncTime(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Last push count: " + AuctionFirebaseSync.getLastPushCount(), NamedTextColor.WHITE));
            sender.sendMessage(Component.text("Last pull count: " + AuctionFirebaseSync.getLastPullCount(), NamedTextColor.WHITE));
            if (AuctionFirebaseSync.getLastError() != null) {
                sender.sendMessage(Chat.prefixed("Last error: " + AuctionFirebaseSync.getLastError(), NamedTextColor.RED));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
