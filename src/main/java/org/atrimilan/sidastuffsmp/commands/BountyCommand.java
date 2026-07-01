package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.bounty.BountyConfig;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;
import org.atrimilan.sidastuffsmp.gui.BountyGui;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Set;

public class BountyCommand {

    public static final String DESCRIPTION = "View and manage player bounties";
    public static final Set<String> ALIASES = Set.of("bounty", "bounties");

    private static final String PERMISSION = "sidastuffsmp.bounty.use";

    private BountyCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("bounty")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(BountyCommand::openBountyBoard)
                .then(Commands.literal("top")
                        .executes(BountyCommand::openTopBounties))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.word())
                                        .executes(BountyCommand::addBounty))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(BountyCommand::removeBounty)))
                .then(Commands.argument("player", StringArgumentType.greedyString())
                        .executes(BountyCommand::viewPlayerBounty))
                .build();
    }

    private static int openBountyBoard(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!BountyConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Bounty system is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        // Bounty board can still be viewed even without economy, just can't add bounties
        BountyGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int openTopBounties(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!BountyConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Bounty system is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        BountyGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int viewPlayerBounty(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!BountyConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Bounty system is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            player.sendMessage(Chat.prefixed("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        BountyGui.openPlayerDetail(player, target.getUniqueId());
        return Command.SINGLE_SUCCESS;
    }

    private static int addBounty(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (!BountyConfig.enabled()) {
            player.sendMessage(Chat.prefixed("Bounty system is currently disabled.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (!BountyManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available. Bounties require an economy plugin.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        String amountText = StringArgumentType.getString(ctx, "amount");
        double amount;
        try {
            amount = org.atrimilan.sidastuffsmp.utils.AmountParser.parse(amountText);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Chat.prefixed("Invalid bounty amount: " + e.getMessage(), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            player.sendMessage(Chat.prefixed("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        double total = BountyManager.addBounty(player, target, amount);
        if (total > 0) {
            BountyGui.openPlayerDetail(player, target.getUniqueId());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removeBounty(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (!BountyManager.hasEconomy()) {
            player.sendMessage(Chat.prefixed("Economy system is not available. Bounties require an economy plugin.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            player.sendMessage(Chat.prefixed("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        boolean success = BountyManager.removeAllBountiesBySetter(player, target);
        if (success) {
            BountyGui.openPlayerDetail(player, target.getUniqueId());
        }
        return Command.SINGLE_SUCCESS;
    }
}
