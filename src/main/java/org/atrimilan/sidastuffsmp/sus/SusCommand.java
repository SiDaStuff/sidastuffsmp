package org.atrimilan.sidastuffsmp.sus;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SusCommand {

    public static final String DESCRIPTION = "View suspicious players list";
    public static final java.util.Set<String> ALIASES = java.util.Set.of();
    public static final String FLAGGERADD_DESCRIPTION = "Add player to sus list (alias for /sus add)";
    public static final java.util.Set<String> FLAGGERADD_ALIASES = java.util.Set.of();

    private static final String PERM = "sidastuffsmp.sus";
    private static final String PERM_REMOVE = "sidastuffsmp.sus.remove";
    private static final String PERM_ADD = "sidastuffsmp.sus.add";
    private static final String PERM_CLEAR = "sidastuffsmp.sus.clear";
    private static final int ITEMS_PER_PAGE = 45;
    private static final int GUI_SIZE = 54;

    private SusCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("sus")
                .requires(s -> s.getSender().hasPermission(PERM))
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        openSusGui(player, 0);
                    } else {
                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Only players can use this!", NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("remove")
                        .requires(s -> s.getSender().hasPermission(PERM_REMOVE))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    boolean removed = SusManager.removeSus(playerName);
                                    if (removed) {
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Removed " + playerName + " from the sus list.", NamedTextColor.GREEN));
                                    } else {
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed(playerName + " is not on the sus list.", NamedTextColor.RED));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("add")
                        .requires(s -> s.getSender().hasPermission(PERM_ADD))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String playerName = StringArgumentType.getString(ctx, "player");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            Player target = Bukkit.getPlayerExact(playerName);
                                            if (target != null) {
                                                SusManager.addPlayer(target.getUniqueId(), target.getName(), reason, "manual");
                                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Added " + playerName + " to the sus list for: " + reason, NamedTextColor.GREEN));
                                            } else {
                                                @SuppressWarnings("deprecation")
                                                OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
                                                SusManager.addPlayer(offline.getUniqueId(), playerName, reason, "manual");
                                                ctx.getSource().getSender().sendMessage(Chat.prefixed("Added " + playerName + " to the sus list for: " + reason, NamedTextColor.GREEN));
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("clear")
                        .requires(s -> s.getSender().hasPermission(PERM_CLEAR))
                        .executes(ctx -> {
                            SusManager.clearAll();
                            ctx.getSource().getSender().sendMessage(Chat.prefixed("Cleared the entire sus list.", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    public static void openSusGui(Player viewer, int page) {
        List<SusManager.SusEntry> entries = SusManager.getAllEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        page = Math.min(page, totalPages - 1);
        page = Math.max(0, page);

        SusGuiHolder holder = new SusGuiHolder(page);
        Inventory inv = Bukkit.createInventory(holder, GUI_SIZE, Component.text("Suspicious Players", NamedTextColor.DARK_RED));

        int offset = page * ITEMS_PER_PAGE;
        int end = Math.min(offset + ITEMS_PER_PAGE, entries.size());
        for (int i = offset; i < end; i++) {
            SusManager.SusEntry entry = entries.get(i);
            inv.setItem(i - offset, createSusHead(entry));
        }

        if (page > 0) {
            inv.setItem(45, createControlItem(Material.ARROW, "Previous Page", List.of(
                    Component.text("Page " + page, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, createControlItem(Material.ARROW, "Next Page", List.of(
                    Component.text("Page " + (page + 2), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }

        ItemStack closeItem = createControlItem(Material.BARRIER, "Close", List.of());
        inv.setItem(49, closeItem);

        viewer.openInventory(inv);
    }

    private static ItemStack createSusHead(SusManager.SusEntry entry) {
        boolean isGeyser = isGeyserPlayer(entry.playerUuid());
        Material displayMat = isGeyser ? Material.PAPER : Material.PLAYER_HEAD;
        ItemStack head = new ItemStack(displayMat);
        ItemMeta meta = head.getItemMeta();
        if (meta == null) return head;

        if (!isGeyser && meta instanceof SkullMeta skullMeta) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.playerUuid());
            skullMeta.setOwningPlayer(offlinePlayer);
        }

        meta.displayName(Component.text(entry.playerName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Flags:", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        for (String flag : entry.flags()) {
            lore.add(Component.text("  - " + flag, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to report in chat", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    static boolean isGeyserPlayer(UUID uuid) {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = geyserApiClass.getMethod("api").invoke(null);
            Object connection = geyserApiClass.getMethod("connection", UUID.class).invoke(api, uuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static LiteralCommandNode<CommandSourceStack> createFlaggerAddCommand() {
        return Commands.literal("flaggeradd")
                .requires(s -> s.getSender().hasPermission(PERM_ADD))
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String playerName = StringArgumentType.getString(ctx, "player");
                                    String reason = StringArgumentType.getString(ctx, "reason");
                                    Player target = Bukkit.getPlayerExact(playerName);
                                    if (target != null) {
                                        SusManager.addPlayer(target.getUniqueId(), target.getName(), reason, "flaggeradd");
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Added " + playerName + " to the sus list for: " + reason, NamedTextColor.GREEN));
                                    } else {
                                        @SuppressWarnings("deprecation")
                                        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
                                        SusManager.addPlayer(offline.getUniqueId(), playerName, reason, "flaggeradd");
                                        ctx.getSource().getSender().sendMessage(Chat.prefixed("Added " + playerName + " to the sus list for: " + reason, NamedTextColor.GREEN));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }
}
