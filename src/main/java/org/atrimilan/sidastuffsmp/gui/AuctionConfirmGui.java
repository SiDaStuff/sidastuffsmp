package org.atrimilan.sidastuffsmp.gui;

import org.atrimilan.sidastuffsmp.auction.AuctionListing;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.utils.ShulkerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public class AuctionConfirmGui {

    public static final int CONFIRM_SIZE = 27;
    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_ITEM = 13;
    public static final int SLOT_CANCEL = 15;
    public static final int SLOT_SHULKER_CONTENTS = 20; // Slot for showing shulker contents

    private AuctionConfirmGui() {}

    public static Inventory openPurchaseConfirm(Player player, int listingId) {
        AuctionListing listing = AuctionManager.getListingById(listingId);
        if (listing == null) return null;

        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.GuiType.CONFIRM_PURCHASE);
        holder.setListingId(listingId);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, CONFIRM_SIZE,
                Component.text("Confirm Purchase"));

        ItemStack displayItem = AuctionManager.deserializeItem(listing.itemBase64());
        if (displayItem != null) {
            if (displayItem.hasItemMeta() && displayItem.getItemMeta() != null && displayItem.getItemMeta().hasEnchants()) {
                AuctionGuiUtil.addEnchantmentGlow(displayItem);
            }
            var meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Seller: " + listing.sellerName(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                
                // If it's a shulker box, show contents in lore
                if (ShulkerUtil.isShulkerBox(displayItem)) {
                    List<ItemStack> contents = ShulkerUtil.extractItemsFromShulker(displayItem);
                    if (!contents.isEmpty()) {
                        lore.add(Component.empty());
                        lore.add(Component.text("Shulker Contents:", NamedTextColor.AQUA)
                                .decoration(TextDecoration.BOLD, true)
                                .decoration(TextDecoration.ITALIC, false));
                        for (ItemStack content : contents) {
                            if (content != null && !content.getType().isAir()) {
                                String name = formatMaterialName(content.getType());
                                lore.add(Component.text("  " + content.getAmount() + "x " + name, NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false));
                            }
                        }
                    } else {
                        lore.add(Component.empty());
                        lore.add(Component.text("Shulker is empty", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(SLOT_ITEM, displayItem);
        }

        inv.setItem(SLOT_CONFIRM,
                AuctionGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                        "Confirm - " + AuctionManager.formatPrice(listing.price()), List.of(
                                Component.text("Click to purchase", NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false)
                        )));

        inv.setItem(SLOT_CANCEL, AuctionGuiUtil.cancelButtonItem());

        AuctionGuiUtil.fillEmptySlots(inv, AuctionGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    private static String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public static Inventory openCancelConfirm(Player player, int listingId) {
        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.GuiType.CONFIRM_CANCEL);
        holder.setListingId(listingId);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, CONFIRM_SIZE,
                Component.text("Confirm Cancel"));

        ItemStack displayItem = null;
        AuctionListing listing = AuctionManager.getListingById(listingId);
        if (listing != null) {
            displayItem = AuctionManager.deserializeItem(listing.itemBase64());
            if (displayItem != null && displayItem.hasItemMeta() && displayItem.getItemMeta() != null && displayItem.getItemMeta().hasEnchants()) {
                AuctionGuiUtil.addEnchantmentGlow(displayItem);
            }
        }
        if (displayItem != null) {
            var meta = displayItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Price: " + AuctionManager.formatPrice(listing.price()), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Seller: " + listing.sellerName(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                
                // If it's a shulker box, show contents in lore
                if (ShulkerUtil.isShulkerBox(displayItem)) {
                    List<ItemStack> contents = ShulkerUtil.extractItemsFromShulker(displayItem);
                    if (!contents.isEmpty()) {
                        lore.add(Component.empty());
                        lore.add(Component.text("Shulker Contents:", NamedTextColor.AQUA)
                                .decoration(TextDecoration.BOLD, true)
                                .decoration(TextDecoration.ITALIC, false));
                        for (ItemStack content : contents) {
                            if (content != null && !content.getType().isAir()) {
                                String name = formatMaterialName(content.getType());
                                lore.add(Component.text("  " + content.getAmount() + "x " + name, NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false));
                            }
                        }
                    } else {
                        lore.add(Component.empty());
                        lore.add(Component.text("Shulker is empty", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                }
                
                meta.lore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(SLOT_ITEM, displayItem);
        }

        inv.setItem(SLOT_CONFIRM,
                AuctionGuiUtil.createControlItem(Material.ORANGE_STAINED_GLASS_PANE, "Confirm Cancel", List.of(
                        Component.text("Item will be returned to you", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Click to confirm", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false)
                )));

        inv.setItem(SLOT_CANCEL, AuctionGuiUtil.cancelButtonItem());

        AuctionGuiUtil.fillEmptySlots(inv, AuctionGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }
}
