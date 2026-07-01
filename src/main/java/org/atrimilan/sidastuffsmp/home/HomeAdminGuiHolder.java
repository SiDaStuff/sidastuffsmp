package org.atrimilan.sidastuffsmp.home;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HomeAdminGuiHolder implements InventoryHolder {

    private final UUID ownerUuid;
    private final String ownerName;
    private int page;
    private int pendingDeleteSlot = -1;

    public HomeAdminGuiHolder(UUID ownerUuid, String ownerName, int page) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.page = page;
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPendingDeleteSlot() { return pendingDeleteSlot; }
    public void setPendingDeleteSlot(int slot) { this.pendingDeleteSlot = slot; }
    public void setPendingDelete(int slot) { this.pendingDeleteSlot = slot; }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}