package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.ConfigManager;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class SetMessageCommand {

    public static final String DESCRIPTION = "Manage the join book message";
    public static final Set<String> ALIASES = Set.of("joinmessage");
    private static final String PERMISSION = "sidastuffsmp.setmessage";

    private SetMessageCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("setmessage")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("on").executes(SetMessageCommand::runOn))
                .then(Commands.literal("off").executes(SetMessageCommand::runOff))
                .then(Commands.literal("set")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(SetMessageCommand::runSet)))
                .executes(SetMessageCommand::runHelp)
                .build();
    }

    private static int runOn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        ConfigManager.setJoinMessageEnabled(true);
        sender.sendMessage(Chat.prefixed("Join message book is now ON.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runOff(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        ConfigManager.setJoinMessageEnabled(false);
        sender.sendMessage(Chat.prefixed("Join message book is now OFF.", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSet(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String message = StringArgumentType.getString(ctx, "message").trim();
        if (message.isBlank()) {
            sender.sendMessage(Chat.prefixed("Message cannot be empty.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        ConfigManager.setJoinMessageText(message);
        sender.sendMessage(Chat.prefixed("Join message book text updated.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(Chat.prefixed("Usage: /setmessage set <message>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Usage: /setmessage on", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Usage: /setmessage off", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }
}
