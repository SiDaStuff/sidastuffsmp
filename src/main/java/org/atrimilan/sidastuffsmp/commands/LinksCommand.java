package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.utils.Chat;

import java.util.Set;

public class LinksCommand {

    public static final String MEDIA_DESCRIPTION = "Show the media application link";
    public static final Set<String> MEDIA_ALIASES = Set.of();
    public static final String APPLY_DESCRIPTION = "Show the staff application link";
    public static final Set<String> APPLY_ALIASES = Set.of();

    private static final String MEDIA_URL = "https://apply.sidastuff.com/media";
    private static final String APPLY_URL = "https://apply.sidastuff.com/mc";

    private LinksCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createMediaCommand() {
        return Commands.literal("media")
                .requires(sender -> sender.getSender().hasPermission("sidastuffsmp.links"))
                .executes(LinksCommand::runMedia)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createApplyCommand() {
        return Commands.literal("apply")
                .requires(sender -> sender.getSender().hasPermission("sidastuffsmp.links"))
                .executes(LinksCommand::runApply)
                .build();
    }

    private static int runMedia(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(buildLinkMessage(
                "Apply for media here: ",
                "apply.sidastuff.com/media",
                MEDIA_URL
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static int runApply(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(buildLinkMessage(
                "Apply for staff here: ",
                "apply.sidastuff.com/mc",
                APPLY_URL
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static Component buildLinkMessage(String prefix, String visibleUrl, String targetUrl) {
        return Chat.prefix()
                .append(Component.text(prefix, NamedTextColor.WHITE))
                .append(Component.text(visibleUrl, NamedTextColor.AQUA)
                        .decoration(TextDecoration.UNDERLINED, true)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
                        .clickEvent(ClickEvent.openUrl(targetUrl))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open.", NamedTextColor.GRAY))))
                .append(Component.text(".", NamedTextColor.WHITE));
    }
}
