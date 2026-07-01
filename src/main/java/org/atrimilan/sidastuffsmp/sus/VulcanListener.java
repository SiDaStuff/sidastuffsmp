package org.atrimilan.sidastuffsmp.sus;

import me.frep.vulcan.api.event.VulcanFlagEvent;
import me.frep.vulcan.api.check.Check;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class VulcanListener implements Listener {

    private volatile boolean registered = false;

    public VulcanListener() {}

    public void register() {
        if (registered) return;
        if (Bukkit.getPluginManager().getPlugin("Vulcan") == null) {
            SiDaStuffSmp.getInstance().getLogger().info("Vulcan integration: Vulcan not found, skipping.");
            return;
        }
        Bukkit.getPluginManager().registerEvents(new VulcanFlagEventListener(), SiDaStuffSmp.getInstance());
        registered = true;
        SiDaStuffSmp.getInstance().getLogger().info("Vulcan integration: listening for VulcanFlagEvent.");
    }

    static boolean isGeyserPlayer(UUID uuid) {
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object api = geyserApiClass.getMethod("api").invoke(null);
            Object connection = geyserApiClass.getMethod("connection", UUID.class).invoke(api, uuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static class VulcanFlagEventListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onVulcanFlag(VulcanFlagEvent event) {
            Player player = event.getPlayer();
            if (isGeyserPlayer(player.getUniqueId())) return;

            Check check = event.getCheck();
            String checkName = check.getCategory() + " " + check.getName() + " " + check.getType();
            String displayName = check.getDisplayName();
            String info = event.getInfo();

            String flagLabel = displayName != null && !displayName.isEmpty()
                    ? displayName
                    : checkName;
            if (info != null && !info.isEmpty()) {
                flagLabel += " (" + info + ")";
            }

            SusManager.addPlayer(player.getUniqueId(), player.getName(), "Vulcan:" + flagLabel, "Vulcan");
        }
    }
}
