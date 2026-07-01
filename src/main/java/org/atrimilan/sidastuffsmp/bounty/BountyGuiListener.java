package org.atrimilan.sidastuffsmp.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class BountyGuiListener implements Listener {

    // Amount selection inventory
    private static final Map<UUID, UUID> pendingTarget = new HashMap<>();
    private static final Map<UUID, Boolean> pendingMode = new HashMap<>(); // true = add, false = remove

    private static final int[] AMOUNT_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int SLOT_CONFIRM_ADD = 22;
    private static final int SLOT_CONFIRM_REMOVE = 24;
    private static final int SLOT_BACK = 49;

    // Preset amounts for quick selection
    private static final double[] PRESET_AMOUNTS = {100, 500, 1000, 5000, 10000, 50000, 100000};

    public BountyGuiListener() {}

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle AmountSelectHolder separately (doesn't extend BountyGuiHolder)
        if (event.getInventory().getHolder() instanceof AmountSelectHolder) {
            handleAmountSelectClick(event);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof BountyGuiHolder holder)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // Close button
        if (event.getSlot() == 45) {
            player.closeInventory();
            return;
        }

        // Clicking on heads does nothing - just a viewing list
    }

    private void handleAmountSelectClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (!(event.getInventory().getHolder() instanceof AmountSelectHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        int slot = event.getSlot();

        // Back button
        if (slot == SLOT_BACK) {
            player.closeInventory();
            BountyGui.open(player);
            return;
        }

        // Confirm add
        if (slot == SLOT_CONFIRM_ADD) {
            confirmAddBounty(player, holder);
            return;
        }

        // Confirm remove
        if (slot == SLOT_CONFIRM_REMOVE) {
            confirmRemoveBounty(player, holder);
            return;
        }

        // Amount preset selection
        for (int i = 0; i < AMOUNT_SLOTS.length; i++) {
            if (slot == AMOUNT_SLOTS[i] && i < PRESET_AMOUNTS.length) {
                double amount = PRESET_AMOUNTS[i];
                setSelectedAmount(player, amount, holder);
                return;
            }
        }

        // Check if clicked on one of the amount display items (to cycle through)
        if (clickedItem.getType() == Material.NETHER_STAR) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.lore() != null && !meta.lore().isEmpty()) {
                // Try to parse current amount and cycle
                Component lore = meta.lore().get(0);
                if (lore != null) {
                    String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore);
                    // Extract number from text like "Current: 1,000"
                    try {
                        String numStr = loreText.replaceAll("[^0-9.]", "");
                        if (!numStr.isEmpty()) {
                            double currentAmount = Double.parseDouble(numStr);
                            // Find next amount in cycle
                            double nextAmount = getNextAmount(currentAmount);
                            setSelectedAmount(player, nextAmount, holder);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private double getNextAmount(double current) {
        for (int i = 0; i < PRESET_AMOUNTS.length; i++) {
            if (PRESET_AMOUNTS[i] == current && i < PRESET_AMOUNTS.length - 1) {
                return PRESET_AMOUNTS[i + 1];
            }
        }
        // If not found or last, return first
        return PRESET_AMOUNTS[0];
    }

    private void setSelectedAmount(Player player, double amount, AmountSelectHolder holder) {
        holder.setSelectedAmount(amount);
        
        // Update the buttons to show the selected amount on confirm buttons
        // Note: In Paper 1.20+, inventory updates require reopening the inventory
        // For now, the holder stores the amount and confirm buttons read from it
    }

    private void openAmountSelectGui(Player player, UUID targetUuid, String targetName) {
        AmountSelectHolder holder = new AmountSelectHolder(targetUuid, targetName);
        Inventory inv = Bukkit.createInventory(holder, 54, 
                Component.text("Bounty: " + targetName));

        // Target skull
        ItemStack targetHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) targetHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            skullMeta.displayName(Component.text(targetName, NamedTextColor.YELLOW));
            skullMeta.lore(List.of(
                Component.text("Total Bounty: " + BountyManager.formatPrice(BountyManager.getTotalBounty(targetUuid)), NamedTextColor.GOLD),
                Component.text("Your bounties on this player: " + BountyManager.formatPrice(getPlayerBountyOnTarget(player, targetUuid)), NamedTextColor.GRAY)
            ));
            targetHead.setItemMeta(skullMeta);
        }
        inv.setItem(4, targetHead);

        // Amount selector
        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta selectorMeta = selector.getItemMeta();
        selectorMeta.displayName(Component.text("Selected: " + BountyManager.formatPrice(1000), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        selectorMeta.lore(List.of(
            Component.text("Click to cycle through amounts", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
        ));
        selector.setItemMeta(selectorMeta);
        inv.setItem(19, selector);

        // Preset amount buttons
        for (int i = 0; i < AMOUNT_SLOTS.length && i < PRESET_AMOUNTS.length; i++) {
            ItemStack amountBtn = createAmountButton(PRESET_AMOUNTS[i]);
            inv.setItem(AMOUNT_SLOTS[i], amountBtn);
        }

        // Custom amount info
        ItemStack customInfo = createControlItem(Material.PAPER, "Custom Amount",
                List.of(Component.text("Use /bounty add " + targetName + " <amount>", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                        Component.text("to set a custom amount", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
        inv.setItem(25, customInfo);

        // Add bounty button
        ItemStack addBtn = createControlItem(Material.LIME_STAINED_GLASS_PANE, "Add Bounty",
                List.of(Component.text("Click to add " + BountyManager.formatPrice(1000) + " bounty", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false),
                        Component.text("on " + targetName, NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)));
        inv.setItem(SLOT_CONFIRM_ADD, addBtn);

        // Remove bounty button
        double existingBounty = getPlayerBountyOnTarget(player, targetUuid);
        if (existingBounty > 0) {
            ItemStack removeBtn = createControlItem(Material.RED_STAINED_GLASS_PANE, "Remove Bounty",
                    List.of(Component.text("Click to remove " + BountyManager.formatPrice(Math.min(1000, existingBounty)), NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false),
                            Component.text("from bounty on " + targetName, NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_CONFIRM_REMOVE, removeBtn);
        } else {
            ItemStack disabledBtn = createControlItem(Material.GRAY_STAINED_GLASS_PANE, "Remove Bounty (None)",
                    List.of(Component.text("You have no bounties on this player", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            inv.setItem(SLOT_CONFIRM_REMOVE, disabledBtn);
        }

        // Back button
        ItemStack backBtn = createControlItem(Material.ARROW, "Back",
                List.of(Component.text("Return to bounty list", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        inv.setItem(SLOT_BACK, backBtn);

        player.openInventory(inv);
    }

    private ItemStack createAmountButton(double amount) {
        ItemStack btn = new ItemStack(Material.BEACON);
        ItemMeta meta = btn.getItemMeta();
        meta.displayName(Component.text(BountyManager.formatPrice(amount), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Click to select this amount", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
        ));
        btn.setItemMeta(meta);
        return btn;
    }

    private ItemStack createControlItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private double getPlayerBountyOnTarget(Player player, UUID targetUuid) {
        // This checks how much the player has added to bounties on this target
        List<BountyManager.BountyEntry> playerBounties = BountyManager.getBountiesSetBy(player.getUniqueId(), 100);
        for (BountyManager.BountyEntry entry : playerBounties) {
            if (entry.targetUuid().equals(targetUuid)) {
                return entry.amount();
            }
        }
        return 0;
    }

    private void confirmAddBounty(Player player, AmountSelectHolder holder) {
        double amount = holder.getSelectedAmount();
        if (amount <= 0) {
            player.sendMessage(Chat.prefixed("Please select a valid amount!", NamedTextColor.RED));
            return;
        }

        UUID targetUuid = holder.getTargetUuid();
        String targetName = holder.getTargetName();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);

        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(Chat.prefixed("Target player not found!", NamedTextColor.RED));
            return;
        }

        // Remove from cooldown check if implemented
        // For now, proceed with adding bounty
        double result = BountyManager.addBounty(player, target, amount);
        
        if (result >= 0) {
            player.sendMessage(Chat.prefixed("Successfully added " + BountyManager.formatPrice(amount) + 
                    " bounty on " + targetName + "!", NamedTextColor.GREEN));
            player.sendMessage(Chat.prefixed("Total bounty on " + targetName + ": " + 
                    BountyManager.formatPrice(result), NamedTextColor.GOLD));
        }
        // Error messages are already sent by BountyManager

        player.closeInventory();
    }

    private void confirmRemoveBounty(Player player, AmountSelectHolder holder) {
        double amount = holder.getSelectedAmount();
        if (amount <= 0) {
            player.sendMessage(Chat.prefixed("Please select a valid amount!", NamedTextColor.RED));
            return;
        }

        UUID targetUuid = holder.getTargetUuid();
        String targetName = holder.getTargetName();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);

        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(Chat.prefixed("Target player not found!", NamedTextColor.RED));
            return;
        }

        boolean success = BountyManager.removeBounty(player, target, amount);
        
        if (success) {
            player.sendMessage(Chat.prefixed("Successfully removed " + BountyManager.formatPrice(amount) + 
                    " from bounty on " + targetName + "!", NamedTextColor.GREEN));
        }
        // Error messages are already sent by BountyManager

        player.closeInventory();
    }

    // Holder class for the amount selection GUI
    public static class AmountSelectHolder implements org.bukkit.inventory.InventoryHolder {
        private final UUID targetUuid;
        private final String targetName;
        private double selectedAmount = 1000.0;

        public AmountSelectHolder(UUID targetUuid, String targetName) {
            this.targetUuid = targetUuid;
            this.targetName = targetName;
        }

        public UUID getTargetUuid() {
            return targetUuid;
        }

        public String getTargetName() {
            return targetName;
        }

        public double getSelectedAmount() {
            return selectedAmount;
        }

        public void setSelectedAmount(double amount) {
            this.selectedAmount = amount;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
