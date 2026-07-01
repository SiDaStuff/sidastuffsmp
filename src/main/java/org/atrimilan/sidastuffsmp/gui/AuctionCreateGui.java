package org.atrimilan.sidastuffsmp.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionCreateGui {

    private static final Map<UUID, Double> PENDING_LISTINGS = new ConcurrentHashMap<>();

    private AuctionCreateGui() {}

    public static Inventory openCreateConfirm(Player player, double price) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir()) return null;

        PENDING_LISTINGS.put(player.getUniqueId(), price);

        AuctionGuiHolder holder = new AuctionGuiHolder(AuctionGuiHolder.GuiType.CREATE);
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, AuctionConfirmGui.CONFIRM_SIZE,
                Component.text("List Item"));

        ItemStack displayClone = handItem.clone();
        inv.setItem(AuctionConfirmGui.SLOT_ITEM, displayClone);

        inv.setItem(AuctionConfirmGui.SLOT_CONFIRM,
                AuctionGuiUtil.createControlItem(Material.LIME_STAINED_GLASS_PANE,
                        "List for " + AuctionManager.formatPrice(price), List.of(
                                Component.text("Click to confirm listing", NamedTextColor.GRAY)
                                        .decoration(TextDecoration.ITALIC, false)
                        )));

        inv.setItem(AuctionConfirmGui.SLOT_CANCEL, AuctionGuiUtil.cancelButtonItem());

        AuctionGuiUtil.fillEmptySlots(inv, AuctionGuiUtil.fillerItem());

        player.openInventory(inv);
        return inv;
    }

    public static double getPendingPrice(UUID uuid) {
        Double price = PENDING_LISTINGS.remove(uuid);
        return price != null ? price : -1;
    }
}