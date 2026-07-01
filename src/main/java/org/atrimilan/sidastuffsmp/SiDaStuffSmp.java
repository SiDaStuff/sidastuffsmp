package org.atrimilan.sidastuffsmp;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.atrimilan.sidastuffsmp.auction.AuctionConfig;
import org.atrimilan.sidastuffsmp.auction.AuctionManager;
import org.atrimilan.sidastuffsmp.bounty.BountyConfig;
import org.atrimilan.sidastuffsmp.bounty.BountyManager;
import org.atrimilan.sidastuffsmp.bounty.BountyListener;
import org.atrimilan.sidastuffsmp.commands.AuctionAdminCommand;
import org.atrimilan.sidastuffsmp.commands.AuctionCommand;
import org.atrimilan.sidastuffsmp.commands.BalanceCommand;
import org.atrimilan.sidastuffsmp.commands.BountyCommand;
import org.atrimilan.sidastuffsmp.commands.EconomyAdminCommand;
import org.atrimilan.sidastuffsmp.commands.GameModeCommands;
import org.atrimilan.sidastuffsmp.commands.LinksCommand;
import org.atrimilan.sidastuffsmp.commands.OrderAdminCommand;
import org.atrimilan.sidastuffsmp.commands.OrderCommand;
import org.atrimilan.sidastuffsmp.commands.PayCommand;
import org.atrimilan.sidastuffsmp.commands.PunishmentCommands;
import org.atrimilan.sidastuffsmp.commands.SetMessageCommand;
import org.atrimilan.sidastuffsmp.commands.TopBalanceCommand;
import org.atrimilan.sidastuffsmp.commands.StatsAdminCommand;
import org.atrimilan.sidastuffsmp.commands.AdminCommands;
import org.atrimilan.sidastuffsmp.commands.SpawnCommand;
import org.atrimilan.sidastuffsmp.commands.TpSilentCommand;
import org.atrimilan.sidastuffsmp.commands.TransactionHistoryCommand;
import org.atrimilan.sidastuffsmp.commands.WarnCommand;
import org.atrimilan.sidastuffsmp.economy.EconomyManager;
import org.atrimilan.sidastuffsmp.economy.EconomyShopGuiPriceLoader;
import org.atrimilan.sidastuffsmp.gui.AuctionGuiListener;
import org.atrimilan.sidastuffsmp.gui.BountyGuiListener;
import org.atrimilan.sidastuffsmp.gui.OrderGuiListener;
import org.atrimilan.sidastuffsmp.home.HomeAdminCommand;
import org.atrimilan.sidastuffsmp.home.HomeCommand;
import org.atrimilan.sidastuffsmp.home.HomeAdminGuiListener;
import org.atrimilan.sidastuffsmp.home.HomeConfig;
import org.atrimilan.sidastuffsmp.home.HomeFirebaseSync;
import org.atrimilan.sidastuffsmp.home.HomeGuiListener;
import org.atrimilan.sidastuffsmp.home.HomeManager;
import org.atrimilan.sidastuffsmp.home.HomeTeleportManager;
import org.atrimilan.sidastuffsmp.utils.AnvilInput;
import org.atrimilan.sidastuffsmp.utils.SignInput;
import org.atrimilan.sidastuffsmp.listeners.AntiDupingListener;
import org.atrimilan.sidastuffsmp.listeners.JoinMessageListener;
import org.atrimilan.sidastuffsmp.listeners.PunishmentRestoreListener;
import org.atrimilan.sidastuffsmp.listeners.WarningJoinListener;
import org.atrimilan.sidastuffsmp.order.MinecraftDataRegistry;
import org.atrimilan.sidastuffsmp.order.OrderConfig;
import org.atrimilan.sidastuffsmp.order.OrderManager;
import org.atrimilan.sidastuffsmp.placeholder.EconomyPlaceholderExpansion;
import org.atrimilan.sidastuffsmp.placeholder.SiDaStuffSmpExpansion;
import org.atrimilan.sidastuffsmp.rtp.RtpCommand;
import org.atrimilan.sidastuffsmp.rtp.RtpConfig;
import org.atrimilan.sidastuffsmp.rtp.RtpCooldown;
import org.atrimilan.sidastuffsmp.rtp.RtpGuiListener;
import org.atrimilan.sidastuffsmp.rtp.RtpManager;
import org.atrimilan.sidastuffsmp.sell.SellCommand;
import org.atrimilan.sidastuffsmp.sell.SellGuiListener;
import org.atrimilan.sidastuffsmp.stats.PlayerStatsManager;
import org.atrimilan.sidastuffsmp.stats.StatsCommand;
import org.atrimilan.sidastuffsmp.stats.StatsGuiListener;
import org.atrimilan.sidastuffsmp.stats.StatsListener;
import org.atrimilan.sidastuffsmp.sus.SusCommand;
import org.atrimilan.sidastuffsmp.sus.SusConfig;
import org.atrimilan.sidastuffsmp.sus.SusGuiListener;
import org.atrimilan.sidastuffsmp.sus.SusManager;
import org.atrimilan.sidastuffsmp.sus.SusPluginCommandListener;
import org.atrimilan.sidastuffsmp.sus.VulcanListener;
import org.atrimilan.sidastuffsmp.sync.AuctionFirebaseSync;
import org.atrimilan.sidastuffsmp.sync.BountyFirebaseSync;
import org.atrimilan.sidastuffsmp.sync.OrderFirebaseSync;
import org.atrimilan.sidastuffsmp.sync.StatsFirebaseSync;
import org.atrimilan.sidastuffsmp.teleport.NightVisionListener;
import org.atrimilan.sidastuffsmp.teleport.PlayerSettings;
import org.atrimilan.sidastuffsmp.teleport.TeleportCommand;
import org.atrimilan.sidastuffsmp.teleport.TeleportGuiListener;
import org.atrimilan.sidastuffsmp.teleport.TeleportManager;
import org.atrimilan.sidastuffsmp.utils.ConfigManager;
import org.atrimilan.sidastuffsmp.utils.PunishmentManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SiDaStuffSmp extends JavaPlugin {

	private static SiDaStuffSmp instance;
	private Location spawnLocation;
	private int expiryTaskId = -1;
	private int syncTaskId = -1;
	private int orderExpiryTaskId = -1;
	private int orderSyncTaskId = -1;
	private int bountySyncTaskId = -1;
	private int rtpCooldownCleanupTaskId = -1;

	@Override
	public void onEnable() {
		instance = this;

		getConfig().options().copyDefaults(true);
		saveConfig();
		loadSpawn();

		ConfigManager.init(this);
		PunishmentManager.init(this);
		PunishmentManager.flushSave();

		SusConfig.init(this);

		EconomyManager.init(this);

		AuctionConfig.init(this);
		AuctionManager.init(this);
		AuctionFirebaseSync.init(this);

        OrderConfig.init(this);
        MinecraftDataRegistry.init(this);
        OrderManager.init(this);
        OrderFirebaseSync.init(this);

        PlayerSettings.init(this);

        RtpConfig.init(this);
        RtpCooldown.init(this);
        RtpManager.init(this);
        PlayerStatsManager.init(this);
        StatsFirebaseSync.init(this);

        HomeConfig.init(this);
        HomeManager.init(this);
        HomeFirebaseSync.init(this);
        SignInput.init();
        AnvilInput.init();

        if (SusConfig.isEnabled()) {
            SusManager.init(this);
        }

	        BountyConfig.init(this);
	        BountyManager.init(this);
	        BountyFirebaseSync.init(this);

        EconomyShopGuiPriceLoader.loadPrices(this);

        Bukkit.getScheduler().runTaskTimer(this, () -> PlayerSettings.saveDirty(), 6000L, 6000L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, StatsListener::updateSessions, 1200L, 1200L);

        if (StatsFirebaseSync.isEnabled()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, StatsFirebaseSync::performSync, 6000L, 6000L);
        }

        rtpCooldownCleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            org.atrimilan.sidastuffsmp.rtp.RtpCooldown.cleanupExpired();
        }, 6000L, 6000L).getTaskId();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            AuctionManager.tickExpiry();
            AuctionManager.cleanupOnStartup();
            OrderManager.tickExpiry();
            OrderManager.cleanupOnStartup();
        });

this.registerPluginCommands();
        this.registerPluginEvents();
        this.registerPlaceholderExpansions();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            org.atrimilan.sidastuffsmp.placeholder.SiDaStuffSmpExpansion.startCacheRefresh();
        }

		if (AuctionConfig.enabled()) {
			this.scheduleAuctionTasks();
		}

		if (OrderConfig.enabled()) {
			this.scheduleOrderTasks();
		}

		getLogger().info("SiDaStuff SMP has been enabled!");
	}

	@Override
	public void onDisable() {
		if (expiryTaskId != -1) {
			Bukkit.getScheduler().cancelTask(expiryTaskId);
		}
		if (syncTaskId != -1) {
			Bukkit.getScheduler().cancelTask(syncTaskId);
		}
		if (orderExpiryTaskId != -1) {
			Bukkit.getScheduler().cancelTask(orderExpiryTaskId);
		}
			if (orderSyncTaskId != -1) {
				Bukkit.getScheduler().cancelTask(orderSyncTaskId);
			}
			if (bountySyncTaskId != -1) {
				Bukkit.getScheduler().cancelTask(bountySyncTaskId);
			}
			if (rtpCooldownCleanupTaskId != -1) {
				Bukkit.getScheduler().cancelTask(rtpCooldownCleanupTaskId);
			}

	        OrderFirebaseSync.shutdown();
	        BountyFirebaseSync.shutdown();
        OrderManager.shutdown();
        AuctionFirebaseSync.shutdown();
        AuctionManager.shutdown();
        EconomyManager.shutdown();
        HomeManager.shutdown();

        if (SusConfig.isEnabled()) {
            SusManager.shutdown();
        }

        TeleportManager.cleanup();
        PlayerSettings.saveAll();
        RtpCooldown.saveAll();
        RtpManager.cleanup();
        StatsListener.saveAllPlaytime();
        StatsFirebaseSync.shutdown();
        PlayerStatsManager.shutdown();

        AdminCommands.cleanupAll();

		getLogger().info("SiDaStuff SMP has been disabled!");
	}

    private void registerPluginCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(
                    LinksCommand.createMediaCommand(),
                    LinksCommand.MEDIA_DESCRIPTION,
                    LinksCommand.MEDIA_ALIASES);
            commands.registrar().register(
                    LinksCommand.createApplyCommand(),
                    LinksCommand.APPLY_DESCRIPTION,
                    LinksCommand.APPLY_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createPunishCommand(),
                    PunishmentCommands.PUNISH_DESCRIPTION,
                    PunishmentCommands.PUNISH_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createKickCommand(),
                    PunishmentCommands.KICK_DESCRIPTION,
                    PunishmentCommands.KICK_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createBanCommand(),
                    PunishmentCommands.BAN_DESCRIPTION,
                    PunishmentCommands.BAN_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createTempBanCommand(),
                    PunishmentCommands.TEMPBAN_DESCRIPTION,
                    PunishmentCommands.TEMPBAN_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createPunishHistoryCommand(),
                    PunishmentCommands.HISTORY_DESCRIPTION,
                    PunishmentCommands.HISTORY_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createNewSeasonUnbanAllCommand(),
                    PunishmentCommands.NEW_SEASON_DESCRIPTION,
                    PunishmentCommands.NEW_SEASON_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createUnbanCommand(),
                    PunishmentCommands.UNBAN_DESCRIPTION,
                    PunishmentCommands.UNBAN_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createRestoreCommand(),
                    PunishmentCommands.RESTORE_DESCRIPTION,
                    PunishmentCommands.RESTORE_ALIASES);
            commands.registrar().register(
                    PunishmentCommands.createPunishTypeCommand(),
                    PunishmentCommands.PUNISHTYPE_DESCRIPTION,
                    PunishmentCommands.PUNISHTYPE_ALIASES);
            commands.registrar().register(
                    SetMessageCommand.createCommand(),
                    SetMessageCommand.DESCRIPTION,
                    SetMessageCommand.ALIASES);
            commands.registrar().register(
                    WarnCommand.createCommand(),
                    WarnCommand.DESCRIPTION,
                    WarnCommand.ALIASES);
            commands.registrar().register(
                    AuctionCommand.createCommand(),
                    AuctionCommand.DESCRIPTION,
                    AuctionCommand.ALIASES);
            commands.registrar().register(
                    AuctionAdminCommand.createCommand(),
                    AuctionAdminCommand.DESCRIPTION,
                    AuctionAdminCommand.ALIASES);
            commands.registrar().register(
                    BalanceCommand.createCommand(),
                    BalanceCommand.DESCRIPTION,
                    BalanceCommand.ALIASES);
            commands.registrar().register(
                    PayCommand.createCommand(),
                    PayCommand.DESCRIPTION,
                    PayCommand.ALIASES);
            commands.registrar().register(
                    TopBalanceCommand.createCommand(),
                    TopBalanceCommand.DESCRIPTION,
                    TopBalanceCommand.ALIASES);
            commands.registrar().register(
                    TransactionHistoryCommand.createCommand(),
                    TransactionHistoryCommand.DESCRIPTION,
                    TransactionHistoryCommand.ALIASES);
            commands.registrar().register(
                    StatsAdminCommand.createCommand(),
                    StatsAdminCommand.DESCRIPTION,
                    StatsAdminCommand.ALIASES);
		commands.registrar().register(
			EconomyAdminCommand.createCommand(),
			EconomyAdminCommand.DESCRIPTION,
			EconomyAdminCommand.ALIASES);
		commands.registrar().register(
			EconomyAdminCommand.createReloadCommand(),
			"Reload economy config",
			EconomyAdminCommand.ALIASES);
		commands.registrar().register(
			OrderCommand.createCommand(),
			OrderCommand.DESCRIPTION,
			OrderCommand.ALIASES);
        commands.registrar().register(
                OrderAdminCommand.createCommand(),
                OrderAdminCommand.DESCRIPTION,
                OrderAdminCommand.ALIASES);
        commands.registrar().register(
                TeleportCommand.createTpaCommand(),
                TeleportCommand.TPA_DESCRIPTION,
                TeleportCommand.TPA_ALIASES);
        commands.registrar().register(
                TeleportCommand.createTpaHereCommand(),
                TeleportCommand.TPAHERE_DESCRIPTION,
                TeleportCommand.TPAHERE_ALIASES);
        commands.registrar().register(
                TeleportCommand.createTpaAcceptCommand(),
                TeleportCommand.TPAACCEPT_DESCRIPTION,
                TeleportCommand.TPAACCEPT_ALIASES);
        commands.registrar().register(
                TeleportCommand.createTpaDenyCommand(),
                TeleportCommand.TPADENY_DESCRIPTION,
                TeleportCommand.TPADENY_ALIASES);
        commands.registrar().register(
                TeleportCommand.createTpaAutoCommand(),
                TeleportCommand.TPAAUTO_DESCRIPTION,
                TeleportCommand.TPAAUTO_ALIASES);
        commands.registrar().register(
                TeleportCommand.createSettingsCommand(),
                TeleportCommand.SETTINGS_DESCRIPTION,
                TeleportCommand.SETTINGS_ALIASES);
        commands.registrar().register(
                RtpCommand.createRtpCommand(),
                RtpCommand.RTP_DESCRIPTION,
                RtpCommand.RTP_ALIASES);
        commands.registrar().register(
                RtpCommand.createRtpSudoCommand(),
                RtpCommand.RTP_SUDO_DESCRIPTION,
                RtpCommand.RTP_SUDO_ALIASES);
        commands.registrar().register(
                StatsCommand.createCommand(),
                StatsCommand.DESCRIPTION,
                StatsCommand.ALIASES);
        commands.registrar().register(
                SpawnCommand.createSpawnCommand(),
                SpawnCommand.SPAWN_DESCRIPTION,
                SpawnCommand.SPAWN_ALIASES);
        commands.registrar().register(
                SpawnCommand.createSetSpawnCommand(),
                SpawnCommand.SETSPAWN_DESCRIPTION,
                SpawnCommand.SETSPAWN_ALIASES);
        commands.registrar().register(
                HomeCommand.createCommand(),
                HomeCommand.DESCRIPTION,
                HomeCommand.ALIASES);
        commands.registrar().register(
                HomeCommand.createSetHomeCommand(),
                "Set a home at your location",
                Set.of("sethome"));
        commands.registrar().register(
                HomeCommand.createDelHomeCommand(),
                "Delete a home",
                Set.of("deletehome"));
        commands.registrar().register(
                HomeAdminCommand.createCommand(),
                HomeAdminCommand.DESCRIPTION,
                HomeAdminCommand.ALIASES);
        commands.registrar().register(
                AdminCommands.createSidaReloadCommand(),
                "Reload all plugin configs",
                Set.of());
        commands.registrar().register(
                AdminCommands.createVanishCommand(),
                "Toggle vanish mode",
                Set.of());
        commands.registrar().register(
                AdminCommands.createInspectCommand(),
                "View player inventory",
                Set.of());
        commands.registrar().register(
                AdminCommands.createEnderChestCommand(),
                "View player ender chest",
                Set.of());
        commands.registrar().register(
                AdminCommands.createFreezeCommand(),
                "Toggle freeze mode for a player",
                Set.of());
        commands.registrar().register(
                AdminCommands.createRunCommand(),
                "Cheat detection commands",
                Set.of("testcheat"));
        commands.registrar().register(
                SellCommand.createCommand(),
                SellCommand.DESCRIPTION,
                SellCommand.ALIASES);
        commands.registrar().register(
                BountyCommand.createCommand(),
                BountyCommand.DESCRIPTION,
                BountyCommand.ALIASES);
        commands.registrar().register(
                GameModeCommands.createGmsCommand(),
                GameModeCommands.DESCRIPTION,
                GameModeCommands.ALIASES);
        commands.registrar().register(
                GameModeCommands.createGmcCommand(),
                GameModeCommands.DESCRIPTION,
                GameModeCommands.ALIASES);
        commands.registrar().register(
                GameModeCommands.createGmspCommand(),
                GameModeCommands.DESCRIPTION,
                GameModeCommands.ALIASES);
        commands.registrar().register(
                GameModeCommands.createGmaCommand(),
                GameModeCommands.DESCRIPTION,
                GameModeCommands.ALIASES);
        if (SusConfig.isEnabled()) {
            commands.registrar().register(
                    SusCommand.createCommand(),
                    SusCommand.DESCRIPTION,
                    SusCommand.ALIASES);
            commands.registrar().register(
                    SusCommand.createFlaggerAddCommand(),
                    SusCommand.FLAGGERADD_DESCRIPTION,
                    SusCommand.FLAGGERADD_ALIASES);
        }
        commands.registrar().register(
                TpSilentCommand.createCommand(),
                TpSilentCommand.DESCRIPTION,
                TpSilentCommand.ALIASES);
    });
    }

    private void registerPluginEvents() {
        Bukkit.getPluginManager().registerEvents(new PunishmentRestoreListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinMessageListener(), this);
        Bukkit.getPluginManager().registerEvents(new WarningJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new AntiDupingListener(this), this);
		Bukkit.getPluginManager().registerEvents(new AuctionGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new OrderGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new BountyGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new BountyListener(), this);
        Bukkit.getPluginManager().registerEvents(new EconomyJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new TeleportGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new NightVisionListener(), this);
        Bukkit.getPluginManager().registerEvents(RtpManager.createListener(), this);
        Bukkit.getPluginManager().registerEvents(new RtpGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(), this);
        Bukkit.getPluginManager().registerEvents(new StatsGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new SellGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new HomeGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new HomeAdminGuiListener(), this);
        Bukkit.getPluginManager().registerEvents(new HomeTeleportManager(), this);

        if (SusConfig.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(new SusGuiListener(), this);
            Bukkit.getPluginManager().registerEvents(new SusPluginCommandListener(), this);
            if (SusConfig.isVulcanIntegration() && Bukkit.getPluginManager().getPlugin("Vulcan") != null) {
                new VulcanListener().register();
            }
        }

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onPlayerMove(PlayerMoveEvent event) {
                Player player = event.getPlayer();
                if (SpawnCommand.hasActiveTeleport(player.getUniqueId())) {
                    Location from = event.getFrom();
                    Location to = event.getTo();
                    if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY())) {
                        SpawnCommand.cancelTeleport(player.getUniqueId());
                        player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleport cancelled - you moved!", NamedTextColor.RED));
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendActionBar(net.kyori.adventure.text.Component.text("Teleport cancelled", NamedTextColor.RED));
                    }
                }
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
                Player player = event.getPlayer();
                if (org.atrimilan.sidastuffsmp.commands.AdminCommands.isFrozen(player.getUniqueId())) {
                    Location from = event.getFrom();
                    Location to = event.getTo();
                    if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ() || from.getBlockY() != to.getBlockY())) {
                        Location frozenLocation = from.clone();
                        frozenLocation.setDirection(to.getDirection());
                        player.teleport(frozenLocation);
                        player.sendActionBar(net.kyori.adventure.text.Component.text("You are frozen!", NamedTextColor.RED));
                    }
                }
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
                if (event.getEntity() instanceof Player player) {
                    if (SpawnCommand.hasActiveTeleport(player.getUniqueId())) {
                        SpawnCommand.cancelTeleport(player.getUniqueId());
                        player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed("Teleport cancelled - you took damage!", NamedTextColor.RED));
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendActionBar(net.kyori.adventure.text.Component.text("Teleport cancelled", NamedTextColor.RED));
                    }
                }
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                Player player = event.getPlayer();
                if (org.atrimilan.sidastuffsmp.commands.AdminCommands.isVanished(player.getUniqueId())) {
                    event.joinMessage(null);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.hasPermission("sidastuffsmp.admin")) {
                            online.hidePlayer(SiDaStuffSmp.this, player);
                        }
                    }
                }
                Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.this, () -> {
                    for (Player vanished : Bukkit.getOnlinePlayers()) {
                        if (org.atrimilan.sidastuffsmp.commands.AdminCommands.isVanished(vanished.getUniqueId())
                                && !player.hasPermission("sidastuffsmp.admin")) {
                            player.hidePlayer(SiDaStuffSmp.this, vanished);
                        }
                    }
                }, 1L);
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                Player player = event.getPlayer();
                if (org.atrimilan.sidastuffsmp.commands.AdminCommands.isVanished(player.getUniqueId())) {
                    event.quitMessage(null);
                }
                AdminCommands.removeFromAllSets(player.getUniqueId());
                EconomyManager.invalidateCache(player.getUniqueId());
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
                if (!(event.getPlayer() instanceof Player player)) return;
                UUID uuid = player.getUniqueId();
                if (AdminCommands.isInspecting(uuid)) {
                    AdminCommands.stopInspecting(uuid);
                }
                if (AdminCommands.isInspectingEc(uuid)) {
                    AdminCommands.stopInspectingEc(uuid);
                }
            }
        }, this);

        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                Player player = event.getPlayer();
                org.bukkit.Bukkit.getScheduler().runTaskLater(SiDaStuffSmp.this, () -> {
                    org.atrimilan.sidastuffsmp.teleport.PlayerSettings joinSettings = org.atrimilan.sidastuffsmp.teleport.PlayerSettings.get(player.getUniqueId());
                    int sold = AuctionManager.getUncollectedSoldCount(player.getUniqueId());
                    int expired = AuctionManager.getUncollectedExpiredCount(player.getUniqueId());
                if (sold > 0 && joinSettings.isAuctionMessagesEnabled()) {
                    player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed(
                            "You have " + sold + " sold auction(s) with money to collect! Use /ah my",
                            net.kyori.adventure.text.format.NamedTextColor.GREEN));
                }
                if (expired > 0 && joinSettings.isAuctionMessagesEnabled()) {
                    player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed(
                            "You have " + expired + " expired auction(s) with items to collect! Use /ah my",
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                }
                int stashItems = OrderManager.getUncollectedStashItemCount(player.getUniqueId());
                if (stashItems > 0 && joinSettings.isOrderMessagesEnabled()) {
                    player.sendMessage(org.atrimilan.sidastuffsmp.utils.Chat.prefixed(
                            "You have " + stashItems + " item(s) in your order stash! Use /orders my and click Order Stash",
                            net.kyori.adventure.text.format.NamedTextColor.GREEN));
                }
                }, 40L);
            }
        }, this);
    }

    private void registerPlaceholderExpansions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EconomyPlaceholderExpansion().register();
            EconomyPlaceholderExpansion.startCacheRefresh();
            try {
                Class<?> clazz = Class.forName("org.atrimilan.sidastuffsmp.placeholder.SiDaStuffSmpExpansion");
                Object instance = clazz.getDeclaredConstructor().newInstance();
                clazz.getMethod("register").invoke(instance);
                getLogger().info("PlaceholderAPI support enabled.");
            } catch (Exception e) {
                getLogger().warning("Failed to register SiDaStuffSmpExpansion: " + e.getMessage());
            }
        }
    }

	private void scheduleAuctionTasks() {
		expiryTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			AuctionManager.tickExpiry();
			AuctionManager.tickCleanup();
		}, 60L * 20L, 60L * 20L).getTaskId();

		if (AuctionFirebaseSync.isEnabled()) {
			long syncInterval = AuctionConfig.syncIntervalSeconds() * 20L;
			syncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
				AuctionFirebaseSync.performSync();
			}, syncInterval, syncInterval).getTaskId();
		}
	}

	private void scheduleOrderTasks() {
		orderExpiryTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			OrderManager.tickExpiry();
			OrderManager.tickCleanup();
		}, 60L * 20L, 60L * 20L).getTaskId();

			if (OrderFirebaseSync.isEnabled()) {
				long syncInterval = OrderConfig.syncIntervalSeconds() * 20L;
				orderSyncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
					OrderFirebaseSync.performSync();
				}, syncInterval, syncInterval).getTaskId();
			}
			if (BountyFirebaseSync.isEnabled()) {
				long syncInterval = OrderConfig.syncIntervalSeconds() * 20L;
				bountySyncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
					BountyFirebaseSync.performSync();
				}, syncInterval, syncInterval).getTaskId();
			}
		}

    public static SiDaStuffSmp getInstance() {
        return instance;
    }

    private void loadSpawn() {
        if (getConfig().contains("spawn.world")) {
            World w = Bukkit.getWorld(getConfig().getString("spawn.world", "world"));
            if (w != null) {
                spawnLocation = new Location(w,
                        getConfig().getDouble("spawn.x"),
                        getConfig().getDouble("spawn.y"),
                        getConfig().getDouble("spawn.z"),
                        (float) getConfig().getDouble("spawn.yaw"),
                        (float) getConfig().getDouble("spawn.pitch"));
            }
        }
        if (spawnLocation == null) {
            World w = Bukkit.getWorld("world");
            if (w != null) spawnLocation = w.getSpawnLocation();
        }
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }

    private static class EconomyJoinListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (!EconomyManager.hasAccount(player.getUniqueId())) {
                EconomyManager.createAccount(player.getUniqueId(), player.getName());
            } else {
                EconomyManager.updatePlayerName(player.getUniqueId(), player.getName());
            }
        }
    }
}
