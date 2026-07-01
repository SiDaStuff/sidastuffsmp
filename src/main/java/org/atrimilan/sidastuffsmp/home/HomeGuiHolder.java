package org.atrimilan.sidastuffsmp.home;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HomeGuiHolder implements InventoryHolder {
    private int pendingDeleteSlot = -1;
    private int homeIconSelectionSlot = -1;
    private int page = 0;
    private UUID viewerUuid;
    private String searchTerm = null;

    public int getPendingDeleteSlot() {
        return pendingDeleteSlot;
    }

    public void setPendingDeleteSlot(int slot) {
        this.pendingDeleteSlot = slot;
    }

    public void setPendingDelete(int slot) {
        this.pendingDeleteSlot = slot;
    }

    public int getHomeIconSelectionSlot() {
        return homeIconSelectionSlot;
    }

    public void setHomeIconSelectionSlot(int slot) {
        this.homeIconSelectionSlot = slot;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
