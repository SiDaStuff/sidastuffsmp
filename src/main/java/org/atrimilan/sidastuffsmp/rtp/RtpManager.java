package org.atrimilan.sidastuffsmp.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RtpManager implements Listener {

    private static final Map<UUID, CountdownTask> ACTIVE_COUNTDOWNS = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private static final int MAX_ATTEMPTS = 12;
    private static SiDaStuffSmp plugin;

    private RtpManager() {}
    public static RtpManager createListener() { return new RtpManager(); }

    public static void init(SiDaStuffSmp pluginInstance) {
        plugin = pluginInstance;
    }
    
    public static void pregenerateLocations() {
    }

    public static Location getOrGenerateLocation(RtpConfig.RegionConfig region) {
        World world = plugin.getServer().getWorld(region.world());
        if (world == null) return null;
        return generateBetterRtpLocation(world, region);
    }

    static CompletableFuture<Location> findSafeLocation(RtpConfig.RegionConfig region) {
        World world = plugin.getServer().getWorld(region.world());
        if (world == null) return CompletableFuture.completedFuture(null);
        return findSafeLocation(region, world, 0);
    }

    private static CompletableFuture<Location> findSafeLocation(RtpConfig.RegionConfig region, World world, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return CompletableFuture.completedFuture(null);
        }

        Location candidate = generateBetterRtpLocation(world, region);
        return loadChunk(candidate).thenCompose(chunk -> validateCandidate(region, world, candidate)
                .thenCompose(location -> location != null
                        ? CompletableFuture.completedFuture(location)
                        : findSafeLocation(region, world, attempt + 1)));
    }

    private static Location generateBetterRtpLocation(World world, RtpConfig.RegionConfig region) {
        int centerX = (region.minX() + region.maxX()) / 2;
        int centerZ = (region.minZ() + region.maxZ()) / 2;
        int maxRadius = Math.max(
                Math.max(Math.abs(region.maxX() - centerX), Math.abs(region.minX() - centerX)),
                Math.max(Math.abs(region.maxZ() - centerZ), Math.abs(region.minZ() - centerZ))
        );
        int minRadius = Math.min(10, Math.max(0, maxRadius - 1));
        int range = Math.max(1, maxRadius - minRadius);
        int x;
        int z;
        int quadrant = RANDOM.nextInt(4);

        switch (quadrant) {
            case 0 -> {
                x = RANDOM.nextInt(range) + minRadius;
                z = RANDOM.nextInt(range) + minRadius;
            }
            case 1 -> {
                x = -RANDOM.nextInt(range) - minRadius;
                z = -RANDOM.nextInt(range) - minRadius;
            }
            case 2 -> {
                x = -RANDOM.nextInt(range) - minRadius;
                z = RANDOM.nextInt(range) + minRadius;
            }
            default -> {
                x = RANDOM.nextInt(range) + minRadius;
                z = -RANDOM.nextInt(range) - minRadius;
            }
        }

        return new Location(world, centerX + x, 69, centerZ + z);
    }

    private static CompletableFuture<Chunk> loadChunk(Location location) {
        return location.getWorld().getChunkAtAsync(location, true);
    }

    private static CompletableFuture<Location> validateCandidate(RtpConfig.RegionConfig region, World world, Location candidate) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> result.complete(findSafeSpot(region, world, candidate.getBlockX(), candidate.getBlockZ())));
        return result;
    }

    static Location findSafeSpot(RtpConfig.RegionConfig region, World world, int x, int z) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            return findNetherSafeSpot(region, world, x, z);
        }
        return findNormalSafeSpot(region, world, x, z);
    }

    private static Location findNormalSafeSpot(RtpConfig.RegionConfig region, World world, int x, int z) {
        Block block = world.getHighestBlockAt(x, z);
        if (block.getType().name().endsWith("AIR")) {
            block = world.getBlockAt(x, block.getY() - 1, z);
        } else if (!block.getType().isSolid() && !badBlock(block.getType(), world, x, z)) {
            block = world.getBlockAt(x, block.getY() - 1, z);
        }

        int minY = Math.max(world.getMinHeight(), world.getEnvironment() == World.Environment.THE_END ? 1 : region.minY());
        int maxY = Math.min(world.getMaxHeight() - 2, region.maxY());
        if (block.getY() < minY || block.getY() > maxY || badBlock(block.getType(), world, x, z)) {
            return null;
        }

        int blockY = block.getY();
        Material feetMat = world.getBlockAt(x, blockY + 1, z).getType();
        Material headMat = world.getBlockAt(x, blockY + 2, z).getType();
        if (!isOpenPlayerSpace(feetMat) || !isOpenPlayerSpace(headMat)) {
            return null;
        }

        return new Location(world, x + 0.5, blockY + 1, z + 0.5);
    }

    private static Location findNetherSafeSpot(RtpConfig.RegionConfig region, World world, int x, int z) {
        int minY = Math.max(world.getMinHeight() + 1, region.minY() + 1);
        int maxY = Math.min(world.getMaxHeight() - 2, region.maxY());
        for (int y = minY; y < maxY; y++) {
            Block current = world.getBlockAt(x, y, z);
            Material currentType = current.getType();
            if (currentType.name().endsWith("AIR") || !currentType.isSolid()) {
                if (!currentType.name().endsWith("AIR") && !currentType.isSolid() && badBlock(currentType, world, x, z)) {
                    continue;
                }

                Material below = world.getBlockAt(x, y - 1, z).getType();
                Material above = world.getBlockAt(x, y + 1, z).getType();
                if (!below.name().endsWith("AIR") && above.name().endsWith("AIR") && !badBlock(below, world, x, z)) {
                    return new Location(world, x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }

    private static boolean isOpenPlayerSpace(Material material) {
        String name = material.name();
        return !material.isSolid()
                && !name.contains("WATER")
                && !name.contains("LAVA")
                && material != Material.FIRE
                && material != Material.SOUL_FIRE;
    }

    private static boolean badBlock(Material material, World world, int x, int z) {
        if (RtpConfig.getBlacklistedBlocks().contains(material)) {
            return true;
        }

        String name = material.name();
        if (name.endsWith("AIR")
                || name.contains("WATER")
                || name.contains("LAVA")
                || name.contains("LEAVES")
                || name.contains("FIRE")
                || material == Material.CACTUS
                || material == Material.MAGMA_BLOCK
                || material == Material.POWDER_SNOW
                || material == Material.POINTED_DRIPSTONE
                || material == Material.BEDROCK) {
            return true;
        }

        if (RtpConfig.getBlacklistedBiomes().isEmpty()) {
            return false;
        }

        String biome = world.getBiome(x, z).getKey().getKey();
        for (String allowedBiome : RtpConfig.getBlacklistedBiomes()) {
            if (biome.equalsIgnoreCase(allowedBiome) || biome.toUpperCase(java.util.Locale.ROOT).contains(allowedBiome.toUpperCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveCountdown(UUID playerUuid) {
        return ACTIVE_COUNTDOWNS.containsKey(playerUuid);
    }

    public static boolean startCountdown(Player player, RtpConfig.RegionConfig region, boolean bypassCooldown) {
        UUID uuid = player.getUniqueId();

        if (ACTIVE_COUNTDOWNS.containsKey(uuid)) {
            player.sendMessage(Chat.prefixed("You already have an RTP in progress.", NamedTextColor.RED));
            return false;
        }

        if (!bypassCooldown) {
            long remaining = RtpCooldown.getRemainingCooldown(uuid);
            if (remaining > 0) {
                player.sendMessage(Chat.prefixed("You must wait " + remaining + " seconds before using RTP again.", NamedTextColor.RED));
                return false;
            }
        }

        if (plugin.getServer().getWorld(region.world()) == null) {
            player.sendMessage(Chat.prefixed("We couldn't find a safe location! Try again later.", NamedTextColor.RED));
            return false;
        }

        int waitTime = RtpConfig.getWaitTimeSeconds();
        CountdownTask task = new CountdownTask(player, waitTime, region, bypassCooldown);
        ACTIVE_COUNTDOWNS.put(uuid, task);
        task.start();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        return true;
    }

    public static void cancelCountdown(UUID playerUuid) {
        CountdownTask task = ACTIVE_COUNTDOWNS.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static void cleanup() {
        for (CountdownTask task : ACTIVE_COUNTDOWNS.values()) {
            task.cancel();
        }
        ACTIVE_COUNTDOWNS.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        CountdownTask task = ACTIVE_COUNTDOWNS.get(uuid);
        if (task == null || task.isComplete()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY()) {
            cancelCountdown(uuid);
            player.sendActionBar(Component.text("You moved! Teleportation cancelled", NamedTextColor.RED));
            player.sendMessage(Chat.prefixed("RTP cancelled - you moved!", NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelCountdown(event.getPlayer().getUniqueId());
    }

    static class CountdownTask implements Runnable {
        private final Player player;
        private final RtpConfig.RegionConfig region;
        private final boolean bypassCooldown;
        private final int totalSeconds;
        private int secondsLeft;
        private int taskId = -1;
        private boolean complete = false;
        private CompletableFuture<Location> preloadedLocation = null;
        private boolean chunkLoadingStarted = false;

        CountdownTask(Player player, int seconds, RtpConfig.RegionConfig region, boolean bypassCooldown) {
            this.player = player;
            this.totalSeconds = seconds;
            this.secondsLeft = seconds;
            this.region = region;
            this.bypassCooldown = bypassCooldown;
        }

        void start() {
            taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 20L);
        }

        void cancel() {
            if (taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            }
            if (preloadedLocation != null) {
                preloadedLocation.cancel(false);
                preloadedLocation = null;
            }
            complete = true;
        }

        boolean isComplete() {
            return complete;
        }

        private void startChunkPreloading() {
            if (chunkLoadingStarted) return;
            chunkLoadingStarted = true;
            preloadedLocation = findSafeLocation(region);
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancel();
                ACTIVE_COUNTDOWNS.remove(player.getUniqueId());
                return;
            }

            if (!chunkLoadingStarted && secondsLeft <= totalSeconds - 1) {
                startChunkPreloading();
            }

            if (secondsLeft <= 0) {
                complete = true;
                cancel();
                ACTIVE_COUNTDOWNS.remove(player.getUniqueId());

                player.sendActionBar(Component.text("Teleporting...", NamedTextColor.GRAY));
                player.sendMessage(Chat.prefixed("Teleporting to a random location...", NamedTextColor.YELLOW));

                CompletableFuture<Location> locationFuture = preloadedLocation != null ? preloadedLocation : findSafeLocation(region);

                locationFuture.thenAccept(target -> {
                    if (target == null) {
                        player.sendMessage(Chat.prefixed("Could not find a safe spot nearby. Try again.", NamedTextColor.RED));
                        return;
                    }

                    target.setYaw(player.getLocation().getYaw());
                    target.setPitch(player.getLocation().getPitch());
                    player.teleportAsync(target).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(Chat.prefixed("Teleported to a random location!", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                            if (!bypassCooldown) {
                                Bukkit.getScheduler().runTask(plugin, () -> RtpCooldown.setCooldown(player.getUniqueId()));
                            }
                            org.atrimilan.sidastuffsmp.stats.PlayerStatsManager.incrementRtpCount(player.getUniqueId());
                        } else {
                            player.sendMessage(Chat.prefixed("Teleport failed! Try again.", NamedTextColor.RED));
                        }
                    });
                }).exceptionally(ex -> {
                    player.sendMessage(Chat.prefixed("Teleport failed! Try again.", NamedTextColor.RED));
                    plugin.getLogger().warning("RTP teleport failed: " + ex.getMessage());
                    return null;
                });
                return;
            }

            player.sendActionBar(Component.text("Teleporting in " + secondsLeft, NamedTextColor.GRAY));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + (0.1f * (totalSeconds - secondsLeft)));
            secondsLeft--;
        }
    }
}
