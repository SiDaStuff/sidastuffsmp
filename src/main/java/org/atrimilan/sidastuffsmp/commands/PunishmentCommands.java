package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.listeners.AntiDupingListener;
import org.atrimilan.sidastuffsmp.sus.SusConfig;
import org.atrimilan.sidastuffsmp.sus.SusConfig.PunishPreset;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.atrimilan.sidastuffsmp.utils.PunishmentManager;
import org.atrimilan.sidastuffsmp.economy.EconomyDatabase;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.home.HomeManager;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.stats.PlayerStatsManager;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class PunishmentCommands {

    public static final String PUNISH_DESCRIPTION = "Escalating punish command";
    public static final Set<String> PUNISH_ALIASES = Set.of();
    public static final String KICK_DESCRIPTION = "Kick a player";
    public static final Set<String> KICK_ALIASES = Set.of();
    public static final String BAN_DESCRIPTION = "Ban a player";
    public static final Set<String> BAN_ALIASES = Set.of();
    public static final String TEMPBAN_DESCRIPTION = "Temporarily ban a player";
    public static final Set<String> TEMPBAN_ALIASES = Set.of();
    public static final String HISTORY_DESCRIPTION = "Show punishment history";
    public static final Set<String> HISTORY_ALIASES = Set.of("phistory");
    public static final String NEW_SEASON_DESCRIPTION = "Unban everyone for a new season";
    public static final Set<String> NEW_SEASON_ALIASES = Set.of("seasonreset");
    public static final String UNBAN_DESCRIPTION = "Unban a player";
    public static final Set<String> UNBAN_ALIASES = Set.of("pardonplayer");
    public static final String RESTORE_DESCRIPTION = "Restore a punished player's data";
    public static final Set<String> RESTORE_ALIASES = Set.of("restoreplayer");
    public static final String PUNISHTYPE_DESCRIPTION = "External punishment test tools";
    public static final Set<String> PUNISHTYPE_ALIASES = Set.of();

    private static final String LEGACY_PERMISSION = "sidastuffsmp.punish";
    private static final String WILDCARD_PERMISSION = "sidastuffsmp.punish.*";
    private static final String PUNISH_PERMISSION = "sidastuffsmp.punish.punish";
    private static final String KICK_PERMISSION = "sidastuffsmp.punish.kick";
    private static final String BAN_PERMISSION = "sidastuffsmp.punish.ban";
    private static final String TEMPBAN_PERMISSION = "sidastuffsmp.punish.tempban";
    private static final String HISTORY_PERMISSION = "sidastuffsmp.punish.history";
    private static final String NEW_SEASON_PERMISSION = "sidastuffsmp.punish.newseason";
    private static final String UNBAN_PERMISSION = "sidastuffsmp.punish.unban";
    private static final String RESTORE_PERMISSION = "sidastuffsmp.punish.restore";
    private static final String PUNISHTYPE_PERMISSION = "sidastuffsmp.punish.type";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault());

    private PunishmentCommands() {}

    public static LiteralCommandNode<CommandSourceStack> createPunishCommand() {
        return Commands.literal("punish")
                .requires(sender -> hasPunishPermission(sender, PUNISH_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> runPunish(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "days"),
                                                StringArgumentType.getString(ctx, "reason")
                                        ))))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> runPunish(
                                        ctx,
                                        null,
                                        StringArgumentType.getString(ctx, "reason")
                                ))))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createKickCommand() {
        return Commands.literal("kick")
                .requires(sender -> hasPunishPermission(sender, KICK_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> runKick(ctx, "Removed by staff."))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> runKick(ctx, StringArgumentType.getString(ctx, "reason")))))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createBanCommand() {
        return Commands.literal("ban")
                .requires(sender -> hasPunishPermission(sender, BAN_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> runBan(ctx, StringArgumentType.getString(ctx, "reason")))))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createTempBanCommand() {
        return Commands.literal("tempban")
                .requires(sender -> hasPunishPermission(sender, TEMPBAN_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> runTempBan(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "days"),
                                                StringArgumentType.getString(ctx, "reason")
                                        )))))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createPunishHistoryCommand() {
        return Commands.literal("punishhistory")
                .requires(sender -> hasPunishPermission(sender, HISTORY_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(PunishmentCommands::runPunishHistory))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createNewSeasonUnbanAllCommand() {
        return Commands.literal("newseasonunbanall")
                .requires(sender -> hasPunishPermission(sender, NEW_SEASON_PERMISSION))
                .executes(PunishmentCommands::runNewSeasonUnbanAll)
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createUnbanCommand() {
        return Commands.literal("unban")
                .requires(sender -> hasPunishPermission(sender, UNBAN_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(PunishmentCommands::runUnban))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createRestoreCommand() {
        return Commands.literal("restore")
                .requires(sender -> hasPunishPermission(sender, RESTORE_PERMISSION))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(PunishmentCommands::runRestore))
                .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createPunishTypeCommand() {
        return Commands.literal("punishtype")
                .requires(sender -> hasPunishPermission(sender, PUNISHTYPE_PERMISSION))
                .then(Commands.literal("testdupe")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> runPunishTypeTestDupe(ctx, 1))
                                .then(Commands.argument("times", IntegerArgumentType.integer(1, 5))
                                        .executes(ctx -> runPunishTypeTestDupe(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "times")
                                        )))))
                .build();
    }

    private static boolean hasPunishPermission(CommandSourceStack source, String specificPermission) {
        CommandSender sender = source.getSender();
        return sender.hasPermission(specificPermission)
                || sender.hasPermission(WILDCARD_PERMISSION)
                || sender.hasPermission(LEGACY_PERMISSION);
    }

    private static int runPunish(CommandContext<CommandSourceStack> ctx, Integer customDays, String reason) {
        CommandSender sender = ctx.getSource().getSender();
        String senderName = sender.getName();
        String playerArg = StringArgumentType.getString(ctx, "player");
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String lowerReason = reason.toLowerCase(Locale.ROOT).trim();
        PunishPreset preset = SusConfig.getPreset(lowerReason);

        if (preset != null) {
            executePresetActions(sender, senderName, target, preset);
            sender.sendMessage(Chat.prefixed("Punished " + target.displayName() + " with preset: " + preset.reason(), NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }

        int punishCount = PunishmentManager.incrementPunishCount(target.playerId(), target.displayName());
        int durationDays = customDays != null ? customDays : defaultDurationDays(punishCount);
        boolean permanent = customDays == null && punishCount >= 4;

        BanResult result = banWithCleanup(
                target,
                senderName,
                permanent ? -1 : durationDays,
                reason,
                "PUNISH",
                true,
                getEscalationActions(punishCount)
        );

        sender.sendMessage(Chat.prefixed("Punished " + target.displayName() + ".", NamedTextColor.GREEN));
        if (permanent) {
            sender.sendMessage(Chat.prefixed("Escalation level " + punishCount + ": permanent ban until next season.", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Chat.prefixed("Escalation level " + punishCount + ": " + durationDays + " day(s).", NamedTextColor.YELLOW));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static List<String> getEscalationActions(int punishCount) {
        if (punishCount >= 4) return List.of("ban", "statswipe", "inventorywipe", "ecwipe", "balancewipe", "auctionwipe", "homewipe", "orderwipe");
        if (punishCount == 3) return List.of("tempban", "statswipe", "inventorywipe", "ecwipe", "balancewipe");
        if (punishCount == 2) return List.of("tempban", "statswipe", "inventorywipe", "ecwipe");
        return List.of("tempban", "statswipe");
    }

    private static void executePresetActions(CommandSender sender, String senderName, Target target, PunishPreset preset) {
        List<String> actions = preset.actions();
        int durationDays = preset.durationDays();
        boolean isPermanent = preset.isPermanent();
        boolean doBan = preset.hasAction("ban");
        boolean doTempBan = preset.hasAction("tempban");

        org.bukkit.inventory.ItemStack[] inventorySnapshot = null;
        org.bukkit.inventory.ItemStack[] enderChestSnapshot = null;
        double playerBalance = 0.0;
        PlayerStatsManager.PlayerStats stats = new PlayerStatsManager.PlayerStats("Unknown", 0, 0, 0, 0, 0);

        Player online = target.onlinePlayer();
        if (online != null) {
            inventorySnapshot = online.getInventory().getContents().clone();
            enderChestSnapshot = online.getEnderChest().getContents().clone();
            if (EconomyManager.hasAccount(target.playerId())) {
                playerBalance = EconomyManager.getBalance(target.playerId());
            }
            stats = PlayerStatsManager.getStats(target.playerId());
        }

        boolean anyWipeAction = preset.hasAction("statswipe")
                || preset.hasAction("inventorywipe")
                || preset.hasAction("ecwipe")
                || preset.hasAction("balancewipe")
                || preset.hasAction("auctionwipe")
                || preset.hasAction("homewipe")
                || preset.hasAction("orderwipe");

        if (anyWipeAction && (inventorySnapshot != null || enderChestSnapshot != null)) {
            PunishmentManager.saveRestoreSnapshot(
                    target.playerId(),
                    target.displayName(),
                    inventorySnapshot != null ? inventorySnapshot : new org.bukkit.inventory.ItemStack[0],
                    enderChestSnapshot != null ? enderChestSnapshot : new org.bukkit.inventory.ItemStack[0],
                    playerBalance,
                    stats.kills(),
                    stats.deaths(),
                    stats.rtpCount(),
                    (int) stats.playtimeSeconds()
            );
        }

        StringBuilder actionLog = new StringBuilder();
        for (String action : actions) {
            switch (action.toLowerCase(Locale.ROOT)) {
                case "ban", "tempban" -> {}
                case "statswipe" -> {
                    PlayerStatsManager.wipeStats(target.playerId());
                    actionLog.append("statswipe ");
                }
                case "inventorywipe" -> {
                    if (online != null) {
                        online.getInventory().clear();
                        online.getInventory().setArmorContents(null);
                        online.getInventory().setExtraContents(null);
                        online.getInventory().setItemInOffHand(null);
                    }
                    actionLog.append("inventorywipe ");
                }
                case "ecwipe" -> {
                    if (online != null) {
                        online.getEnderChest().clear();
                    }
                    actionLog.append("ecwipe ");
                }
                case "balancewipe" -> {
                    if (EconomyManager.hasAccount(target.playerId()) && playerBalance > 0) {
                        EconomyManager.setBalance(target.playerId(), 0.0);
                        EconomyDatabase.addTransaction(target.playerId(), "ADMIN_TAKE", playerBalance, null, senderName, "Balance wiped by punish preset: " + preset.key());
                    }
                    actionLog.append("balancewipe ");
                }
                case "auctionwipe" -> {
                    wipeAuctions(target.playerId(), senderName, preset.key());
                    actionLog.append("auctionwipe ");
                }
                case "homewipe" -> {
                    wipeHomes(target.playerId());
                    actionLog.append("homewipe ");
                }
                case "orderwipe" -> {
                    wipeOrders(target.playerId(), senderName, preset.key());
                    actionLog.append("orderwipe ");
                }
                default -> sender.sendMessage(Chat.prefixed("Unknown preset action: " + action, NamedTextColor.YELLOW));
            }
        }

        if (online != null && !online.isDead() && (preset.hasAction("inventorywipe") || preset.hasAction("ecwipe"))) {
            online.setHealth(0.0D);
        }

        if (doBan || doTempBan) {
            int banDays = isPermanent ? -1 : durationDays;
            long expiresAt = -1L;
            Date expiresDate = null;
            if (banDays > 0) {
                expiresAt = System.currentTimeMillis() + (banDays * 24L * 60L * 60L * 1000L);
                expiresDate = new Date(expiresAt);
            }

            String fixedReason = appendPeriod(preset.reason());
            String banReason = buildBanReasonText(fixedReason, banDays, expiresAt);
            Bukkit.getBanList(BanList.Type.NAME).addBan(target.banName(), banReason, expiresDate, senderName);

            if (online != null) {
                online.kick(buildBanKickMessage(fixedReason, banDays, expiresAt));
            }

            String actionLabel = doBan ? "BAN" : "TEMPBAN";
            PunishmentManager.addHistoryEntry(
                    target.playerId(),
                    target.displayName(),
                    actionLabel,
                    fixedReason + " [" + actionLog.toString().trim() + "]",
                    senderName,
                    banDays,
                    expiresAt
            );
        } else {
            PunishmentManager.addHistoryEntry(
                    target.playerId(),
                    target.displayName(),
                    "PUNISH_PRESET",
                    preset.reason() + " [" + actionLog.toString().trim() + "]",
                    senderName,
                    durationDays,
                    -1L
            );

            if (online != null) {
                online.kick(buildKickMessage("Punished: " + preset.reason()));
            }
        }
    }

    private static void wipeAuctions(UUID playerUuid, String sourceName, String presetKey) {
        try {
            var conn = AuctionManager.getConnection();
            if (conn == null || conn.isClosed()) return;
            try (var ps = conn.prepareStatement("UPDATE auction_listings SET status = 'CANCELLED', collected = 1 WHERE seller_uuid = ? AND status = 'ACTIVE'")) {
                ps.setString(1, playerUuid.toString());
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) {
                    org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance().getLogger().info(
                            "Auctionwipe: cancelled " + cancelled + " active auctions for " + playerUuid + " (preset: " + presetKey + ")");
                }
            }
            try (var ps = conn.prepareStatement("DELETE FROM auction_listings WHERE seller_uuid = ? AND status IN ('SOLD','EXPIRED','CANCELLED') AND collected = 1")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    private static void wipeHomes(UUID playerUuid) {
        HomeManager.deleteAllHomes(playerUuid);
    }

    private static void wipeOrders(UUID playerUuid, String sourceName, String presetKey) {
        try {
            var conn = OrderManager.getConnection();
            if (conn == null || conn.isClosed()) return;
            try (var ps = conn.prepareStatement("UPDATE buy_orders SET status = 'CANCELLED', completed_at = ? WHERE buyer_uuid = ? AND status = 'ACTIVE'")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
            try (var ps = conn.prepareStatement("DELETE FROM order_stash WHERE buyer_uuid = ? AND collected = 0")) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    private static int runKick(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Player online = target.onlinePlayer();
        if (online == null) {
            sender.sendMessage(Chat.prefixed("Player " + target.displayName() + " is not online.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        online.kick(buildKickMessage(reason));
        PunishmentManager.addHistoryEntry(
                target.playerId(),
                target.displayName(),
                "KICK",
                appendPeriod(reason),
                sender.getName(),
                0,
                -1L
        );
        sender.sendMessage(Chat.prefixed("Kicked " + target.displayName() + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runBan(CommandContext<CommandSourceStack> ctx, String reason) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        BanResult result = banWithCleanup(target, sender.getName(), -1, reason, "BAN", false, List.of());
        sender.sendMessage(Chat.prefixed("Banned " + target.displayName() + " permanently.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runTempBan(CommandContext<CommandSourceStack> ctx, int days, String reason) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        BanResult result = banWithCleanup(target, sender.getName(), days, reason, "TEMPBAN", false, List.of());
        sender.sendMessage(Chat.prefixed("Tempbanned " + target.displayName() + " for " + days + " day(s).", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runPunishHistory(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player");
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        int punishCount = PunishmentManager.getPunishCount(target.playerId());
        List<PunishmentManager.HistoryEntry> entries = PunishmentManager.getHistory(target.playerId(), 10);

        sender.sendMessage(Chat.prefixed("History for " + target.displayName() + ":", NamedTextColor.AQUA));
        sender.sendMessage(Chat.prefixed("Escalation count: " + punishCount + ".", NamedTextColor.YELLOW));

        if (entries.isEmpty()) {
            sender.sendMessage(Chat.prefixed("No punishment history found.", NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        for (PunishmentManager.HistoryEntry entry : entries) {
            String when = formatEpoch(entry.timestampEpochMillis());
            String expires = entry.expiresAtEpochMillis() > 0 ? formatEpoch(entry.expiresAtEpochMillis()) : "Never";
            sender.sendMessage(Component.text("- " + when + " | " + entry.action() + " | " + entry.reason(), NamedTextColor.WHITE));
            sender.sendMessage(Component.text("  Source: " + entry.source() + " | Duration(days): " + entry.durationDays() + " | Expires: " + expires + ".", NamedTextColor.GRAY));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int runNewSeasonUnbanAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        Set<String> targets = new HashSet<>();

        for (Object entryObject : banList.getBanEntries()) {
            if (entryObject instanceof BanEntry entry) {
                targets.add(String.valueOf(entry.getTarget()));
            }
        }
        for (String target : targets) {
            banList.pardon(target);
        }

        PunishmentManager.resetAllPunishCounts();

        sender.sendMessage(Chat.prefixed(
                "New season reset complete. Unbanned " + targets.size() + " player(s) and reset punish escalation counts.",
                NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static int runUnban(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player").trim();
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);

        banList.pardon(playerArg);
        if (!target.banName().equalsIgnoreCase(playerArg)) {
            banList.pardon(target.banName());
        }

        PunishmentManager.addHistoryEntry(
                target.playerId(),
                target.displayName(),
                "UNBAN",
                "Unbanned by staff.",
                sender.getName(),
                0,
                -1L
        );

        sender.sendMessage(Chat.prefixed("Unbanned " + target.displayName() + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int runRestore(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player").trim();
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (!PunishmentManager.hasRestoreSnapshot(target.playerId())) {
            sender.sendMessage(Chat.prefixed("No saved punish snapshot found for " + target.displayName() + ".", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Player online = target.onlinePlayer();
        if (online == null) {
            PunishmentManager.setRestorePending(target.playerId(), true);
            sender.sendMessage(Chat.prefixed(
                    target.displayName() + " is offline. Restore is queued and will apply when they join.",
                    NamedTextColor.YELLOW
            ));
            return Command.SINGLE_SUCCESS;
        }

        RestoreResult result = restoreSnapshotToOnlinePlayer(online);
        if (!result.restored()) {
            sender.sendMessage(Chat.prefixed("Restore failed for " + target.displayName() + ".", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        StringBuilder restoreDetails = new StringBuilder("Restored inventory, ender chest");
        if (result.balanceRestored()) {
            restoreDetails.append(", and balance");
        }
        if (result.statsRestored()) {
            restoreDetails.append(", and stats");
        }

        PunishmentManager.addHistoryEntry(
                target.playerId(),
                target.displayName(),
                "RESTORE",
                restoreDetails.toString() + ".",
                sender.getName(),
                0,
                -1L
        );

        sender.sendMessage(Chat.prefixed("Restored " + target.displayName() + " successfully.", NamedTextColor.GREEN));
        if (!result.balanceRestored()) {
            sender.sendMessage(Chat.prefixed("Inventory and ender chest restored, but balance could not be restored.", NamedTextColor.RED));
        }
        if (!result.statsRestored()) {
            sender.sendMessage(Chat.prefixed("Inventory and ender chest restored, but stats could not be restored.", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runPunishTypeTestDupe(CommandContext<CommandSourceStack> ctx, int times) {
        CommandSender sender = ctx.getSource().getSender();
        String playerArg = StringArgumentType.getString(ctx, "player").trim();
        Target target = resolveTarget(playerArg);
        if (target == null) {
            sender.sendMessage(Chat.prefixed("Player not found: " + playerArg, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        Player online = target.onlinePlayer();

        if (online == null) {
            sender.sendMessage(Chat.prefixed("Player " + target.displayName() + " must be online for testdupe.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        int totalDuped = 0;
        int executedRuns = 0;
        for (int i = 0; i < times; i++) {
            AntiDupingListener.ExternalTestResult result = AntiDupingListener.runExternalTestDupe(online, "punishtype-testdupe");
            if (!result.executed()) {
                sender.sendMessage(Chat.prefixed("testdupe failed: " + result.message(), NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }

            executedRuns++;
            totalDuped += result.duplicatedItems();
        }

        sender.sendMessage(Chat.prefixed(
                "Executed testdupe on " + target.displayName() + " (runs: " + executedRuns + ", duplicated items: " + totalDuped + ").",
                NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static BanResult banWithCleanup(
            Target target,
            String sourceName,
            int durationDays,
            String reason,
            String actionLabel,
            boolean runSpecialPunishActions,
            List<String> extraActions
    ) {
        Player online = target.onlinePlayer();

        if (runSpecialPunishActions && online != null) {
            org.bukkit.inventory.ItemStack[] inventorySnapshot = online.getInventory().getContents().clone();
            org.bukkit.inventory.ItemStack[] enderChestSnapshot = online.getEnderChest().getContents().clone();
            double playerBalance = 0.0;
            if (EconomyManager.hasAccount(target.playerId())) {
                playerBalance = EconomyManager.getBalance(target.playerId());
            }

            org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.PlayerStats stats =
                    org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.getStats(target.playerId());

            PunishmentManager.saveRestoreSnapshot(
                target.playerId(),
                target.displayName(),
                inventorySnapshot,
                enderChestSnapshot,
                playerBalance,
                stats.kills(),
                stats.deaths(),
                stats.rtpCount(),
                (int) stats.playtimeSeconds()
            );

            for (String action : extraActions) {
                switch (action.toLowerCase(Locale.ROOT)) {
                    case "statswipe" -> PlayerStatsManager.wipeStats(target.playerId());
                    case "inventorywipe" -> {
                        online.getInventory().clear();
                        online.getInventory().setArmorContents(null);
                        online.getInventory().setExtraContents(null);
                        online.getInventory().setItemInOffHand(null);
                    }
                    case "ecwipe" -> online.getEnderChest().clear();
                    case "balancewipe" -> {
                        if (playerBalance > 0 && EconomyManager.hasAccount(target.playerId())) {
                            EconomyManager.setBalance(target.playerId(), 0.0);
                            EconomyDatabase.addTransaction(target.playerId(), "ADMIN_TAKE", playerBalance, null, sourceName, "Balance wiped by punish");
                        }
                    }
                    case "auctionwipe" -> wipeAuctions(target.playerId(), sourceName, "escalation");
                    case "homewipe" -> wipeHomes(target.playerId());
                    case "orderwipe" -> wipeOrders(target.playerId(), sourceName, "escalation");
                }
            }

            if (!online.isDead() && (extraActions.contains("inventorywipe") || extraActions.contains("ecwipe"))) {
                online.setHealth(0.0D);
            } else if (!online.isDead() && extraActions.contains("statswipe")) {
                online.setHealth(0.0D);
            }
        }

        long expiresAt = -1L;
        Date expiresDate = null;
        if (durationDays > 0) {
            expiresAt = System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L);
            expiresDate = new Date(expiresAt);
        }

        String fixedReason = appendPeriod(reason);
        String banReason = buildBanReasonText(fixedReason, durationDays, expiresAt);
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.banName(), banReason, expiresDate, sourceName);

        if (online != null) {
            online.kick(buildBanKickMessage(fixedReason, durationDays, expiresAt));
        }

        PunishmentManager.addHistoryEntry(
                target.playerId(),
                target.displayName(),
                actionLabel,
                fixedReason,
                sourceName,
                durationDays,
                expiresAt
        );

        return new BanResult();
    }

    private static Component buildKickMessage(String reason) {
        return Component.text("SiDaStuff SMP", NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .append(Component.newline())
                .append(Component.text("You were kicked.", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.GRAY))
                .append(Component.text(appendPeriod(reason), NamedTextColor.WHITE));
    }

    private static Component buildBanKickMessage(String reason, int durationDays, long expiresAtEpochMillis) {
        return Component.text(
                "SiDaStuff SMP\n"
                        + "You have been banned.\n"
                        + "Reason: " + reason + "\n"
                        + "Duration: " + formatDuration(durationDays, expiresAtEpochMillis) + "\n"
                        + "Appeal: appeal.sidastuff.com/mc"
        );
    }

    private static String buildBanReasonText(String reason, int durationDays, long expiresAtEpochMillis) {
        return "SiDaStuff SMP\n"
                + "Reason: " + reason + "\n"
                + "Duration: " + formatDuration(durationDays, expiresAtEpochMillis) + "\n"
                + "Appeal: appeal.sidastuff.com/mc";
    }

    private static String formatDuration(int durationDays, long expiresAtEpochMillis) {
        if (durationDays <= 0) {
            return "Permanent until next season.";
        }
        return durationDays + " day(s), until " + formatEpoch(expiresAtEpochMillis) + ".";
    }

    private static String formatEpoch(long epochMillis) {
        if (epochMillis <= 0) return "Never";
        return DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String appendPeriod(String text) {
        if (text == null || text.isBlank()) return "No reason provided.";
        String trimmed = text.trim();
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?")) {
            return trimmed;
        }
        return trimmed + ".";
    }

	public static RestoreResult restoreSnapshotToOnlinePlayer(Player player) {
		PunishmentManager.RestoreSnapshot snapshot = PunishmentManager.getRestoreSnapshot(player.getUniqueId());
        if (snapshot == null) {
            return new RestoreResult(false, false, false);
        }
        if (snapshot.inventoryContents() != null) {
            player.getInventory().setContents(snapshot.inventoryContents());
        }

		boolean balanceRestored = false;
		if (snapshot.balance() >= 0 && EconomyManager.hasAccount(player.getUniqueId())) {
			double currentBalance = EconomyManager.getBalance(player.getUniqueId());
			double restoredBalance = currentBalance + snapshot.balance();
			boolean setOk = EconomyManager.setBalance(player.getUniqueId(), restoredBalance);
			if (setOk) {
				EconomyDatabase.addTransaction(player.getUniqueId(), "ADMIN_GIVE", snapshot.balance(), null, "RESTORE", "Balance restored from punish snapshot");
				balanceRestored = true;
			}
		}

		boolean statsRestored = false;
		if (snapshot.kills() >= 0 && snapshot.deaths() >= 0) {
			PlayerStatsManager.restoreStats(
					player.getUniqueId(),
					player.getName(),
					snapshot.kills(),
					snapshot.deaths(),
					snapshot.rtpCount(),
					snapshot.playtimeSeconds()
			);
			statsRestored = true;
		}

        if (snapshot.enderChestContents() != null) {
            player.getEnderChest().setContents(snapshot.enderChestContents());
        }

		PunishmentManager.clearRestoreSnapshot(player.getUniqueId());
		return new RestoreResult(true, balanceRestored, statsRestored);
	}

    public static AutoPunishResult autoPunishForDuping(Player player, String sourceName) {
        Target target = resolveTarget(player.getName());
        if (target == null) {
            SiDaStuffSmp.getInstance().getLogger().warning("autoPunishForDuping: could not resolve target " + player.getName());
            return new AutoPunishResult(player.getName(), 0, false, 0);
        }
        int punishCount = PunishmentManager.incrementPunishCount(target.playerId(), target.displayName());
        int durationDays = defaultDurationDays(punishCount);
        boolean permanent = punishCount >= 4;

        banWithCleanup(
                target,
                sourceName,
                permanent ? -1 : durationDays,
                "Duping",
                "PUNISH",
                true,
                getEscalationActions(punishCount)
        );

        return new AutoPunishResult(
                target.displayName(),
                punishCount,
                permanent,
                permanent ? -1 : durationDays
        );
    }

    private static int defaultDurationDays(int punishCount) {
        if (punishCount <= 2) return 30;
        if (punishCount == 3) return 60;
        return -1;
    }

    private static Target resolveTarget(String playerName) {
        String typedName = playerName.trim();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(typedName)) {
                return new Target(online.getUniqueId(), online, online.getName(), online.getName());
            }
        }

        UUID economyUuid = org.atrimilan.sidastuffsmp.economy.EconomyDatabase.getUuidByName(typedName);
        if (economyUuid != null) {
            String dbName = org.atrimilan.sidastuffsmp.economy.EconomyDatabase.getPlayerName(economyUuid);
            if (dbName == null) dbName = typedName;
            Player online = Bukkit.getPlayer(economyUuid);
            return new Target(economyUuid, online, dbName, dbName);
        }

        UUID statsUuid = org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.getUuidByName(typedName);
        if (statsUuid != null) {
            String dbName = org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.getPlayerName(statsUuid);
            if (dbName == null) dbName = typedName;
            Player online = Bukkit.getPlayer(statsUuid);
            return new Target(statsUuid, online, dbName, dbName);
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(typedName);
        if (offline != null && offline.hasPlayedBefore()) {
            String cachedName = offline.getName();
            if (cachedName != null) {
                return new Target(offline.getUniqueId(), offline.getPlayer(), cachedName, cachedName);
            }
        }

        return null;
    }

    private record Target(UUID playerId, Player onlinePlayer, String displayName, String banName) {}
    private record BanResult() {}
    public record RestoreResult(boolean restored, boolean balanceRestored, boolean statsRestored) {}
    public record AutoPunishResult(String playerName, int punishCount, boolean permanent, int durationDays) {}
}
