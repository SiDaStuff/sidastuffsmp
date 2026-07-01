package org.atrimilan.sidastuffsmp.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyGui {

    private static final int SLOT_BACK = 45;

    private BountyGui() {}

    public static Inventory open(Player player) {
        BountyGuiHolder holder = new BountyGuiHolder();
        holder.setViewerUuid(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("Bounties"));

        List<BountyManager.BountyEntry> topBounties = BountyManager.getTopBounties(45);
        for (int i = 0; i < topBounties.size(); i++) {
            BountyManager.BountyEntry entry = topBounties.get(i);
            inv.setItem(i, createBountyItem(entry));
        }

        inv.setItem(SLOT_BACK, createControlItem(Material.BARRIER, "Back", List.of(
                Component.text("Close GUI", NamedTextColor.GRAY)
        )));

        return inv;
    }

    private static ItemStack createBountyItem(BountyManager.BountyEntry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.targetUuid()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Bounty: " + BountyManager.formatPrice(entry.amount()), NamedTextColor.GOLD));
            lore.add(Component.empty());
            lore.add(Component.text("Click to add/remove bounty", NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true));
            
            skullMeta.displayName(Component.text(entry.targetName(), NamedTextColor.YELLOW));
            skullMeta.lore(lore);
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private static ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
