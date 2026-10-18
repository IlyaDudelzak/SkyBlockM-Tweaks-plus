package despairscent.skyblockm.tweaks.features.updater;

import despairscent.skyblockm.tweaks.ModUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModDownloader {

    public interface DownloadProgressCallback {
        void onProgress(float percentage, long downloadedBytes, long totalBytes);
    }

    public static void downloadAndInstall(UpdateInfo update, DownloadProgressCallback progressCallback,
                                          Runnable onComplete, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .connectTimeout(Duration.ofSeconds(15))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(update.downloadUrl))
                        .header("User-Agent", "SkyBlockM-Tweaks-plus-Updater")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    onError.accept("HTTP " + response.statusCode());
                    return;
                }

                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(update.assetSize);

                Path modsDir = getModsDirectory();
                if (!Files.exists(modsDir)) {
                    Files.createDirectories(modsDir);
                }

                Path targetJar = modsDir.resolve(update.assetName);
                Path tempJar = modsDir.resolve(update.assetName + ".download");

                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(tempJar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                    byte[] buffer = new byte[8192];
                    long downloaded = 0;
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (contentLength > 0 && progressCallback != null) {
                            float percent = (float) downloaded / contentLength;
                            progressCallback.onProgress(percent, downloaded, contentLength);
                        }
                    }
                }

                // Атомарно перемещаем временный файл в целевой jar
                Files.move(tempJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
                ModUtils.LOGGER.info("[ModDownloader] Новый jar успешно загружен: " + targetJar.getFileName());

                // Отключаем или удаляем старый jar мода
                Path currentJar = getCurrentJarPath();
                if (currentJar != null && Files.exists(currentJar) &&
                        !currentJar.toAbsolutePath().normalize().equals(targetJar.toAbsolutePath().normalize())) {
                    disableOrDeleteOldJar(currentJar);
                }

                if (update.isNightly && ModUtils.CONFIG != null && ModUtils.CONFIG.updater != null) {
                    ModUtils.CONFIG.updater.lastNightlyTimestamp = update.publishedAt;
                    ModUtils.CONFIG.save();
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (Exception e) {
                ModUtils.LOGGER.error("[ModDownloader] Ошибка загрузки обновления", e);
                if (onError != null) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    onError.accept(msg);
                }
            }
        });
    }

    private static void disableOrDeleteOldJar(Path oldJar) {
        try {
            Files.deleteIfExists(oldJar);
            ModUtils.LOGGER.info("[ModDownloader] Старый jar удален: " + oldJar.getFileName());
        } catch (Exception e) {
            // В Windows JVM держит открытый файл jar во время работы.
            // Переименование в .disabled предотвращает его повторную загрузку Fabric'ом при следующем старте:
            try {
                Path disabled = oldJar.resolveSibling(oldJar.getFileName().toString() + ".disabled");
                Files.move(oldJar, disabled, StandardCopyOption.REPLACE_EXISTING);
                ModUtils.LOGGER.info("[ModDownloader] Старый jar переименован в .disabled: " + disabled.getFileName());
            } catch (Exception ex2) {
                oldJar.toFile().deleteOnExit();
                ModUtils.LOGGER.info("[ModDownloader] Старый jar помечен на удаление при выходе (deleteOnExit)");
            }
        }
    }

    public static Path getCurrentJarPath() {
        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("skyblockm-tweaks")
                .or(() -> FabricLoader.getInstance().getModContainer("skyblockm-tweaks-plus"));
        return mod.map(m -> {
            List<Path> paths = m.getOrigin().getPaths();
            if (!paths.isEmpty()) {
                Path p = paths.get(0);
                if (p.toString().endsWith(".jar")) {
                    return p;
                }
            }
            return null;
        }).orElse(null);
    }

    public static Path getModsDirectory() {
        Path currentJar = getCurrentJarPath();
        if (currentJar != null && currentJar.getParent() != null) {
            return currentJar.getParent();
        }
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }
}
