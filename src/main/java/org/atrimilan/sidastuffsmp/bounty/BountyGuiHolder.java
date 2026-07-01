package org.atrimilan.sidastuffsmp.bounty;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class BountyGuiHolder implements InventoryHolder {
    private UUID viewerUuid;

    public BountyGuiHolder() {
        this.viewerUuid = null;
    }

    public BountyGuiHolder(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
