package org.atrimilan.sidastuffsmp.stats;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StatsCommand {

    public static final String DESCRIPTION = "View player statistics";
    public static final Set<String> ALIASES = Set.of("playerstats", "statistics");

    private static final String PERMISSION = "sidastuffsmp.stats";
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private StatsCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("stats")
                .requires(sender -> sender.getSender().hasPermission(PERMISSION))
                .executes(StatsCommand::runSelf)
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                builder.suggest(p.getName());
                            }
                            return builder.buildFuture();
                        })
                        .executes(StatsCommand::runLookup))
                .build();
    }

    private static int runSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        openStatsGui(player, player);
        return Command.SINGLE_SUCCESS;
    }

    private static int runLookup(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this command!", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "player");
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(Chat.prefixed("Player not found.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        Player onlineTarget = target.getPlayer();
        openStatsGui(player, onlineTarget != null ? onlineTarget : target);
        return Command.SINGLE_SUCCESS;
    }

    private static void openStatsGui(Player viewer, OfflinePlayer target) {
        UUID targetUuid = target.getUniqueId();
        PlayerStatsManager.PlayerStats stats = PlayerStatsManager.getStats(targetUuid);
        double balance = EconomyManager.getBalance(targetUuid);

        Inventory inv = Bukkit.createInventory(new StatsGuiHolder(), 27,
                Component.text(target.getName() + "'s Stats"));

        inv.setItem(11, createStatItem(Material.GOLD_INGOT, "Balance",
                List.of(
                        Component.empty(),
                        Component.text("$" + String.format("%,.2f", balance), NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(12, createStatItem(Material.CLOCK, "Playtime",
                List.of(
                        Component.empty(),
                        Component.text(formatPlaytime(stats.playtimeSeconds()), NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(13, createStatItem(Material.DIAMOND_SWORD, "Kills",
                List.of(
                        Component.empty(),
                        Component.text(String.valueOf(stats.kills()), NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(14, createStatItem(Material.SKELETON_SKULL, "Deaths",
                List.of(
                        Component.empty(),
                        Component.text(String.valueOf(stats.deaths()), NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(15, createStatItem(Material.NETHERITE_INGOT, "K/D Ratio",
                List.of(
                        Component.empty(),
                        Component.text(DF.format(stats.kdr()), NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.BOLD, true)
                                .decoration(TextDecoration.ITALIC, false))));

        inv.setItem(16, createStatItem(Material.ENDER_PEARL, "RTP Count",
                List.of(
                        Component.empty(),
                        Component.text(String.valueOf(stats.rtpCount()), NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, false))));

        viewer.openInventory(inv);
    }

    public static class StatsGuiHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }

    public static void refreshStatsGui(Player viewer, OfflinePlayer target) {
        openStatsGui(viewer, target);
    }

    private static ItemStack createStatItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatPlaytime(int seconds) {
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }
}