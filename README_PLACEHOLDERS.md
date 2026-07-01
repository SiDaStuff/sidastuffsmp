# Placeholder Feature Reference

This file documents the placeholder features that exist in the codebase along with the matching
implemented behavior. Anything labelled **Open** is not yet implemented and is the source of the
TODO bullets in the parent `README.md`.

---

## 1. `/sell` GUI for EconomyShopGUI

### Implemented

- `/sell` opens a 54-slot GUI where the top two rows accept any items the player wants to sell.
- A `Calculate` button at slot 22 reads the contents of the input rows, looks up prices through
  EconomyShopGUI's `ShopHandler`/`ShopItem` reflection bridge, and falls back to a built-in default
  price map when EconomyShopGUI is missing.
- A `Confirm` button at slot 24 (or 11 in the confirmation GUI) removes the sold items from the
  inventory and pays the player through `EconomyManager.deposit(...)`.
- The confirmation GUI shows each item with its per-unit price and the calculated total.

### Open / Notes

- The sign-input flow that was planned for an enhanced sell GUI is not in place; quantities are
  always taken from the input rows. Adding sign quantity input per item is left as a future task.

---

## 2. Order Sell Priority Setting

### Implemented

- New boolean `orders.sell-priority` in `config.yml` (and the matching `orders.yml` default).
- Helper `OrderConfig.isSellPriorityEnabled()` exposes it to other code.
- `SellCommand` builds a per-material price map. When sell-priority is on, the price used for each
  material is the larger of the EconomyShopGUI sell price and the highest active buy-order price
  for that material (`OrderManager.getBestOrderPriceForMaterial`).
- The confirmation GUI lists every item with the resolved per-unit price.

### Open

- The actual physical routing of the items into the highest-order is left as a follow-up. For now
  the sell GUI credits the player using the higher per-unit price while continuing to remove the
  items from the inventory. Hooking this into `OrderManager.deliverItems(...)` per material is the
  intended future integration.

---

## 3. `/home` and `/homes` System

### Implemented

- `/home` (alias `/homes`) opens the main homes GUI with 8 colored bed slots and 8 red delete
  buttons. Clicking a set bed starts the 5-second teleport countdown; clicking an empty bed opens
  a sign input for the home's name and sets the home at the player's current location. Clicking
  the red button under a set home opens a confirmation GUI before deletion.
- `/home <name>` starts the teleport countdown directly without opening the GUI.
- `/sethome <name>` sets a home at the player's current location in the first free slot.
- `/delhome <name>` (alias `/deletehome`) deletes a home by name.
- Blocked worlds list (`homes.blocked-worlds` in `homes.yml`) prevents homes from being set in
  those worlds; both `/sethome` and the GUI sign-input flow honor the list.
- The teleport countdown supports both movement cancellation (configurable) and damage
  cancellation (also configurable).

### Open

- Cross-world-permission checks (per-world cooldowns, etc.) are not implemented.
- Cross-server synchronization of homes via Firebase is not implemented; only the local SQLite
  store is used.

---

## 4. Admin Commands (`/homeadmin`)

### Implemented

- `/homeadmin view <player>` opens an admin GUI listing that player's homes with colored beds.
  Clicking a bed teleports the admin to that home.
- `/homeadmin tp <player> <home>` teleports the admin directly without opening the GUI.
- `/homeadmin delete <player> <home>` removes one of the player's homes.
- `/homeadmin backupimport` reads `backup_homes.csv` from the plugin's data folder (and falls
  back to the server root) and imports every row via `INSERT OR REPLACE` so the import is
  idempotent.
- `/homeadmin reload` reloads `homes.yml` and the teleport delay cache.

### CSV format

```text
owner_uuid,slot,world,x,y,z,yaw,pitch,created_at,updated_at[,name]
```

A trailing `name` column is optional and defaults to `home`. The importer counts imported rows
and skipped (malformed) rows and reports both in chat.

### Open

- CSV export from in-memory data is not implemented. If you need to dump homes you can stop the
  server and run a one-off SQLite dump of `homes.db`.

---

## 5. Firebase JVM Warning

### Issue

The console prints `WARN addURL injection failed: Unable to make protected ... java.net ...` lines
on startup because the JVM module system is closed by default.

### Fix (recommended)

Add the following JVM arguments to your Paper startup script or your hosting panel:

```text
--add-opens java.base/java.net=ALL-UNNAMED
--add-opens java.base/jdk.internal.loader=ALL-UNNAMED
```

These flags are also surfaced by the plugin's own logger when the Firebase isolated classloader
fallback is engaged. With them present Firebase will load natively instead of via reflection.

### Code change (optional)

The plugin already falls back to a reflection-based isolated classloader, so the warning is
harmless. You can leave it as-is, but adding the flags above silences the warnings entirely.

---

## 6. Orders Menu Variant Toggle Fix

### Issue

Clicking the variant item at slot 19 in the create-order GUI did nothing for potions with no
strong/long variants.

### Fix

- `OrderGuiListener.handleCreateOrderClick` now always updates the variant when a potion item is
  selected. If the effect has no `strong_` or `long_` variant the click still cycles to the
  next available variant (or back to normal), and an action-bar message confirms the new variant.
- `CreateOrderGui.open` now always renders the variant slot for any potion-effect item, with
  helpful lore describing the available variants (Strong II / Long +).