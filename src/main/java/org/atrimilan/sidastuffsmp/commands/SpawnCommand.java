package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCommand {

    public static final String SPAWN_DESCRIPTION = "Teleport to spawn";
    public static final Set<String> SPAWN_ALIASES = Set.of("lobby", "hub");
    public static final String SETSPAWN_DESCRIPTION = "Set the spawn location";
    public static final Set<String> SETSPAWN_ALIASES = Set.of();

    private static final String PERMISSION_SPAWN = "sidastuffsmp.spawn";
    private static final String PERMISSION_SETSPAWN = "sidastuffsmp.spawn.set";
    private static final ConcurrentHashMap<UUID, Integer> ACTIVE_TELEPORTS = new ConcurrentHashMap<>();

    private SpawnCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createSpawnCommand() {
        return Commands.literal("spawn")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION_SPAWN))
                .executes(SpawnCommand::runSpawn)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createSetSpawnCommand() {
        return Commands.literal("setspawn")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION_SETSPAWN))
                .executes(SpawnCommand::runSetSpawn)
                .build();
    }

    private static int runSpawn(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        UUID uuid = player.getUniqueId();
        if (ACTIVE_TELEPORTS.containsKey(uuid)) {
            player.sendMessage(Chat.prefixed("You already have a teleport in progress.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Location spawn = SiDaStuffSmp.getInstance().getSpawnLocation();
        World world = Bukkit.getWorld("world");
        if (spawn == null || spawn.getWorld() == null) {
            if (world != null) {
                spawn = world.getSpawnLocation();
            } else {
                player.sendMessage(Chat.prefixed("Spawn location is not set.", NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
        }

        player.sendMessage(Chat.prefixed("Teleporting to spawn in 5 seconds... Don't move!", NamedTextColor.GRAY));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);

        final int[] taskIdHolder = new int[1];
        taskIdHolder[0] = Bukkit.getScheduler().runTaskTimer(SiDaStuffSmp.getInstance(), new Runnable() {
            int secondsLeft = 5;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    ACTIVE_TELEPORTS.remove(uuid);
                    return;
                }
                if (secondsLeft <= 0) {
                    ACTIVE_TELEPORTS.remove(uuid);
                    Bukkit.getScheduler().cancelTask(taskIdHolder[0]);
                    Location target = SiDaStuffSmp.getInstance().getSpawnLocation();
                    if (target == null || target.getWorld() == null) {
                        target = world != null ? world.getSpawnLocation() : player.getLocation();
                    }
                    player.teleportAsync(target).thenAccept(s -> {
                        if (s) {
                            player.sendMessage(Chat.prefixed("Teleported to spawn!", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        }
                    });
                    return;
                }
                player.sendActionBar(net.kyori.adventure.text.Component.text("Teleporting to spawn in " + secondsLeft + "s...", NamedTextColor.GRAY));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + (0.1f * (5 - secondsLeft)));
                secondsLeft--;
            }
        }, 0L, 20L).getTaskId();

        ACTIVE_TELEPORTS.put(uuid, taskIdHolder[0]);
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetSpawn(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        SiDaStuffSmp.getInstance().setSpawnLocation(player.getLocation());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.world", player.getWorld().getName());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.x", player.getLocation().getX());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.y", player.getLocation().getY());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.z", player.getLocation().getZ());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.yaw", (double) player.getLocation().getYaw());
        SiDaStuffSmp.getInstance().getConfig().set("spawn.pitch", (double) player.getLocation().getPitch());
        SiDaStuffSmp.getInstance().saveConfig();

        player.sendMessage(Chat.prefixed("Spawn location set!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        return Command.SINGLE_SUCCESS;
    }

    public static void cancelTeleport(UUID uuid) {
        Integer taskId = ACTIVE_TELEPORTS.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public static boolean hasActiveTeleport(UUID uuid) {
        return ACTIVE_TELEPORTS.containsKey(uuid);
    }
}
