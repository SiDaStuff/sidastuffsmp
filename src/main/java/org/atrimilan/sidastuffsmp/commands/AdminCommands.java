package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.auction.AuctionConfig;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.economy.EconomyConfig;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.home.HomeConfig;
import org.atrimilan.sidastuffsmp.home.HomeTeleportManager;
import org.atrimilan.sidastuffsmp.order.OrderConfig;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.rtp.RtpConfig;
import org.atrimilan.sidastuffsmp.rtp.RtpCooldown;
import org.atrimilan.sidastuffsmp.rtp.RtpManager;
import org.atrimilan.sidastuffsmp.teleport.PlayerSettings;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.ConfigManager;
import org.atrimilan.sidastuffsmp.utils.PunishmentManager;
import org.atrimilan.sidastuffsmp.stats.PlayerStatsManager;
import org.atrimilan.sidastuffsmp.stats.StatsListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminCommands {

    private static final Set<UUID> VANISHED = new HashSet<>();
    private static final Set<UUID> FROZEN = new HashSet<>();
    private static final Set<UUID> INSPECTING = new HashSet<>();
    private static final Set<UUID> INSPECTING_EC = new HashSet<>();
    private static final Map<UUID, UUID> INSPECT_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> INSPECT_EC_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack> OFFHAND_HISTORY = new ConcurrentHashMap<>();
    private static final String ADMIN_PERM = "sidastuffsmp.admin";

    // Per-command permissions
    private static final String PERM_RELOAD = "sidastuffsmp.admin.reload";
    private static final String PERM_VANISH = "sidastuffsmp.admin.vanish";
    private static final String PERM_FREEZE = "sidastuffsmp.admin.freeze";
    private static final String PERM_INSPECT = "sidastuffsmp.admin.inspect";
    private static final String PERM_ENDERCHEST = "sidastuffsmp.admin.enderchest";
    private static final String PERM_RUN_TOTEM = "sidastuffsmp.admin.run.totem";
    private static final String PERM_RUN_PING = "sidastuffsmp.admin.run.ping";
    private static final String PERM_RUN_FLY = "sidastuffsmp.admin.run.fly";
    private static final String PERM_RUN_GOD = "sidastuffsmp.admin.run.god";
    private static final String PERM_RUN_HEAL = "sidastuffsmp.admin.run.heal";
    private static final String PERM_RUN_FEED = "sidastuffsmp.admin.run.feed";
    private static final String PERM_RUN_CLEAR = "sidastuffsmp.admin.run.clear";

    public static boolean isVanished(UUID uuid) { return VANISHED.contains(uuid); }
    public static boolean isFrozen(UUID uuid) { return FROZEN.contains(uuid); }
    public static boolean isInspecting(UUID uuid) { return INSPECTING.contains(uuid); }
    public static boolean isInspectingEc(UUID uuid) { return INSPECTING_EC.contains(uuid); }
    public static void stopInspecting(UUID uuid) { INSPECTING.remove(uuid); INSPECT_TARGET.remove(uuid); }
    public static void stopInspectingEc(UUID uuid) { INSPECTING_EC.remove(uuid); INSPECT_EC_TARGET.remove(uuid); }
    public static UUID getInspectTarget(UUID uuid) { return INSPECT_TARGET.get(uuid); }
    public static UUID getInspectEcTarget(UUID uuid) { return INSPECT_EC_TARGET.get(uuid); }

    public static void removeFromAllSets(UUID uuid) {
        VANISHED.remove(uuid);
        FROZEN.remove(uuid);
        if (INSPECTING.remove(uuid)) INSPECT_TARGET.remove(uuid);
        if (INSPECTING_EC.remove(uuid)) INSPECT_EC_TARGET.remove(uuid);
        OFFHAND_HISTORY.remove(uuid);
    }

    public static void cleanupAll() {
        VANISHED.clear();
        FROZEN.clear();
        INSPECTING.clear();
        INSPECTING_EC.clear();
        INSPECT_TARGET.clear();
        INSPECT_EC_TARGET.clear();
        OFFHAND_HISTORY.clear();
    }

    private AdminCommands() {}

    private static boolean hasPerm(CommandSourceStack source, String specificPerm) {
        return source.getSender().hasPermission(specificPerm) || source.getSender().hasPermission(ADMIN_PERM);
    }

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) -> {
        for (Player p : Bukkit.getOnlinePlayers()) builder.suggest(p.getName());
        return builder.buildFuture();
    };

    public static LiteralCommandNode<CommandSourceStack> createSidaReloadCommand() {
        return Commands.literal("sidareload").requires(s -> hasPerm(s, PERM_RELOAD)).executes(ctx -> {
            SiDaStuffSmp plugin = SiDaStuffSmp.getInstance();
            plugin.reloadConfig();
            ConfigManager.reload();
            EconomyConfig.reload();
            AuctionConfig.reload();
            OrderConfig.reload();
            HomeConfig.reload();
            HomeTeleportManager.reload();
            RtpConfig.init(plugin);
            RtpCooldown.init(plugin);
            PlayerSettings.init(plugin);
            PunishmentManager.init(plugin);
            PlayerStatsManager.init(plugin);
            StatsListener.saveAllPlaytime();
            AuctionManager.cleanupOnStartup();
            OrderManager.cleanupOnStartup();
            RtpManager.pregenerateLocations();
            ctx.getSource().getSender().sendMessage(Chat.prefixed("All configs reloaded!", NamedTextColor.GREEN));
            if (ctx.getSource().getSender() instanceof Player p) {
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
                p.showTitle(Title.title(
                        net.kyori.adventure.text.Component.text("Config Reloaded", NamedTextColor.GREEN),
                        net.kyori.adventure.text.Component.empty()));
            }
            return Command.SINGLE_SUCCESS;
        }).build();
    }

    // --- Freeze command -----------------------------------------------------
    public static LiteralCommandNode<CommandSourceStack> createFreezeCommand() {
        return Commands.literal("freeze")
                .requires(s -> hasPerm(s, PERM_FREEZE))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                        .executes(ctx -> {
                            Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                            if (target == null) {
                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            UUID uuid = target.getUniqueId();
                            if (FROZEN.contains(uuid)) {
                                FROZEN.remove(uuid);
                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Unfrozen " + target.getName(), NamedTextColor.GREEN));
                                target.sendMessage(Chat.prefixed("You are no longer frozen.", NamedTextColor.GREEN));
                            } else {
                                FROZEN.add(uuid);
                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Frozen " + target.getName(), NamedTextColor.GREEN));
                                target.sendMessage(Chat.prefixed("You have been frozen.", NamedTextColor.RED));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createVanishCommand() {
        return Commands.literal("vanish").requires(s -> hasPerm(s, PERM_VANISH)).executes(ctx -> {
            if (!(ctx.getSource().getSender() instanceof Player player)) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
            UUID uuid = player.getUniqueId();
            if (VANISHED.contains(uuid)) {
                VANISHED.remove(uuid);
                for (Player p : Bukkit.getOnlinePlayers()) p.showPlayer(SiDaStuffSmp.getInstance(), player);
                player.sendMessage(Chat.prefixed("You are now visible!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            } else {
                VANISHED.add(uuid);
                for (Player p : Bukkit.getOnlinePlayers()) { if (!p.hasPermission(ADMIN_PERM)) p.hidePlayer(SiDaStuffSmp.getInstance(), player); }
                player.sendMessage(Chat.prefixed("You are now vanished!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            return Command.SINGLE_SUCCESS;
        }).build();
    }

    public static LiteralCommandNode<CommandSourceStack> createInspectCommand() {
        return Commands.literal("inspect").requires(s -> hasPerm(s, PERM_INSPECT))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                        .executes(ctx -> openInventoryInspect(ctx, false))
                        .then(Commands.literal("inventory").executes(ctx -> openInventoryInspect(ctx, false)))
                        .then(Commands.literal("enderchest").executes(ctx -> openInventoryInspect(ctx, true))))
                .executes(ctx -> { ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /inspect <player> [inventory|enderchest]", NamedTextColor.YELLOW)); return Command.SINGLE_SUCCESS; }).build();
    }

    public static LiteralCommandNode<CommandSourceStack> createEnderChestCommand() {
        return Commands.literal("enderchest").requires(s -> hasPerm(s, PERM_ENDERCHEST))
                .then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> openInventoryInspect(ctx, true)))
                .executes(ctx -> { ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /enderchest <player>", NamedTextColor.YELLOW)); return Command.SINGLE_SUCCESS; }).build();
    }

    private static int openInventoryInspect(CommandContext<CommandSourceStack> ctx, boolean enderChest) {
        if (!(ctx.getSource().getSender() instanceof Player admin)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
        if (target == null) {
            admin.sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (enderChest) {
            admin.openInventory(target.getEnderChest());
            INSPECTING_EC.add(admin.getUniqueId());
            INSPECT_EC_TARGET.put(admin.getUniqueId(), target.getUniqueId());
            admin.sendMessage(Chat.prefixed("Editing " + target.getName() + "'s ender chest. Changes save automatically.", NamedTextColor.GREEN));
            admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        } else {
            admin.openInventory(target.getInventory());
            INSPECTING.add(admin.getUniqueId());
            INSPECT_TARGET.put(admin.getUniqueId(), target.getUniqueId());
            admin.sendMessage(Chat.prefixed("Editing " + target.getName() + "'s inventory. Changes save automatically.", NamedTextColor.GREEN));
            admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void doAutoTotemAlert(Player target, Player admin) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(ADMIN_PERM)) {
                p.sendMessage(Chat.prefixed("AUTO-TOTEM DETECTED on " + target.getName() + "! Totem reappeared in offhand.", NamedTextColor.RED));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
            }
        }
    }

    private static String formatEffectName(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }

    public static LiteralCommandNode<CommandSourceStack> createRunCommand() {
        return Commands.literal("run").requires(s -> hasPerm(s, PERM_RUN_TOTEM) || hasPerm(s, PERM_RUN_PING) || hasPerm(s, PERM_RUN_FLY) || hasPerm(s, PERM_RUN_GOD) || hasPerm(s, PERM_RUN_HEAL) || hasPerm(s, PERM_RUN_FEED) || hasPerm(s, PERM_RUN_CLEAR) || hasPerm(s, PERM_FREEZE))
                .then(Commands.literal("totem").requires(s -> hasPerm(s, PERM_RUN_TOTEM)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player admin)) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { admin.sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    PlayerInventory inv = target.getInventory();
                    ItemStack offhand = inv.getItemInOffHand();
                    if (offhand.getType() != Material.TOTEM_OF_UNDYING) {
                        admin.sendMessage(Chat.prefixed(target.getName() + " does not have a totem in offhand.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Stash the totem and remove it.
                    ItemStack saved = offhand.clone();
                    inv.setItemInOffHand(null);
                    OFFHAND_HISTORY.put(target.getUniqueId(), saved);
                    admin.sendMessage(Chat.prefixed(
                            "Removed totem from " + target.getName() + "'s offhand. Watching for auto-totem...",
                            NamedTextColor.YELLOW));
                    target.sendMessage(Chat.prefixed("An admin removed your totem for testing.", NamedTextColor.RED));

                    // Wait about a second. If a totem reappeared in the offhand, flag the player.
                    Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                        if (!target.isOnline()) {
                            OFFHAND_HISTORY.remove(target.getUniqueId());
                            return;
                        }
                        ItemStack currentOffhand = target.getInventory().getItemInOffHand();
                        if (currentOffhand != null && currentOffhand.getType() == Material.TOTEM_OF_UNDYING) {
                            doAutoTotemAlert(target, admin);
                            // No need to put the totem back, the cheat already placed one.
                            OFFHAND_HISTORY.remove(target.getUniqueId());
                            return;
                        }

                        // Player did not auto-totem within ~1 second; put the original totem back.
                        target.getInventory().setItemInOffHand(saved);
                        admin.sendMessage(Chat.prefixed(
                                target.getName() + " did NOT auto-totem. Original totem returned to offhand.",
                                NamedTextColor.GREEN));
                        target.sendMessage(Chat.prefixed("Your totem was returned to your offhand.", NamedTextColor.GREEN));
                        OFFHAND_HISTORY.remove(target.getUniqueId());
                    }, 20L); // 20 ticks = 1 second
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("ping").requires(s -> hasPerm(s, PERM_RUN_PING)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    ctx.getSource().getSender().sendMessage(Chat.prefixed(target.getName() + "'s ping: " + target.getPing() + "ms", NamedTextColor.AQUA));
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("fly").requires(s -> hasPerm(s, PERM_RUN_FLY)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    boolean allow = !target.getAllowFlight();
                    target.setAllowFlight(allow); target.setFlying(allow);
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Toggled flight for " + target.getName() + ": " + allow, NamedTextColor.AQUA));
                    if (ctx.getSource().getSender() instanceof Player p) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("god").requires(s -> hasPerm(s, PERM_RUN_GOD)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player admin)) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players!", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { admin.sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    boolean god = !target.isInvulnerable(); target.setInvulnerable(god);
                    admin.sendMessage(Chat.prefixed("Toggled god mode for " + target.getName() + ": " + god, NamedTextColor.AQUA));
                    admin.playSound(admin.getLocation(), god ? org.bukkit.Sound.BLOCK_BEACON_ACTIVATE : org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("heal").requires(s -> hasPerm(s, PERM_RUN_HEAL)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    target.setHealth(target.getMaxHealth()); target.setFoodLevel(20); target.setSaturation(5f);
                    target.getActivePotionEffects().forEach(e -> target.removePotionEffect(e.getType()));
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Healed " + target.getName(), NamedTextColor.GREEN));
                    if (ctx.getSource().getSender() instanceof Player p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("feed").requires(s -> hasPerm(s, PERM_RUN_FEED)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    target.setFoodLevel(20); target.setSaturation(5f);
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Fed " + target.getName(), NamedTextColor.GREEN));
                    if (ctx.getSource().getSender() instanceof Player p) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("clear").requires(s -> hasPerm(s, PERM_RUN_CLEAR)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    target.getInventory().clear();
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Cleared " + target.getName() + "'s inventory.", NamedTextColor.GREEN));
                    if (ctx.getSource().getSender() instanceof Player p) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("freeze").requires(s -> hasPerm(s, PERM_FREEZE)).then(Commands.argument("target", StringArgumentType.word()).suggests(ONLINE_PLAYERS).executes(ctx -> {
                    Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                    if (target == null) { ctx.getSource().getSender().sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                    UUID uuid = target.getUniqueId();
                    if (FROZEN.contains(uuid)) {
                        FROZEN.remove(uuid);
                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Unfroze " + target.getName() + ".", NamedTextColor.GREEN));
                    } else {
                        FROZEN.add(uuid);
                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Froze " + target.getName() + ".", NamedTextColor.GREEN));
                    }
                    if (ctx.getSource().getSender() instanceof Player p) p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                    return Command.SINGLE_SUCCESS;
                })))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Usage: /run <test> <target>", NamedTextColor.YELLOW));
                    ctx.getSource().getSender().sendMessage(Chat.prefixed("Tests: totem, ping, fly, god, heal, feed, clear, freeze", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                }).build();
    }
}
