package org.atrimilan.sidastuffsmp.teleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TeleportConfirmGui {

    public static final int SIZE = 27;
    public static final int SLOT_CANCEL = 10;
    public static final int SLOT_DIMENSION = 12;
    public static final int SLOT_PLAYER_HEAD = 13;
    public static final int SLOT_PING = 14;
    public static final int SLOT_CONFIRM = 16;

    private TeleportConfirmGui() {}

    public static Inventory openSendConfirm(Player sender, Player target, TeleportRequest.Type type) {
        TeleportGuiHolder holder = new TeleportGuiHolder(TeleportGuiHolder.GuiType.SEND_CONFIRM);
        holder.setViewerUuid(sender.getUniqueId());
        holder.setTargetUuid(target.getUniqueId());
        holder.setRequestType(type);

        String title = type == TeleportRequest.Type.TPA
                ? "Teleport to " + target.getName() + "?"
                : "Teleport " + target.getName() + " to you?";

        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text(title));

        inv.setItem(SLOT_CANCEL, createCancelItem());
        inv.setItem(SLOT_DIMENSION, createDimensionItem(target));
        inv.setItem(SLOT_PLAYER_HEAD, createPlayerHeadItem(target, type, true));
        inv.setItem(SLOT_PING, createPingItem(target));
        inv.setItem(SLOT_CONFIRM, createConfirmItem());

        sender.openInventory(inv);
        return inv;
    }

    public static Inventory openReceiveConfirm(Player target, TeleportRequest request) {
        TeleportGuiHolder holder = new TeleportGuiHolder(TeleportGuiHolder.GuiType.RECEIVE_CONFIRM);
        holder.setViewerUuid(target.getUniqueId());
        holder.setRequestSenderUuid(request.senderUuid());
        holder.setRequestType(request.type());

        String title = request.type() == TeleportRequest.Type.TPA
                ? request.senderName() + " wants to teleport to you"
                : request.senderName() + " wants you to teleport to them";

        Inventory inv = Bukkit.createInventory(holder, SIZE, Component.text(title));

        Player sender = Bukkit.getPlayer(request.senderUuid());

        inv.setItem(SLOT_CANCEL, createDenyItem());
        inv.setItem(SLOT_DIMENSION, sender != null ? createDimensionItem(sender) : createUnknownDimensionItem());
        inv.setItem(SLOT_PLAYER_HEAD, createPlayerHeadItem(sender, request.type(), false));
        inv.setItem(SLOT_PING, sender != null ? createPingItem(sender) : createOfflinePingItem());
        inv.setItem(SLOT_CONFIRM, createAcceptItem());

        target.openInventory(inv);
        return inv;
    }

    private static ItemStack createPlayerHeadItem(Player otherPlayer, TeleportRequest.Type type, boolean isSender) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (otherPlayer != null) {
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(otherPlayer);
                head.setItemMeta(skullMeta);
                meta = head.getItemMeta();
            }
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                if (isSender) {
                    lore.add(Component.text(type == TeleportRequest.Type.TPA
                            ? "You \u2192 " + otherPlayer.getName()
                            : otherPlayer.getName() + " \u2192 You", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text(type == TeleportRequest.Type.TPA
                            ? otherPlayer.getName() + " \u2192 You"
                            : "You \u2192 " + otherPlayer.getName(), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
                meta.displayName(Component.text(otherPlayer.getName(), NamedTextColor.WHITE)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false));
                head.setItemMeta(meta);
            }
        }
        return head;
    }

    private static ItemStack createDimensionItem(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        Material dimMat;
        NamedTextColor color;
        String dimName;
        if (worldName.contains("nether")) {
            dimMat = Material.NETHERRACK;
            color = NamedTextColor.RED;
            dimName = "Nether";
        } else if (worldName.contains("the_end") || worldName.contains("end")) {
            dimMat = Material.END_STONE;
            color = NamedTextColor.LIGHT_PURPLE;
            dimName = "The End";
        } else {
            dimMat = Material.GRASS_BLOCK;
            color = NamedTextColor.GREEN;
            dimName = "Overworld";
        }

        ItemStack item = new ItemStack(dimMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(dimName, color)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Location", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createUnknownDimensionItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Unknown", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Location", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createPingItem(Player player) {
        int ping = player.getPing();
        NamedTextColor pingColor;
        if (ping <= 50) {
            pingColor = NamedTextColor.GREEN;
        } else if (ping <= 100) {
            pingColor = NamedTextColor.YELLOW;
        } else if (ping <= 200) {
            pingColor = NamedTextColor.GOLD;
        } else {
            pingColor = NamedTextColor.RED;
        }

        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(ping + "ms", pingColor)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Ping", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createOfflinePingItem() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Offline", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Ping", NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Click to confirm", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Confirm", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createAcceptItem() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Click to accept", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Accept", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Click to cancel", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Cancel", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createDenyItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Click to deny", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.displayName(Component.text("Deny", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
