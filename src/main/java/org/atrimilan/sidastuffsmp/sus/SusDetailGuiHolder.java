package org.atrimilan.sidastuffsmp.sus;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SusDetailGuiHolder implements InventoryHolder {

    private final String playerName;
    private final int page;

    public SusDetailGuiHolder(String playerName, int page) {
        this.playerName = playerName;
        this.page = page;
    }

    public String getPlayerName() { return playerName; }
    public int getPage() { return page; }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
