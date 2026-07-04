package org.atrimilan.sidastuffsmp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.atrimilan.sidastuffsmp.SiDaStuffSmp;
import org.atrimilan.sidastuffsmp.sync.FirebaseConfig;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /verify} command. Generates a one-time code linked to the player's UUID,
 * writes it to Realtime Database under {@code verifyCodes/<code>}, and tells the player
 * the code so they can enter it on the website.
 */
public class VerifyCommand {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_MINUTES = 5;

    public static final String DESCRIPTION = "Generate a verification code to link your Minecraft account on the website";
    public static final java.util.Set<String> ALIASES = java.util.Set.of();

    private VerifyCommand() {}

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("verify")
                .requires(sender -> sender.getSender() instanceof Player)
                .executes(VerifyCommand::execute)
                .build();
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        Player player = (Player) ctx.getSource().getSender();
        SiDaStuffSmp plugin = SiDaStuffSmp.getInstance();

        if (!FirebaseConfig.enabled()) {
            player.sendMessage(Component.text("Website verification is currently disabled.")
                    .color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String code = generateCode();

        long createdAt = Instant.now().getEpochSecond();
        long expiresAt = Instant.now().plus(CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES).getEpochSecond();

        String jsonBody = "{"
                + "\"mcUsername\":\"" + playerName + "\","
                + "\"mcUUID\":\"" + playerUUID + "\","
                + "\"createdAt\":" + createdAt + ","
                + "\"expiresAt\":" + expiresAt + ","
                + "\"claimed\":false,"
                + "\"claimedBy\":\"\""
                + "}";

        String databaseUrl = FirebaseConfig.getDatabaseUrl();
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            player.sendMessage(Component.text("Website verification is not configured.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        databaseUrl = databaseUrl.endsWith("/") ? databaseUrl.substring(0, databaseUrl.length() - 1) : databaseUrl;
        String url = databaseUrl + "/verifyCodes/" + code + ".json";

        String accessToken;
        try {
            accessToken = getAccessToken(plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("Verify: failed to get access token: " + e.getMessage());
            player.sendMessage(Component.text("Unable to connect to verification service.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Component.text(""));
                            player.sendMessage(Component.text("========================================").color(NamedTextColor.GREEN));
                            player.sendMessage(
                                Component.text("Your verification code: ").color(NamedTextColor.GOLD)
                                    .append(Component.text(code).color(NamedTextColor.YELLOW)
                                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                            );
                            player.sendMessage(Component.text("Expires in " + CODE_EXPIRY_MINUTES + " minutes.").color(NamedTextColor.GRAY));
                            player.sendMessage(Component.text("Enter this code on the website to link your account!").color(NamedTextColor.GRAY));
                            player.sendMessage(Component.text("========================================").color(NamedTextColor.GREEN));
                        });
                    } else {
                        plugin.getLogger().warning("Verify: Firebase returned HTTP " + response.statusCode() + ": " + response.body());
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Component.text("Failed to generate verification code.").color(NamedTextColor.RED));
                        });
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Verify: failed to write to RTDB: " + ex.getMessage());
                   Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(Component.text("Failed to generate verification code.").color(NamedTextColor.RED));
                    });
                    return null;
                });

        return Command.SINGLE_SUCCESS;
    }

    private static String getAccessToken(SiDaStuffSmp plugin) throws Exception {
        File serviceAccountFile = new File(plugin.getDataFolder(), FirebaseConfig.getServiceAccountPath());
        if (!serviceAccountFile.exists()) {
            File absolute = new File(FirebaseConfig.getServiceAccountPath());
            if (absolute.isAbsolute() && absolute.exists()) {
                serviceAccountFile = absolute;
            } else {
                throw new IllegalStateException("Firebase service account file not found.");
            }
        }

        try (java.io.FileInputStream fis = new java.io.FileInputStream(serviceAccountFile)) {
            com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.ServiceAccountCredentials.fromStream(fis)
                    .createScoped(java.util.List.of("https://www.googleapis.com/auth/firebase.database",
                            "https://www.googleapis.com/auth/userinfo.email"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private static String generateCode() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(rand.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}