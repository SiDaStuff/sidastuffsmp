package org.atrimilan.sidastuffsmp.home;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HomeGuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeGuiHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (holder.getViewerUuid() != null && !holder.getViewerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains("Select Home Icon")) {
            handleIconPickerClick(player, holder, slot, event);
            return;
        }
        if (title.contains("Delete Home")) {
            handleConfirmDeleteClick(player, holder, slot);
            return;
        }
        if (title.contains("Edit Home")) {
            handleEditMenuClick(player, holder, slot);
            return;
        }
        if (title.contains("My Homes")) {
            handleHomeGuiClick(player, holder, slot, event);
        }
    }

    private void handleHomeGuiClick(Player player, HomeGuiHolder holder, int slot, InventoryClickEvent event) {
        if (slot == HomeGui.SLOT_SEARCH) {
            String currentTerm = holder.getSearchTerm();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Search Homes", "Type home name:",
                    currentTerm,
                    text -> Bukkit.getScheduler().runTaskLater(
                            SiDaStuffSmp.getInstance(),
                            () -> HomeGui.open(player, text),
                            2L),
                    () -> HomeGui.open(player, currentTerm));
            return;
        }

        if (slot == HomeGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        Integer homeSlotIdx = homeSlotFromInventorySlot(slot);
        if (homeSlotIdx == null) return;

        int homeSlotNum = homeSlotIdx + 1;
        Home home = HomeManager.getHome(player.getUniqueId(), homeSlotNum);

        if (event.isShiftClick()) {
            if (home != null) {
                HomeGui.openConfirmDelete(player, home.getSlot(), home.getName());
            } else {
                player.sendMessage(Chat.prefixed("This slot is empty.", NamedTextColor.GRAY));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            return;
        }

        if (event.isRightClick()) {
            if (home != null) {
                HomeGui.openEditMenu(player, home.getSlot(), home.getName());
            } else {
                player.sendMessage(Chat.prefixed("Set the home first before editing.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            return;
        }

        if (home != null) {
            player.closeInventory();
            HomeTeleportManager.teleportToHome(player, home);
        } else {
            HomeGui.requestSetHomeName(player, homeSlotNum);
        }
    }

    private void handleEditMenuClick(Player player, HomeGuiHolder holder, int slot) {
        int homeSlot = holder.getPendingDeleteSlot();
        if (homeSlot <= 0) {
            player.closeInventory();
            return;
        }

        Home home = HomeManager.getHome(player.getUniqueId(), homeSlot);

        if (slot == HomeGui.SLOT_EDIT_NAME) {
            if (home != null) {
                HomeGui.requestRenameHome(player, homeSlot, home.getName());
            }
            return;
        }

        if (slot == HomeGui.SLOT_EDIT_ICON) {
            if (home != null) {
                HomeGui.openIconPicker(player, homeSlot);
            }
            return;
        }

        if (slot == 22) {
            HomeGui.open(player);
            return;
        }
    }

    private Integer homeSlotFromInventorySlot(int slot) {
        for (int i = 0; i < HomeGui.HOME_SLOTS.length; i++) {
            if (HomeGui.HOME_SLOTS[i] == slot) return i;
        }
        return null;
    }

    private void handleIconPickerClick(Player player, HomeGuiHolder holder, int slot, InventoryClickEvent event) {
        int homeSlot = holder.getHomeIconSelectionSlot();
        if (homeSlot <= 0) return;

        int SLOT_PREV = 45;
        int SLOT_SEARCH = 46;
        int SLOT_BACK = 49;
        int SLOT_NEXT = 52;
        int SLOT_CLOSE = 53;

        if (slot == SLOT_BACK) {
            HomeGui.open(player);
            return;
        }

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot == SLOT_SEARCH) {
            HomeIconGui.IconBrowserState iconState = HomeIconGui.getBrowserState(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            AnvilInput.request(player, "Search Icon", "Search icon name:",
                    iconState.getSearchTerm(),
                    searchTerm -> Bukkit.getScheduler().runTaskLater(
                            SiDaStuffSmp.getInstance(),
                            () -> HomeIconGui.open(player, homeSlot, 0, searchTerm),
                            2L),
                    () -> HomeIconGui.open(player, homeSlot, 0, null));
            return;
        }

        if (slot == SLOT_PREV) {
            HomeIconGui.IconBrowserState state = HomeIconGui.getBrowserState(player.getUniqueId());
            int newPage = state.getPage() - 1;
            if (newPage >= 0) {
                HomeIconGui.open(player, homeSlot, newPage, state.getSearchTerm());
            }
            return;
        }

        if (slot == SLOT_NEXT) {
            HomeIconGui.IconBrowserState state = HomeIconGui.getBrowserState(player.getUniqueId());
            HomeIconGui.open(player, homeSlot, state.getPage() + 1, state.getSearchTerm());
            return;
        }

        if (slot >= 0 && slot < 45) {
            org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            String iconName = clicked.getType().name();
            HomeManager.setHomeIcon(player.getUniqueId(), homeSlot, iconName);
            player.sendMessage(Chat.prefixed("Home icon set to " + iconName + ".", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                if (player.isOnline()) HomeGui.open(player);
            }, 2L);
        }
    }

    private void handleConfirmDeleteClick(Player player, HomeGuiHolder holder, int slot) {
        int homeSlot = holder.getPendingDeleteSlot();
        if (homeSlot <= 0) {
            player.closeInventory();
            return;
        }

        Home home = HomeManager.getHome(player.getUniqueId(), homeSlot);

        if (slot == HomeGui.SLOT_CONFIRM) {
            if (home != null) {
                HomeManager.deleteHome(player.getUniqueId(), homeSlot);
                player.sendMessage(Chat.prefixed("Home '" + home.getName() + "' deleted.", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                if (player.isOnline()) HomeGui.open(player);
            }, 2L);
        } else if (slot == HomeGui.SLOT_CANCEL) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.getInstance(), () -> {
                if (player.isOnline()) HomeGui.open(player);
            }, 2L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        AnvilInput.cancel(event.getPlayer());
    }
}
