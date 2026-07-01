package org.atrimilan.sidastuffsmp.home;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeAdminCommand {

    public static final String DESCRIPTION = "Admin home management";
    public static final Set<String> ALIASES = Set.of();

    private static final String PERM = "sidastuffsmp.home.admin";

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) -> {
        for (Player p : Bukkit.getOnlinePlayers()) builder.suggest(p.getName());
        return builder.buildFuture();
    };

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("homeadmin")
                .requires(s -> s.getSender().hasPermission(PERM))
                .then(buildViewSubCommand())
                .then(buildTpSubCommand())
                .then(buildBackupImportSubCommand())
                .then(buildDeleteSubCommand())
                .then(buildReloadSubCommand())
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Chat.prefixed(
                            "Usage: /homeadmin <view|tp|delete|reload|backupimport>", NamedTextColor.YELLOW));
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildViewSubCommand() {
        return Commands.literal("view")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                        .executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player admin)) {
                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can view GUIs!", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            String name = StringArgumentType.getString(ctx, "player");
                            UUID uuid = resolveUuid(name);
                            if (uuid == null) {
                                admin.sendMessage(Chat.prefixed("Player not found: " + name, NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            HomeAdminGui.open(admin, uuid, name, 0);
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildTpSubCommand() {
        return Commands.literal("tp")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player admin)) {
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    String homeName = StringArgumentType.getString(ctx, "name").trim();
                                    UUID uuid = resolveUuid(playerName);
                                    if (uuid == null) {
                                        admin.sendMessage(Chat.prefixed("Player not found: " + playerName, NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Home target = findHomeByName(uuid, homeName);
                                    if (target == null) {
                                        admin.sendMessage(Chat.prefixed("Home '" + homeName + "' not found for " + playerName, NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Location loc = target.toLocation(null);
                                    if (loc == null || loc.getWorld() == null) {
                                        admin.sendMessage(Chat.prefixed("World '" + target.getWorld() + "' is not loaded!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    admin.teleportAsync(loc).thenAccept(success -> {
                                        if (success) {
                                            admin.sendMessage(Chat.prefixed("Teleported to " + playerName + "'s home '" + target.getName() + "'!", NamedTextColor.GREEN));
                                            admin.playSound(admin.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                                        } else {
                                            admin.sendMessage(Chat.prefixed("Teleport failed.", NamedTextColor.RED));
                                        }
                                    });
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteSubCommand() {
        return Commands.literal("delete")
                .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    String homeName = StringArgumentType.getString(ctx, "name").trim();
                                    UUID uuid = resolveUuid(playerName);
                                    if (uuid == null) {
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found: " + playerName, NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Home target = findHomeByName(uuid, homeName);
                                    if (target == null) {
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Home '" + homeName + "' not found for " + playerName, NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    HomeManager.deleteHome(uuid, target.getSlot());
                                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Deleted home '" + target.getName() + "' for " + playerName + ".", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildReloadSubCommand() {
        return Commands.literal("reload").executes(ctx -> {
            HomeConfig.reload();
            HomeTeleportManager.reload();
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Homes config reloaded!", NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildBackupImportSubCommand() {
        return Commands.literal("backupimport")
                .executes(ctx -> runBackupImport(ctx.getSource().getSender()));
    }

    public static int runBackupImport(CommandSender sender) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Chat.prefixed("You don't have permission to run this command.", NamedTextColor.RED));
            return 0;
        }
        File csvFile = new File(SiDaStuffSmp.getInstance().getDataFolder(), HomeConfig.getBackupCsvName());
        if (!csvFile.exists()) {
            // Fall back to the plugin's working directory (root of server directory).
            File rootFile = new File(HomeConfig.getBackupCsvName());
            if (!rootFile.exists()) {
                sender.sendMessage(Chat.prefixed("Backup CSV not found at " + csvFile.getAbsolutePath()
                        + " (or " + rootFile.getAbsolutePath() + ").", NamedTextColor.RED));
                return 0;
            }
            csvFile = rootFile;
        }

        try {
            HomeImportResult result = HomeManager.importFromCsvDetailed(csvFile);
            sender.sendMessage(Chat.prefixed("Imported " + result.imported() + " homes from " + csvFile.getName()
                    + ". Skipped " + result.skipped() + " (invalid or duplicate).", NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Chat.prefixed("Failed to import backup: " + e.getMessage(), NamedTextColor.RED));
            SiDaStuffSmp.getInstance().getLogger().warning("Home backup import failed: " + e.getMessage());
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resolve a player name to a UUID, looking first at online players and then the offline cache.
     * Returns null if no record exists.
     */
    public static UUID resolveUuid(String name) {
        if (name == null || name.isBlank()) return null;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null && offline.hasPlayedBefore()) return offline.getUniqueId();
        return null;
    }

    /**
     * Find a home by case-insensitive name across all slots for a given owner.
     */
    public static Home findHomeByName(UUID ownerUuid, String name) {
        if (ownerUuid == null || name == null) return null;
        for (Home home : HomeManager.getHomes(ownerUuid)) {
            if (home.getName().equalsIgnoreCase(name)) return home;
        }
        return null;
    }
}