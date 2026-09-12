package despairscent.skyblockm.tweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;
import static despairscent.skyblockm.tweaks.ModUtils.LOGGER;

public class SkyBlockPackManager {
    public static final String GITLAB_URL = "https://gitlab.com/worldm/storage/skyblock-resourcepack/-/raw/actual/SkyBlockM.zip?ref_type=heads&inline=false";
    public static final String PACK_FILENAME = "SkyBlockM.zip";
    public static final String PACK_ID = "file/SkyBlockM.zip";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public static void addDefaultServersIfFirstLaunch(MinecraftClient client) {
        if (CONFIG == null) return;
        if (!CONFIG.firstLaunchServersAdded) {
            CONFIG.firstLaunchServersAdded = true;
            CONFIG.save();

            try {
                net.minecraft.client.option.ServerList serverList = new net.minecraft.client.option.ServerList(client);
                serverList.loadFile();

                boolean hasJustMC = false;
                boolean hasSkyblock = false;

                for (int i = 0; i < serverList.size(); i++) {
                    net.minecraft.client.network.ServerInfo info = serverList.get(i);
                    if (info.address != null) {
                        String addr = info.address.toLowerCase();
                        if (addr.contains("join.justmc.io") || (addr.contains("justmc.io") && !addr.contains("skyblock"))) {
                            hasJustMC = true;
                        }
                        if (addr.contains("skyblock.justmc.ru") || addr.contains("skyblockm")) {
                            hasSkyblock = true;
                        }
                    }
                }

                boolean added = false;
                if (!hasJustMC) {
                    serverList.add(new net.minecraft.client.network.ServerInfo("JustMC", "join.justmc.io", net.minecraft.client.network.ServerInfo.ServerType.OTHER), false);
                    added = true;
                }
                if (!hasSkyblock) {
                    serverList.add(new net.minecraft.client.network.ServerInfo("SkyBlockM", "skyblock.justmc.ru", net.minecraft.client.network.ServerInfo.ServerType.OTHER), false);
                    added = true;
                }

                if (added) {
                    serverList.saveFile();
                    LOGGER.info("SkyBlockM Tweaks: Added default JustMC and SkyBlockM servers to server list.");
                }
            } catch (Exception e) {
                LOGGER.error("SkyBlockM Tweaks: Failed to add default servers", e);
            }
        }
    }

    public static void onClientStarted(MinecraftClient client) {
        if (CONFIG == null || CONFIG.skyblockPackOptimization == null || !CONFIG.skyblockPackOptimization.isEnabled()) {
            return;
        }

        Path packPath = client.getResourcePackDir().resolve(PACK_FILENAME);

        if (CONFIG.skyblockPackOptimization.mode == despairscent.skyblockm.tweaks.config.Config.AutoLoadMode.GITLAB) {
            checkAndDownloadGitLabPack(client, packPath);
        }

        // Ensure pack is in enabled list if it exists
        ensurePackEnabled(client, packPath);
    }

    public static void ensurePackEnabled(MinecraftClient client, Path packPath) {
        if (!Files.exists(packPath)) return;

        List<String> enabled = new ArrayList<>(client.options.resourcePacks);
        if (!enabled.contains(PACK_ID)) {
            int insertIdx = 0;
            for (int i = 0; i < enabled.size(); i++) {
                String id = enabled.get(i);
                if (id.equals("vanilla") || id.equals("fabric")) {
                    insertIdx = i + 1;
                }
            }
            enabled.add(insertIdx, PACK_ID);
            client.options.resourcePacks.clear();
            client.options.resourcePacks.addAll(enabled);
            client.options.write();

            client.getResourcePackManager().scanPacks();
            client.getResourcePackManager().setEnabledProfiles(enabled);
            client.reloadResources();
            LOGGER.info("SkyBlockM Tweaks: Enabled and loaded SkyBlockM.zip at startup.");
        }
    }

    private static void checkAndDownloadGitLabPack(MinecraftClient client, Path packPath) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest headRequest = HttpRequest.newBuilder()
                        .uri(URI.create(GITLAB_URL))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<Void> headResponse = HTTP_CLIENT.send(headRequest, HttpResponse.BodyHandlers.discarding());
                String etag = headResponse.headers().firstValue("etag").orElse("");

                boolean exists = Files.exists(packPath) && Files.size(packPath) > 1000;
                boolean sameEtag = !etag.isEmpty() && etag.equals(CONFIG.skyblockPackOptimization.lastGitLabEtag);

                if (exists && sameEtag) {
                    LOGGER.info("SkyBlockM Tweaks: SkyBlockM pack is already up to date with GitLab (ETag: {}).", etag);
                    return;
                }

                LOGGER.info("SkyBlockM Tweaks: Downloading updated SkyBlockM pack from GitLab...");
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(URI.create(GITLAB_URL))
                        .GET()
                        .timeout(Duration.ofMinutes(2))
                        .build();

                HttpResponse<InputStream> getResponse = HTTP_CLIENT.send(getRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (getResponse.statusCode() == 200) {
                    Path tmpPath = client.getResourcePackDir().resolve(PACK_FILENAME + ".tmp");
                    try (InputStream in = getResponse.body()) {
                        Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(tmpPath, packPath, StandardCopyOption.REPLACE_EXISTING);

                    String sha1 = calculateSha1(packPath);
                    CONFIG.skyblockPackOptimization.lastGitLabEtag = etag;
                    if (!sha1.isEmpty()) {
                        CONFIG.skyblockPackOptimization.lastHash = sha1;
                    }
                    CONFIG.save();
                    LOGGER.info("SkyBlockM Tweaks: Successfully downloaded SkyBlockM pack from GitLab (SHA1: {}).", sha1);

                    client.execute(() -> {
                        ensurePackEnabled(client, packPath);
                        client.getResourcePackManager().scanPacks();
                        client.reloadResources();
                    });
                }
            } catch (Exception e) {
                LOGGER.error("SkyBlockM Tweaks: Failed to download pack from GitLab", e);
            }
        });
    }

    public static boolean isSkyBlockM(String serverAddress, String packetUrl) {
        if (serverAddress != null) {
            String addr = serverAddress.toLowerCase();
            if (addr.contains("justmc.ru") || addr.contains("justmc.io") || addr.contains("skyblock")) {
                return true;
            }
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getCurrentServerEntry() != null) {
            String addr = client.getCurrentServerEntry().address.toLowerCase();
            if (addr.contains("justmc.ru") || addr.contains("justmc.io") || addr.contains("skyblock")) {
                return true;
            }
        }
        if (packetUrl != null) {
            String url = packetUrl.toLowerCase();
            if (url.contains("justmc") || url.contains("skyblock") || url.contains("worldm") || url.contains("gitlab")) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldBypassServerPack(ResourcePackSendS2CPacket packet, String serverAddress) {
        if (CONFIG == null || CONFIG.skyblockPackOptimization == null || !CONFIG.skyblockPackOptimization.isEnabled()) {
            return false;
        }
        if (!isSkyBlockM(serverAddress, packet.url())) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Path packPath = client.getResourcePackDir().resolve(PACK_FILENAME);
        try {
            if (!Files.exists(packPath) || Files.size(packPath) < 1000) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        // SkyBlockM.zip is already present locally! Ensure it's enabled in options if not already
        ensurePackEnabled(client, packPath);

        if (CONFIG.skyblockPackOptimization.mode == despairscent.skyblockm.tweaks.config.Config.AutoLoadMode.GITLAB) {
            return true;
        }

        if (CONFIG.skyblockPackOptimization.mode == despairscent.skyblockm.tweaks.config.Config.AutoLoadMode.SERVER) {
            String expectedHash = packet.hash();
            if (expectedHash != null && !expectedHash.isEmpty() && !expectedHash.equalsIgnoreCase(CONFIG.skyblockPackOptimization.lastHash)) {
                // If the hash changed on server, allow downloading the new version
                return false;
            }
            return true;
        }

        return false;
    }

    public static void onServerPackSend(ResourcePackSendS2CPacket packet, String serverAddress) {
        if (CONFIG == null || CONFIG.skyblockPackOptimization == null) return;
        if (CONFIG.skyblockPackOptimization.mode != despairscent.skyblockm.tweaks.config.Config.AutoLoadMode.SERVER) return;
        if (serverAddress == null) return;
        String address = serverAddress.toLowerCase();
        if (!address.contains("justmc.ru") && !address.contains("justmc.io")) return;

        String expectedHash = packet.hash();
        if (expectedHash == null || expectedHash.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Path packPath = client.getResourcePackDir().resolve(PACK_FILENAME);

        CompletableFuture.runAsync(() -> {
            try {
                Path downloaded = client.runDirectory.toPath().resolve("downloads").resolve(packet.id().toString()).resolve(expectedHash);
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(500);
                    if (Files.exists(downloaded) && Files.size(downloaded) > 1000) {
                        Thread.sleep(1000);
                        Files.copy(downloaded, packPath, StandardCopyOption.REPLACE_EXISTING);
                        CONFIG.skyblockPackOptimization.lastHash = expectedHash;
                        CONFIG.save();
                        LOGGER.info("SkyBlockM Tweaks: Cached downloaded server pack to SkyBlockM.zip (hash: {}).", expectedHash);
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("SkyBlockM Tweaks: Failed to cache server resource pack", e);
            }
        });
    }

    public static String calculateSha1(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
