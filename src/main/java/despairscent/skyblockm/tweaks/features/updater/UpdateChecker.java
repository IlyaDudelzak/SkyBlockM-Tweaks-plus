package despairscent.skyblockm.tweaks.features.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import despairscent.skyblockm.tweaks.ModUtils;
import despairscent.skyblockm.tweaks.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private static final String GITHUB_REPO = "IlyaDudelzak/SkyBlockM-Tweaks-plus";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private static volatile UpdateInfo availableUpdate = null;
    private static volatile boolean promptShown = false;

    public static boolean hasAvailableUpdate() {
        return availableUpdate != null;
    }

    public static UpdateInfo getAvailableUpdate() {
        return availableUpdate;
    }

    public static boolean isPromptShown() {
        return promptShown;
    }

    public static void setPromptShown(boolean shown) {
        promptShown = shown;
    }

    public static String getCurrentModVersion() {
        return FabricLoader.getInstance().getModContainer("skyblockm-tweaks")
                .or(() -> FabricLoader.getInstance().getModContainer("skyblockm-tweaks-plus"))
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElseGet(() -> {
                    try {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null && client.getGameVersion() != null) {
                            return client.getGameVersion();
                        }
                    } catch (Throwable ignored) {}
                    return "1.21.1";
                });
    }

    public static String getCurrentGitHash() {
        return FabricLoader.getInstance().getModContainer("skyblockm-tweaks")
                .or(() -> FabricLoader.getInstance().getModContainer("skyblockm-tweaks-plus"))
                .map(c -> {
                    try {
                        net.fabricmc.loader.api.metadata.CustomValue val = c.getMetadata().getCustomValue("gitHash");
                        return val != null ? val.getAsString() : "";
                    } catch (Exception ignored) {
                        return "";
                    }
                })
                .orElse("");
    }

    public static String extractCommitHashFromBody(String body) {
        if (body == null || body.isEmpty()) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("commit/([a-f0-9]{7,40})").matcher(body);
        return m.find() ? m.group(1) : "";
    }

    private static volatile long lastCheckTimestamp = 0L;
    private static final long MIN_CHECK_INTERVAL_MS = 60_000L;
    private static String lastChatNotifiedVersion = "";
    private static java.util.concurrent.ScheduledExecutorService scheduler = null;

    public static synchronized void startPeriodicCheck() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SkyBlockM-UpdateChecker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null && ModUtils.CONFIG.updater.enabled) {
                    checkAsync();
                }
            } catch (Throwable ignored) {}
        }, 30, 30, java.util.concurrent.TimeUnit.MINUTES);
    }

    public static void onWorldOrServerDisconnect() {
        promptShown = false;
        long now = System.currentTimeMillis();
        if (now - lastCheckTimestamp > MIN_CHECK_INTERVAL_MS) {
            checkAsync();
        }
    }

    public static void checkAsync() {
        if (ModUtils.CONFIG == null || ModUtils.CONFIG.updater == null || !ModUtils.CONFIG.updater.enabled) {
            return;
        }
        lastCheckTimestamp = System.currentTimeMillis();
        checkManually().thenAccept(update -> {
            if (update != null) {
                availableUpdate = update;
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        if (client.currentScreen instanceof TitleScreen && !promptShown) {
                            promptShown = true;
                            client.setScreen(new UpdatePromptScreen(client.currentScreen, update));
                        } else if (client.player != null) {
                            sendUpdateNotificationToChat(client, update);
                        }
                    });
                }
            }
        });
    }

    private static void sendUpdateNotificationToChat(MinecraftClient client, UpdateInfo update) {
        if (client.player == null) return;
        if (update.version.equals(lastChatNotifiedVersion)) return;
        lastChatNotifiedVersion = update.version;

        String badge = update.isNightly ? "§e[Dev / Nightly]" : "§a[Релиз]";
        net.minecraft.text.Text msg = net.minecraft.text.Text.literal("§6[SkyBlockM Tweaks+] §aДоступно обновление " + badge + " §f" + update.version + "§a! Окно установки откроется при выходе в меню.");
        client.player.sendMessage(msg, false);
    }

    public static CompletableFuture<UpdateInfo> checkManually() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Config.UpdateChannel channel = ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null
                        ? ModUtils.CONFIG.updater.channel
                        : Config.UpdateChannel.STABLE;

                String url;
                boolean isNightly = (channel == Config.UpdateChannel.NIGHTLY);
                if (isNightly) {
                    url = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/tags/nightly";
                } else {
                    url = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "SkyBlockM-Tweaks-plus-Updater")
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    ModUtils.LOGGER.warn("[UpdateChecker] Не удалось проверить обновления (HTTP " + response.statusCode() + ")");
                    return null;
                }

                JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
                String tagName = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
                String releaseName = release.has("name") && !release.get("name").isJsonNull() ? release.get("name").getAsString() : tagName;
                String body = release.has("body") && !release.get("body").isJsonNull() ? release.get("body").getAsString() : "";
                String publishedAt = release.has("published_at") && !release.get("published_at").isJsonNull() ? release.get("published_at").getAsString() : "";
                String htmlUrl = release.has("html_url") && !release.get("html_url").isJsonNull() ? release.get("html_url").getAsString() : "";

                String mcVersion = getMinecraftVersion();
                String currentModVersion = getCurrentModVersion();

                // Поиск подходящего jar ассета под текущую версию MC (+mc<mcVersion>.jar)
                if (!release.has("assets") || !release.get("assets").isJsonArray()) {
                    return null;
                }

                JsonArray assets = release.getAsJsonArray("assets");
                JsonObject matchingAsset = null;
                String targetSuffix = "+mc" + mcVersion + ".jar";

                for (JsonElement el : assets) {
                    if (!el.isJsonObject()) continue;
                    JsonObject asset = el.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString() : "";
                    if (name.endsWith(targetSuffix) || name.contains("+mc" + mcVersion)) {
                        matchingAsset = asset;
                        break;
                    }
                }

                if (matchingAsset == null) {
                    ModUtils.LOGGER.info("[UpdateChecker] Релиз найден, но нет сборки под Minecraft " + mcVersion);
                    return null;
                }

                String assetName = matchingAsset.get("name").getAsString();
                String downloadUrl = matchingAsset.get("browser_download_url").getAsString();
                long assetSize = matchingAsset.has("size") ? matchingAsset.get("size").getAsLong() : 0;

                // Извлечение версии мода из имени ассета (skyblockm-tweaks-plus-<version>+mc...)
                String remoteVersion = extractVersionFromAssetName(assetName, mcVersion);
                if (remoteVersion.isEmpty()) {
                    remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                }

                if (ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null) {
                    if (remoteVersion.equalsIgnoreCase(ModUtils.CONFIG.updater.ignoredVersion)) {
                        return null; // Версия проигнорирована пользователем
                    }
                }

                if (isNightly) {
                    boolean isUpdateAvailable = false;

                    // 1. Сравнение Git Hash коммита
                    String localCommit = getCurrentGitHash();
                    String remoteCommit = extractCommitHashFromBody(body);

                    if (!localCommit.isEmpty() && !remoteCommit.isEmpty() && !localCommit.equals("unknown")) {
                        if (remoteCommit.startsWith(localCommit) || localCommit.startsWith(remoteCommit)) {
                            return null; // Тот же самый коммит, билд уже установлен
                        } else {
                            isUpdateAvailable = true;
                        }
                    }

                    // 2. Сравнение сохраненного timestamp последней загрузки
                    String lastTimestamp = ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null
                            ? ModUtils.CONFIG.updater.lastNightlyTimestamp
                            : "";
                    if (!lastTimestamp.isEmpty() && !publishedAt.isEmpty()) {
                        try {
                            java.time.Instant remoteInstant = java.time.Instant.parse(publishedAt);
                            java.time.Instant lastInstant = java.time.Instant.parse(lastTimestamp);
                            if (remoteInstant.isAfter(lastInstant)) {
                                isUpdateAvailable = true;
                            } else if (!isUpdateAvailable) {
                                return null; // Этот или более новый nightly уже был скачан
                            }
                        } catch (Exception ignored) {}
                    }

                    // 3. Сравнение с датой изменения локального jar-файла
                    if (!isUpdateAvailable) {
                        try {
                            java.nio.file.Path currentJar = ModDownloader.getCurrentJarPath();
                            if (currentJar != null && java.nio.file.Files.exists(currentJar) && !publishedAt.isEmpty()) {
                                java.time.Instant remoteInstant = java.time.Instant.parse(publishedAt);
                                java.time.Instant localFileInstant = java.nio.file.Files.getLastModifiedTime(currentJar).toInstant();
                                if (remoteInstant.isAfter(localFileInstant.plusSeconds(120))) {
                                    isUpdateAvailable = true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // 4. Проверка по SemVer (если в ночной сборке подняли версию)
                    if (isNewerVersion(remoteVersion, currentModVersion)) {
                        isUpdateAvailable = true;
                    }

                    if (!isUpdateAvailable) {
                        return null;
                    }

                    // Для наглядности в UI добавляем краткий хэш коммита к версии
                    if (!remoteCommit.isEmpty()) {
                        String shortCommit = remoteCommit.length() > 7 ? remoteCommit.substring(0, 7) : remoteCommit;
                        remoteVersion = remoteVersion + " (" + shortCommit + ")";
                    }
                } else {
                    // Для стабильных релизов проверяем semver
                    if (!isNewerVersion(remoteVersion, currentModVersion)) {
                        return null; // Текущая версия актуальна
                    }
                }

                ModUtils.LOGGER.info(String.format("[UpdateChecker] Доступно обновление: %s (текущая: %s, mc: %s)",
                        remoteVersion, currentModVersion, mcVersion));

                return new UpdateInfo(remoteVersion, releaseName, tagName, body, assetName,
                        downloadUrl, assetSize, isNightly, publishedAt, htmlUrl);

            } catch (Exception e) {
                ModUtils.LOGGER.warn("[UpdateChecker] Ошибка при проверке обновлений: " + e.getMessage());
                return null;
            }
        });
    }

    private static String extractVersionFromAssetName(String assetName, String mcVersion) {
        String prefix = "skyblockm-tweaks-plus-";
        String suffix = "+mc" + mcVersion + ".jar";
        if (assetName.startsWith(prefix) && assetName.endsWith(suffix)) {
            return assetName.substring(prefix.length(), assetName.length() - suffix.length());
        }
        return "";
    }

    public static boolean isNewerVersion(String remote, String current) {
        if (remote == null || current == null || current.equals("unknown")) return false;
        String cleanRemote = remote.replaceAll("^[^0-9]+", "").split("[+-]")[0];
        String cleanCurrent = current.replaceAll("^[^0-9]+", "").split("[+-]")[0];

        String[] rParts = cleanRemote.split("\\.");
        String[] cParts = cleanCurrent.split("\\.");
        int len = Math.max(rParts.length, cParts.length);

        for (int i = 0; i < len; i++) {
            int r = i < rParts.length ? parseSafeInt(rParts[i]) : 0;
            int c = i < cParts.length ? parseSafeInt(cParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    private static int parseSafeInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }
}
