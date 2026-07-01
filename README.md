# Project Placeholder Features

This README outlines the planned features for the project, serving as a placeholder until full implementation.

## 1. `/sell` GUI

### Description
A new GUI accessible via `/sell` to facilitate selling items using `ecomoneyshopgui`.

### Features
*   **Item Selection:** Players can place items into specific slots within the GUI to sell them.
*   **Price Display:** Clearly displays the price per item and the total price for the stack.
*   **Confirmation:** A confirmation step (via GUI ask) to ensure the player intends to sell the item for the set price.
*   **Order Sell Priority:**
    *   **New Setting:** A configurable setting to enable/disable order sell priority.
    *   **Prioritization Logic:** If enabled, the system will check for available player orders (presumably from another system like a trading or order book plugin) that offer a higher price per item than the default `ecomoneyshopgui` shop price.
    *   **Automatic Selling:** If a higher-paying order is found, the items will be sold to that order instead of the shop.

## 2. `/home` and `/homes` System

### Description
A comprehensive player home system with GUI management, teleportation, and administrative tools.

### Features

#### User Commands & GUI
*   **/home or /homes:** Opens a main GUI for managing homes.
    *   **Home Slots:** Displays 8 home slots, represented by beds.
    *   **Set Homes:**
        *   Clicking an unset home (gray bed) opens a sign input for the home's name.
        *   Sets the home at the player's current location with the given name.
    *   **Teleport to Home:**
        *   Clicking a set home (colored bed) initiates a 5-second teleport countdown.
        *   Movement during the countdown cancels the teleport.
    *   **Delete Home:**
        *   Each set home will have a corresponding delete button (e.g., red wool or barrier icon).
        *   Clicking the delete button opens a confirmation GUI.
*   **/home <name>:** Initiates a 5-second teleport countdown to the specified home. Movement cancels the teleport.
*   **/sethome <name>:** Sets a new home at the player's current location with the given name. Opens a confirmation GUI.
*   **/delhome <name> or /deletehome <name>:** Deletes the specified home. Opens a confirmation GUI.

#### Configuration & Admin
*   **Blocked Worlds:** A configuration option to specify worlds where players cannot set homes.
*   **Admin Commands (`/homeadmin`)**:
    *   **View Other Users' Homes:** `/homeadmin view <player>` to inspect another player's homes.
    *   **Teleport to Home:** `/homeadmin tp <player> <home_name>` to teleport to a specific home of another player.
    *   **Backup Import (`/homeadmin backupimport`)**:
        *   Reads `backup_homes.csv` from the main plugin directory.
        *   Imports homes from the CSV into the system.
        *   **CSV Format:**
            ```csv
            owner_uuid,slot,world,x,y,z,yaw,pitch,created_at,updated_at
            6d495c37-d90b-4eb4-b2d2-449747c26bc2,1,world,-323.4588932,-19,638.9995237,152.8504944,25.49996758,2147483647,2147483647
            # ... more entries
            ```
            *   `owner_uuid`: UUID of the player who owns the home.
            *   `slot`: The home slot (1-8).
            *   `world`: The world name where the home is set.
            *   `x, y, z`: Coordinates of the home.
            *   `yaw, pitch`: Player orientation at the home.
            *   `created_at, updated_at`: Timestamps (e.g., Unix epoch seconds).

## 3. Firebase Warnings

### Issue
The console displays `WARN` messages regarding Firebase library injection failing due to Java module restrictions (`java.base/java.net` and `java.base/jdk.internal.loader` not being opened to unnamed modules). This results in a fallback to an isolated classloader, potentially impacting performance or native integration.

### Proposed Solution
Add the following JVM flags to your Java startup script (e.g., `server-startup.sh`, `start.bat`, or `jvm-args` in your server hosting panel):

```
--add-opens java.base/java.net=ALL-UNNAMED
--add-opens java.base/jdk.internal.loader=ALL-UNNAMED
```

This will grant the necessary module access for Firebase to load natively.

## 4. Orders Menu Variant Toggle Fix

### Issue
The variant toggle in the orders menu currently does nothing when clicked.

### Proposed Fix
This requires investigation into the existing codebase related to the orders menu and its UI handling. The fix will likely involve:
*   Identifying the event listener or action associated with the variant toggle.
*   Ensuring the event handler correctly updates the displayed order variants or filters the view.
*   Verifying that the underlying data model supports variant toggling and is being updated accordingly.

