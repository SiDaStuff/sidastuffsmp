package org.atrimilan.sidastuffsmp.home;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HomeCommand {

    public static final String DESCRIPTION = "Open home GUI or teleport to home";
    public static final Set<String> ALIASES = Set.of("homes");

    private static final SuggestionProvider<CommandSourceStack> HOME_NAMES = (ctx, builder) -> {
        if (ctx.getSource().getSender() instanceof Player player) {
            for (Home h : HomeManager.getHomes(player.getUniqueId())) {
                builder.suggest("\"" + h.getName() + "\"");
            }
        }
        return builder.buildFuture();
    };

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("home")
                .requires(source -> source.getSender().hasPermission("sidastuffsmp.home") || source.getSender().hasPermission("sidastuffsmp.admin"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage("Only players can use this command.");
                        return Command.SINGLE_SUCCESS;
                    }
                    HomeGui.open(player);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(HOME_NAMES)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                ctx.getSource().getSender().sendMessage("Only players can use this command.");
                                return Command.SINGLE_SUCCESS;
                            }
                            String raw = StringArgumentType.getString(ctx, "name").trim();
                            String name = raw;
                            if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                                name = name.substring(1, name.length() - 1);
                            }
                            List<Home> homes = HomeManager.getHomes(player.getUniqueId());
                            Home target = null;
                            for (Home h : homes) {
                                if (h.getName().equalsIgnoreCase(name)) {
                                    target = h;
                                    break;
                                }
                            }
                            if (target == null) {
                                player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home '" + name + "' not found!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            HomeTeleportManager.teleportToHome(player, target);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createSetHomeCommand() {
        return Commands.literal("sethome")
                .requires(source -> source.getSender().hasPermission("sidastuffsmp.home") || source.getSender().hasPermission("sidastuffsmp.admin"))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                ctx.getSource().getSender().sendMessage("Only players can use this command.");
                                return Command.SINGLE_SUCCESS;
                            }
                            String name = StringArgumentType.getString(ctx, "name").trim();
                            if (name.isEmpty()) {
                                player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home name cannot be empty!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            String world = player.getLocation().getWorld().getName();
                            if (HomeConfig.isWorldBlocked(world)) {
                                player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("You cannot set homes in this world!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            List<Home> homes = HomeManager.getHomes(player.getUniqueId());
                            int slot = -1;
                            for (int i = 1; i <= HomeConfig.maxHomes(); i++) {
                                boolean taken = false;
                                for (Home h : homes) {
                                    if (h.getSlot() == i) { taken = true; break; }
                                }
                                if (!taken) { slot = i; break; }
                            }
                            if (slot == -1) {
                                player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("You have reached the maximum number of homes (" + HomeConfig.maxHomes() + ")!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            HomeManager.createHome(player.getUniqueId(), slot, name, world,
                                    player.getLocation().getX(),
                                    player.getLocation().getY(),
                                    player.getLocation().getZ(),
                                    player.getLocation().getYaw(),
                                    player.getLocation().getPitch());
                            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home '" + name + "' set in slot " + slot + "!", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createDelHomeCommand() {
        return Commands.literal("delhome")
                .requires(source -> source.getSender().hasPermission("sidastuffsmp.home") || source.getSender().hasPermission("sidastuffsmp.admin"))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(HOME_NAMES)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                ctx.getSource().getSender().sendMessage("Only players can use this command.");
                                return Command.SINGLE_SUCCESS;
                            }
                            String raw = StringArgumentType.getString(ctx, "name").trim();
                            String name = raw;
                            if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
                                name = name.substring(1, name.length() - 1);
                            }
                            List<Home> homes = HomeManager.getHomes(player.getUniqueId());
                            Home target = null;
                            for (Home h : homes) {
                                if (h.getName().equalsIgnoreCase(name)) {
                                    target = h;
                                    break;
                                }
                            }
                            if (target == null) {
                                player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home '" + name + "' not found!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            HomeManager.deleteHome(player.getUniqueId(), target.getSlot());
                            player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Home '" + name + "' deleted!", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}