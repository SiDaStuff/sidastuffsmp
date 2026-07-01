package org.atrimilan.sidastuffsmp.rtp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;

public final class RtpCommand {

    public static final String RTP_DESCRIPTION = "Randomly teleport to a dimension";
    public static final Set<String> RTP_ALIASES = Set.of("randomteleport", "wilderness", "wild");
    public static final String RTP_SUDO_DESCRIPTION = "Force a player to RTP";
    public static final Set<String> RTP_SUDO_ALIASES = Set.of("rtpforce", "rtpadmin");

    private static final String RTP_PERMISSION = "sidastuffsmp.rtp";
    private static final String RTP_SUDO_PERMISSION = "sidastuffsmp.rtp.sudo";

    private RtpCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createRtpCommand() {
        return Commands.literal("rtp")
                .requires(sender -> sender.getSender().hasPermission(RTP_PERMISSION))
                .executes(RtpCommand::runRtp)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createRtpSudoCommand() {
        return Commands.literal("rtpsudo")
                .requires(sender -> sender.getSender().hasPermission(RTP_SUDO_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYER_SUGGESTIONS)
                        .then(Commands.argument("dimension", StringArgumentType.word())
                                .suggests(DIMENSION_SUGGESTIONS)
                                .executes(RtpCommand::runRtpSudo))
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /rtpsudo <player> <dimension>", NamedTextColor.YELLOW));
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /rtpsudo <player> <dimension>", NamedTextColor.YELLOW));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYER_SUGGESTIONS = (ctx, builder) -> {
        for (Player p : Bukkit.getOnlinePlayers()) {
            builder.suggest(p.getName());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> DIMENSION_SUGGESTIONS = (ctx, builder) -> {
        builder.suggest("overworld");
        builder.suggest("nether");
        builder.suggest("end");
        return builder.buildFuture();
    };

    private static int runRtp(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        RtpGui.open(player);
        return Command.SINGLE_SUCCESS;
    }

    private static int runRtpSudo(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        String dimension = StringArgumentType.getString(ctx, "dimension").toLowerCase(java.util.Locale.ROOT);

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null || !target.isOnline()) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found or offline.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        RtpConfig.RegionConfig region = switch (dimension) {
            case "overworld" -> RtpConfig.getRegion("overworld-region");
            case "nether" -> RtpConfig.getRegion("nether-region");
            case "end" -> RtpConfig.getRegion("end-region");
            default -> null;
        };

        if (region == null) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Unknown dimension '" + dimension + "'. Use overworld, nether, or end.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        boolean started = RtpManager.startCountdown(target, region, true);
        if (started) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Forcing " + target.getName() + " to RTP to the " + dimension + ".", NamedTextColor.GREEN));
        }
        return Command.SINGLE_SUCCESS;
    }
}