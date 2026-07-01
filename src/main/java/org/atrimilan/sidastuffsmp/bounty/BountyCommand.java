package org.atrimilan.sidastuffsmp.bounty;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.Set;

public class BountyCommand {

    public static final String DESCRIPTION = "Manage player bounties";
    public static final Set<String> ALIASES = Set.of("bounties");

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("bounty")
                .then(Commands.literal("add")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.greedyString())
                                        .executes(BountyCommand::runAdd))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .then(Commands.argument("amount", StringArgumentType.greedyString())
                                        .executes(BountyCommand::runRemove))))
                .executes(BountyCommand::runMain)
                .build();
    }

    private static int runMain(CommandContext<CommandSourceStack> ctx) {
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

    private static int runAdd(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "target");
        String amountStr = StringArgumentType.getString(ctx, "amount");

        player.sendMessage(Chat.prefixed("DEBUG: target=" + targetName + " amount=" + amountStr, NamedTextColor.YELLOW));

        try {
            double amount = Double.parseDouble(amountStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            player.sendMessage(Chat.prefixed("DEBUG: parsed amount=" + amount + " target=" + target.getName(), NamedTextColor.YELLOW));

            if (target == null) {
                player.sendMessage(Chat.prefixed("Player not found!", NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }

            double result = BountyManager.addBounty(player, target, amount);
            player.sendMessage(Chat.prefixed("DEBUG: addBounty returned=" + result, NamedTextColor.YELLOW));
            if (result == -1) {
                // BountyManager already sends error messages
            } else {
                player.sendMessage(Chat.prefixed("You placed a " + BountyManager.formatPrice(amount) + " bounty on " + target.getName() + "! Total: " + BountyManager.formatPrice(result), NamedTextColor.GRAY));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Chat.prefixed("Invalid amount!", NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int runRemove(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "target");
        String amountStr = StringArgumentType.getString(ctx, "amount");
        
        try {
            double amount = Double.parseDouble(amountStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (target == null) {
                player.sendMessage(Chat.prefixed("Player not found!", NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }

            BountyManager.removeBounty(player, target, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(Chat.prefixed("Invalid amount!", NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }
}
