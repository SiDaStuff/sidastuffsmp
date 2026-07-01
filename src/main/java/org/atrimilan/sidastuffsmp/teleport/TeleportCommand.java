package org.atrimilan.sidastuffsmp.teleport;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class TeleportCommand {

    public static final String TPA_DESCRIPTION = "Send a teleport request to a player";
    public static final Set<String> TPA_ALIASES = Set.of("teleportask");
    public static final String TPAHERE_DESCRIPTION = "Request a player to teleport to you";
    public static final Set<String> TPAHERE_ALIASES = Set.of("teleportaskhere");
    public static final String TPAACCEPT_DESCRIPTION = "Accept a pending teleport request";
    public static final Set<String> TPAACCEPT_ALIASES = Set.of("tpaccept");
    public static final String TPADENY_DESCRIPTION = "Deny a pending teleport request";
    public static final Set<String> TPADENY_ALIASES = Set.of("tpadeny");
    public static final String TPAAUTO_DESCRIPTION = "Toggle automatic acceptance of teleport requests";
    public static final Set<String> TPAAUTO_ALIASES = Set.of("tpatoggle");
    public static final String SETTINGS_DESCRIPTION = "Open player settings GUI";
    public static final Set<String> SETTINGS_ALIASES = Set.of("options");

    private static final String TPA_PERMISSION = "sidastuffsmp.tpa";
    private static final String TPA_ACCEPT_PERMISSION = "sidastuffsmp.tpa.accept";
    private static final String TPA_AUTO_PERMISSION = "sidastuffsmp.tpa.auto";
    private static final String SETTINGS_PERMISSION = "sidastuffsmp.settings";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYER_SUGGESTIONS = (ctx, builder) -> {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getName().equals(ctx.getSource().getSender().getName())) {
                builder.suggest(p.getName());
            }
        }
        return builder.buildFuture();
    };

    private TeleportCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createTpaCommand() {
        return Commands.literal("tpa")
                .requires(sender -> sender.getSender().hasPermission(TPA_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYER_SUGGESTIONS)
                        .executes(TeleportCommand::runTpa))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /tpa <player>", NamedTextColor.YELLOW));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createTpaHereCommand() {
        return Commands.literal("tpahere")
                .requires(sender -> sender.getSender().hasPermission(TPA_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYER_SUGGESTIONS)
                        .executes(TeleportCommand::runTpaHere))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /tpahere <player>", NamedTextColor.YELLOW));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createTpaAcceptCommand() {
        return Commands.literal("tpaaccept")
                .requires(sender -> sender.getSender().hasPermission(TPA_ACCEPT_PERMISSION))
                .executes(TeleportCommand::runAcceptLatest)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                List<TeleportRequest> requests = TeleportManager.getPendingRequestsForTarget(player.getUniqueId());
                                for (TeleportRequest req : requests) {
                                    builder.suggest(req.senderName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(TeleportCommand::runAcceptByName))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createTpaDenyCommand() {
        return Commands.literal("tpadeny")
                .requires(sender -> sender.getSender().hasPermission(TPA_ACCEPT_PERMISSION))
                .executes(TeleportCommand::runDeny)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createTpaAutoCommand() {
        return Commands.literal("tpaauto")
                .requires(sender -> sender.getSender().hasPermission(TPA_AUTO_PERMISSION))
                .executes(TeleportCommand::runTpaAuto)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createSettingsCommand() {
        return Commands.literal("settings")
                .requires(sender -> sender.getSender().hasPermission(SETTINGS_PERMISSION))
                .executes(TeleportCommand::runSettings)
                .build();
    }

    private static int runTpa(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Chat.prefixed("Player not found or offline.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Chat.prefixed("You cannot teleport to yourself!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (TeleportManager.hasActiveCountdown(player.getUniqueId())) {
            player.sendMessage(Chat.prefixed("You already have a teleport in progress.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PlayerSettings targetSettings = PlayerSettings.get(target.getUniqueId());
        if (!targetSettings.isTpaEnabled()) {
            player.sendMessage(Chat.prefixed(target.getName() + " has disabled TPA requests.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PlayerSettings playerSettings = PlayerSettings.get(player.getUniqueId());
        if (playerSettings.isConfirmTpGui()) {
            TeleportConfirmGui.openSendConfirm(player, target, TeleportRequest.Type.TPA);
        } else {
            TeleportResult result = TeleportManager.sendTpa(player, target);
            if (result.success()) {
                player.sendMessage(Chat.prefixed("Teleport request sent to " + target.getName() + ".", NamedTextColor.GREEN));
                if (result.request() != null) {
                    PlayerSettings targetSettings2 = PlayerSettings.get(target.getUniqueId());
                    if (targetSettings2.isTpaAutoEnabled()) {
                        TeleportManager.handleAutoAccept(target, result.request());
                    } else {
                        TeleportGuiListener.sendChatRequest(target, result.request());
                    }
                }
            } else {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTpaHere(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Chat.prefixed("Player not found or offline.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Chat.prefixed("You cannot teleport to yourself!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (TeleportManager.hasActiveCountdown(player.getUniqueId())) {
            player.sendMessage(Chat.prefixed("You already have a teleport in progress.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PlayerSettings targetSettings = PlayerSettings.get(target.getUniqueId());
        if (!targetSettings.isTpaHereEnabled()) {
            player.sendMessage(Chat.prefixed(target.getName() + " has disabled TPAhere requests.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PlayerSettings playerSettings = PlayerSettings.get(player.getUniqueId());
        if (playerSettings.isConfirmTpGui()) {
            TeleportConfirmGui.openSendConfirm(player, target, TeleportRequest.Type.TPAHERE);
        } else {
            TeleportResult result = TeleportManager.sendTpaHere(player, target);
            if (result.success()) {
                player.sendMessage(Chat.prefixed("Teleport-here request sent to " + target.getName() + ".", NamedTextColor.GREEN));
                if (result.request() != null) {
                    PlayerSettings targetSettings2 = PlayerSettings.get(target.getUniqueId());
                    if (targetSettings2.isTpaAutoEnabled()) {
                        TeleportManager.handleAutoAccept(target, result.request());
                    } else {
                        TeleportGuiListener.sendChatRequest(target, result.request());
                    }
                }
            } else {
                player.sendMessage(Chat.prefixed(result.message(), NamedTextColor.RED));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runAcceptLatest(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        TeleportRequest request = TeleportManager.getLatestPendingRequest(player.getUniqueId());
        if (request == null) {
            player.sendMessage(Chat.prefixed("You have no pending teleport requests.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        player.closeInventory();
        TeleportConfirmGui.openReceiveConfirm(player, request);
        return Command.SINGLE_SUCCESS;
    }

    private static int runAcceptByName(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String senderName = StringArgumentType.getString(ctx, "player");
        TeleportRequest request = TeleportManager.getPendingRequestFromSender(player.getUniqueId(), senderName);
        if (request == null) {
            player.sendMessage(Chat.prefixed("No pending teleport request from " + senderName + ".", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        player.closeInventory();
        TeleportConfirmGui.openReceiveConfirm(player, request);
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeny(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        TeleportResult result = TeleportManager.denyRequest(player);
        player.sendMessage(Chat.prefixed(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int runTpaAuto(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        PlayerSettings settings = PlayerSettings.get(player.getUniqueId());
        settings.setTpaAutoEnabled(!settings.isTpaAutoEnabled());
        player.sendMessage(Chat.prefixed("TPAuto: " + (settings.isTpaAutoEnabled() ? "Enabled" : "Disabled"),
                settings.isTpaAutoEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private static int runSettings(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        SettingsGui.open(player);
        return Command.SINGLE_SUCCESS;
    }
}
