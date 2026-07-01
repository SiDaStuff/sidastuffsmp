package org.atrimilan.sidastuffsmp.bounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BountyListener implements Listener {

    public BountyListener() {}

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        // Anti-exploit: check for self-bounty abuse
        double playerBountyOnSelf = BountyManager.getBountyBySetterOnTarget(killer.getUniqueId(), victim.getUniqueId());
        if (playerBountyOnSelf > 0) {
            // Player had a bounty on someone they killed - could be self-bounty farming
            // Log it but don't block (they might have been targeted by others)
            Bukkit.getScheduler().runTask(org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance(), () -> {
                org.atrimilan.sidastuffsmp.SiDaStuffSmp.getInstance().getLogger().warning(
                    "Kill detected where killer had placed bounty on victim: " + killer.getName() + " -> " + victim.getName());
            });
        }

        double totalBounty = BountyManager.getTotalBounty(victim.getUniqueId());
        if (totalBounty <= 0) return;

        // Check for suspicious farming pattern
        boolean suspicious = BountyManager.isKillPatternSuspicious(killer, victim);
        if (suspicious) {
            // Notify admins
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("sidastuffsmp.admin")) {
                    admin.sendMessage(Chat.prefixed("Bounty claim flagged for suspicious activity: " + killer.getName() + " killed " + victim.getName(), NamedTextColor.YELLOW));
                }
            }
        }

        // Claim the bounty
        double claimed = BountyManager.claimBounty(killer, victim);

        if (claimed > 0) {
            killer.sendMessage(Chat.prefixed("You claimed a " + BountyManager.formatPrice(claimed) + " bounty on " + victim.getName() + "!", NamedTextColor.GOLD));
            victim.sendMessage(Chat.prefixed("A bounty of " + BountyManager.formatPrice(claimed) + " was claimed on your death!", NamedTextColor.RED));
        }
    }
}