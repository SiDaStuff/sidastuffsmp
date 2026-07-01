package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.economy.EconomyDatabase;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.utils.Chat;

import java.util.List;
import java.util.Set;

public class TopBalanceCommand {

    public static final String DESCRIPTION = "Show the richest players";
    public static final Set<String> ALIASES = Set.of("topbalance", "baltop");

    private static final String PERMISSION = "sidastuffsmp.economy.top";
    private static final int PER_PAGE = 10;

    private TopBalanceCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("top")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(ctx -> runTop(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> runTop(ctx, IntegerArgumentType.getInteger(ctx, "page"))))
                .build();
    }

    private static int runTop(CommandContext<CommandSourceStack> ctx, int page) {
        List<EconomyDatabase.TopEntry> topList = EconomyManager.getTopPlayers(1000);
        int totalPages = Math.max(1, (int) Math.ceil((double) topList.size() / PER_PAGE));
        page = Math.min(page, totalPages);

        ctx.getSource().getSender().sendMessage(Chat.prefixed("Top Balances (Page " + page + "/" + totalPages + "):", NamedTextColor.GOLD));

        int start = (page - 1) * PER_PAGE;
        int end = Math.min(start + PER_PAGE, topList.size());

        for (int i = start; i < end; i++) {
            EconomyDatabase.TopEntry entry = topList.get(i);
            int rank = i + 1;
            String formatted = EconomyManager.formatBalanceWithSymbol(entry.balance());
            ctx.getSource().getSender().sendMessage(Component.text(
                    rank + ". " + entry.name() + " — " + formatted,
                    rank <= 3 ? NamedTextColor.YELLOW : NamedTextColor.WHITE
            ));
        }

        return Command.SINGLE_SUCCESS;
    }
}
