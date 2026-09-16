package despairscent.skyblockm.tweaks;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class DependencyDownloader {

    private static final String ONNX_MAVEN_URL = "https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.20.0/onnxruntime-1.20.0.jar";
    private static final AtomicBoolean initializing = new AtomicBoolean(false);
    private static volatile boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    public static void initAsync() {
        if (ready || initializing.getAndSet(true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Проверяем, доступны ли уже зависимости на classpath
                CaptchaClassifier.resetInit();
                if (CaptchaClassifier.init()) {
                    ready = true;
                    ModUtils.LOGGER.info("[DependencyDownloader] ONNX Runtime готов к работе.");
                    return;
                }

                Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("skyblockm-tweaks").resolve("libs");
                Files.createDirectories(cacheDir);

                Path onnxJar = cacheDir.resolve("onnxruntime-1.20.0.jar");

                if (!Files.exists(onnxJar) || Files.size(onnxJar) < 1000000) {
                    ModUtils.LOGGER.info("[DependencyDownloader] Скачивание библиотеки ONNX Runtime в фоне...");
                    downloadFile(ONNX_MAVEN_URL, onnxJar);
                    ModUtils.LOGGER.info("[DependencyDownloader] ONNX Runtime успешно скачан (" + (Files.size(onnxJar) / (1024 * 1024)) + " MB).");
                }

                // Динамически внедряем JAR в ClassLoader
                injectJar(onnxJar.toFile());

                // Повторно инициализируем классификатор
                CaptchaClassifier.resetInit();
                if (CaptchaClassifier.init()) {
                    ready = true;
                    ModUtils.LOGGER.info("[DependencyDownloader] Авто-решатель капчи полностью готов!");
                } else {
                    ModUtils.LOGGER.warn("[DependencyDownloader] Не удалось инициализировать классификатор после загрузки.");
                }

            } catch (Throwable t) {
                ModUtils.LOGGER.error("[DependencyDownloader] Ошибка загрузки зависимостей: " + t.getMessage(), t);
            } finally {
                initializing.set(false);
            }
        });
    }

    private static void downloadFile(String urlStr, Path targetPath) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (SkyBlockM-Tweaks-plus)");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code + " while downloading " + urlStr);
        }

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void injectJar(File jarFile) {
        try {
            ClassLoader classLoader = DependencyDownloader.class.getClassLoader();
            Method addUrlMethod = null;
            Class<?> clz = classLoader.getClass();
            while (clz != null) {
                try {
                    addUrlMethod = clz.getDeclaredMethod("addUrl", URL.class);
                    break;
                } catch (NoSuchMethodException ignored) {}
                try {
                    addUrlMethod = clz.getDeclaredMethod("addURL", URL.class);
                    break;
                } catch (NoSuchMethodException ignored) {}
                clz = clz.getSuperclass();
            }

            if (addUrlMethod != null) {
                addUrlMethod.setAccessible(true);
                addUrlMethod.invoke(classLoader, jarFile.toURI().toURL());
            } else {
                ModUtils.LOGGER.warn("[DependencyDownloader] Не найден метод addUrl/addURL в ClassLoader " + classLoader.getClass().getName());
            }
        } catch (Throwable t) {
            ModUtils.LOGGER.error("[DependencyDownloader] Не удалось внедрить JAR: " + t.getMessage(), t);
        }
    }
}
