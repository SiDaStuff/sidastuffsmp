package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class BountyGuiUtil {
    public static final int BROWSER_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int CONTROL_BAR_START = 45;
    public static final int SLOT_PREV_PAGE = 45;
    public static final int SLOT_SEARCH = 46;
    public static final int SLOT_MY_BOUNTIES = 47;
    public static final int SLOT_NEXT_PAGE = 48;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_REFRESH = 50;

    private BountyGuiUtil() {}

    public static ItemStack createBountyItem(OfflinePlayer target, double amount, int rank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            String rankStr = rank <= 3 ? "#" + rank : "#" + rank;
            meta.displayName(Component.text(rankStr + " " + target.getName(), NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Bounty: " + BountyManager.formatPrice(amount), NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Target: " + target.getName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to view details", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createPlayerHeadItem(OfflinePlayer player, double currentBounty) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(Component.text(player.getName(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (currentBounty > 0) {
                lore.add(Component.text("Current Bounty: " + BountyManager.formatPrice(currentBounty), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("No bounty on this player", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Click to add bounty", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            if (currentBounty > 0) {
                lore.add(Component.text("Right-click to remove bounty", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack prevPageItem(boolean hasPrev) {
        if (!hasPrev) return null;
        return createControlItem(Material.ARROW, "Previous Page", List.of(
                Component.text("Click to go back", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack nextPageItem(boolean hasNext) {
        if (!hasNext) return null;
        return createControlItem(Material.ARROW, "Next Page", List.of(
                Component.text("Click to go forward", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack closeItem() {
        return createControlItem(Material.BARRIER, "Close", null);
    }

    public static ItemStack fillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void fillEmptySlots(Inventory inv, ItemStack filler) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, filler);
            }
        }
    }

    public static ItemStack refreshItem() {
        return createControlItem(Material.ANVIL, "Refresh", List.of(
                Component.text("Click to refresh bounties", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack myBountiesItem() {
        return createControlItem(Material.CHEST, "My Bounties", List.of(
                Component.text("View bounties you've set", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack searchItem() {
        return createControlItem(Material.NAME_TAG, "Search", List.of(
                Component.text("Click to search by player name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack searchItemWithTerm(String term) {
        return createControlItem(Material.NAME_TAG, "Search: " + term, List.of(
                Component.text("Click to search by player name", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack addBountyItem(double amount) {
        return createControlItem(Material.EMERALD, "Add Bounty", List.of(
                Component.text("Click to add a bounty", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Amount: " + BountyManager.formatPrice(amount), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack removeBountyItem(double amount) {
        return createControlItem(Material.REDSTONE, "Remove Bounty", List.of(
                Component.text("Click to remove your bounty", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Amount: " + BountyManager.formatPrice(amount), NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
        ));
    }

    public static ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}