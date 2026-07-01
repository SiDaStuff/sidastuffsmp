package org.atrimilan.sidastuffsmp.home;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HomeAdminGuiListener implements Listener {

    private static final int[] HOME_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final int[] DELETE_SLOTS = {18, 19, 20, 21, 23, 24, 25, 26};

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeAdminGuiHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains("Confirm Delete")) {
            handleConfirmDelete(admin, holder, slot);
            return;
        }

        if (slot == 35) {
            admin.closeInventory();
            return;
        }

        if (slot == 36 - 9) {
            if (holder.getPage() > 0) {
                admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                HomeAdminGui.open(admin, holder.getOwnerUuid(), holder.getOwnerName(), holder.getPage() - 1);
            }
            return;
        }

        if (slot == 36 - 5) {
            int maxPage = Math.max(0, (HomeConfig.maxHomes() - 1) / 8);
            if (holder.getPage() < maxPage) {
                admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                HomeAdminGui.open(admin, holder.getOwnerUuid(), holder.getOwnerName(), holder.getPage() + 1);
            }
            return;
        }

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            if (HOME_SLOTS[i] == slot) {
                int homeSlot = i + 1 + (holder.getPage() * 8);
                if (homeSlot > HomeConfig.maxHomes()) return;
                Home home = HomeManager.getHome(holder.getOwnerUuid(), homeSlot);
                if (home == null) return;
                Location loc = home.toLocation(null);
                if (loc == null || loc.getWorld() == null) {
                    admin.sendMessage(Chat.prefixed("World '" + home.getWorld() + "' is not loaded!", NamedTextColor.RED));
                    return;
                }
                admin.teleportAsync(loc).thenAccept(success -> {
                    if (success) {
                        admin.sendMessage(Chat.prefixed("Teleported to " + holder.getOwnerName()
                                + "'s home '" + home.getName() + "'!", NamedTextColor.GREEN));
                        admin.playSound(admin.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    } else {
                        admin.sendMessage(Chat.prefixed("Teleport failed.", NamedTextColor.RED));
                    }
                });
                return;
            }
        }

        for (int i = 0; i < DELETE_SLOTS.length; i++) {
            if (DELETE_SLOTS[i] == slot) {
                int homeSlot = i + 1 + (holder.getPage() * 8);
                if (homeSlot > HomeConfig.maxHomes()) return;
                Home home = HomeManager.getHome(holder.getOwnerUuid(), homeSlot);
                if (home != null) {
                    HomeAdminGui.openConfirmDelete(admin, holder.getOwnerUuid(), holder.getOwnerName(),
                            homeSlot, home.getName());
                }
                return;
            }
        }
    }

    private void handleConfirmDelete(Player admin, HomeAdminGuiHolder holder, int slot) {
        if (slot == 11) {
            int homeSlot = holder.getPendingDeleteSlot();
            if (homeSlot > 0) {
                Home home = HomeManager.getHome(holder.getOwnerUuid(), homeSlot);
                if (home != null) {
                    HomeManager.deleteHome(holder.getOwnerUuid(), homeSlot);
                    admin.sendMessage(Chat.prefixed("Deleted home '" + home.getName() + "' for "
                            + holder.getOwnerName() + ".", NamedTextColor.GREEN));
                }
            }
            admin.closeInventory();
        } else if (slot == 15) {
            HomeAdminGui.open(admin, holder.getOwnerUuid(), holder.getOwnerName(), holder.getPage());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // nothing to clean up
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // nothing to clean up
    }
}