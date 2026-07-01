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

import java.util.ArrayList;
import java.util.List;

public class SettingsGui {

    public static final int SLOT_TPA = 10;
    public static final int SLOT_TPA_HERE = 12;
    public static final int SLOT_TPA_AUTO = 14;
    public static final int SLOT_NIGHT_VISION = 16;
    public static final int SLOT_ORDER_MESSAGES = 28;
    public static final int SLOT_AUCTION_MESSAGES = 30;
    public static final int SLOT_CONFIRM_AUCTION = 32;
    public static final int SLOT_CONFIRM_TP = 34;
    public static final int SLOT_CLOSE = 40;

    private SettingsGui() {}

    public static Inventory open(Player player) {
        PlayerSettings settings = PlayerSettings.get(player.getUniqueId());

        TeleportGuiHolder holder = new TeleportGuiHolder(TeleportGuiHolder.GuiType.SETTINGS);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, 45, Component.text("Settings"));

        inv.setItem(SLOT_TPA, createToggleItem(
                Material.ENDER_PEARL,
                "TPA Requests",
                settings.isTpaEnabled(),
                "Allow players to teleport to you"
        ));

        inv.setItem(SLOT_TPA_HERE, createToggleItem(
                Material.ENDER_EYE,
                "TPAhere Requests",
                settings.isTpaHereEnabled(),
                "Allow players to summon you"
        ));

        inv.setItem(SLOT_TPA_AUTO, createToggleItem(
                Material.COMPASS,
                "TPAuto",
                settings.isTpaAutoEnabled(),
                "Auto-accept teleport requests"
        ));

        inv.setItem(SLOT_NIGHT_VISION, createToggleItem(
                Material.GOLDEN_CARROT,
                "Night Vision",
                settings.isNightVisionEnabled(),
                "Infinite night vision effect"
        ));

        inv.setItem(SLOT_ORDER_MESSAGES, createToggleItem(
                Material.PAPER,
                "Order Messages",
                settings.isOrderMessagesEnabled(),
                "Order notifications"
        ));

        inv.setItem(SLOT_AUCTION_MESSAGES, createToggleItem(
                Material.EMERALD,
                "Auction Messages",
                settings.isAuctionMessagesEnabled(),
                "Auction notifications"
        ));

        inv.setItem(SLOT_CONFIRM_AUCTION, createToggleItem(
                Material.WRITABLE_BOOK,
                "Confirm Auction Listing",
                settings.isConfirmAuctionListing(),
                "Ask before listing auction item"
        ));

        inv.setItem(SLOT_CONFIRM_TP, createToggleItem(
                Material.ENDER_PEARL,
                "Confirm TP GUI",
                settings.isConfirmTpGui(),
                "Show confirmation before sending TP request"
        ));

        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            closeItem.setItemMeta(closeMeta);
        }
        inv.setItem(SLOT_CLOSE, closeItem);

        player.openInventory(inv);
        return inv;
    }

    private static ItemStack createToggleItem(Material material, String name, boolean enabled, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor statusColor = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
            String statusText = enabled ? "\u2714 ON" : "\u2718 OFF";

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(description, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(statusText, statusColor)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
