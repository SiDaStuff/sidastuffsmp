package org.atrimilan.sidastuffsmp.sus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SusGuiHolder implements InventoryHolder {

    private final int page;

    public SusGuiHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
