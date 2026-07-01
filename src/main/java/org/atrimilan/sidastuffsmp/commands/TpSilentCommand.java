package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TpSilentCommand {

    public static final String DESCRIPTION = "Silently teleport to a player in spectate mode";
    public static final java.util.Set<String> ALIASES = java.util.Set.of("tps");

    private static final String PERM = "sidastuffsmp.tpsilent";

    private TpSilentCommand() {}

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) -> {
        for (Player p : Bukkit.getOnlinePlayers()) builder.suggest(p.getName());
        return builder.buildFuture();
    };

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("tpsilent")
                .requires(s -> s.getSender().hasPermission(PERM))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYERS)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player admin)) {
                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            String targetName = StringArgumentType.getString(ctx, "player");
                            Player target = Bukkit.getPlayerExact(targetName);
                            if (target == null) {
                                admin.sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (target.getUniqueId().equals(admin.getUniqueId())) {
                                admin.sendMessage(Chat.prefixed("You cannot tpSilent to yourself.", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }

                            admin.teleportAsync(target.getLocation()).thenAccept(success -> {
                                if (success) {
                                    boolean isGeyser = isGeyserPlayer(admin.getUniqueId());
                                    if (isGeyser) {
                                        admin.setGameMode(GameMode.CREATIVE);
                                        admin.setInvisible(true);
                                        admin.sendMessage(Chat.prefixed("Silently teleporting to " + target.getName() + " (creative+invisible - spectator unavailable on Bedrock).", NamedTextColor.YELLOW));
                                    } else {
                                        admin.setGameMode(GameMode.SPECTATOR);
                                        admin.sendMessage(Chat.prefixed("Silently teleporting to " + target.getName() + " in spectate mode.", NamedTextColor.GREEN));
                                    }
                                } else {
                                    admin.sendMessage(Chat.prefixed("Teleportation failed.", NamedTextColor.RED));
                                }
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static boolean isGeyserPlayer(UUID uuid) {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = geyserApiClass.getMethod("api").invoke(null);
            Object connection = geyserApiClass.getMethod("connection", UUID.class).invoke(api, uuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }
}
