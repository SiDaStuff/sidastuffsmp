package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Set;

public class GameModeCommands {

    public static final String DESCRIPTION = "Switch game mode";
    public static final Set<String> ALIASES = Set.of();

    public static LiteralCommandNode<CommandSourceStack> createGmsCommand() {
        return Commands.literal("gms")
                .requires(source -> source.getSender() instanceof Player
                        && (source.getSender().hasPermission("sidastuffsmp.gamemode")
                            || source.getSender().hasPermission("sidastuffsmp.admin")))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(Component.text("Game mode set to Survival").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createGmcCommand() {
        return Commands.literal("gmc")
                .requires(source -> source.getSender() instanceof Player
                        && (source.getSender().hasPermission("sidastuffsmp.gamemode")
                            || source.getSender().hasPermission("sidastuffsmp.admin")))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(Component.text("Game mode set to Creative").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createGmspCommand() {
        return Commands.literal("gmsp")
                .requires(source -> source.getSender() instanceof Player
                        && (source.getSender().hasPermission("sidastuffsmp.gamemode")
                            || source.getSender().hasPermission("sidastuffsmp.admin")))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(Component.text("Game mode set to Spectator").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createGmaCommand() {
        return Commands.literal("gma")
                .requires(source -> source.getSender() instanceof Player
                        && (source.getSender().hasPermission("sidastuffsmp.gamemode")
                            || source.getSender().hasPermission("sidastuffsmp.admin")))
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    player.setGameMode(GameMode.ADVENTURE);
                    player.sendMessage(Component.text("Game mode set to Adventure").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}