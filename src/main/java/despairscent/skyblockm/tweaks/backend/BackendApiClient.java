package despairscent.skyblockm.tweaks.backend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import despairscent.skyblockm.tweaks.ModUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class BackendApiClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static final String SERVER_URL = "http://37.27.211.81:8080";
    private static final URI CAPTCHA_UPLOAD_URI, TELEMETRY_URI;

    static {
        try {
            CAPTCHA_UPLOAD_URI = new URI(SERVER_URL + "/api/v1/captcha/batch");
            TELEMETRY_URI = new URI(SERVER_URL + "/api/v1/telemetry");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CaptchaFrameData {
        public final byte[] colors;
        public final String predictedClass;
        public final float confidence;

        public CaptchaFrameData(byte[] colors, String predictedClass, float confidence) {
            this.colors = colors;
            this.predictedClass = predictedClass;
            this.confidence = confidence;
        }
    }

    public static boolean isEnabled() {
        return ModUtils.CONFIG != null && ModUtils.CONFIG.backendApi != null && ModUtils.CONFIG.backendApi.enabled;
    }

    /**
     * Асинхронно отправляет все 9 рамок карты на эндпоинт POST /api/v1/captcha
     */
    public static void uploadCaptchaFrames(List<CaptchaFrameData> frames, boolean solvedSuccessfully) {
        if (!isEnabled() || !ModUtils.CONFIG.backendApi.uploadCaptcha || frames == null || frames.isEmpty()) {
            return;
        }

        JsonArray captchas = new JsonArray(9);

        for (CaptchaFrameData frame : frames) {
            if (frame.colors == null || frame.colors.length < 16384) continue;

            JsonObject captcha = new JsonObject();
            captcha.addProperty("colors", Base64.getEncoder().encodeToString(frame.colors));
            captcha.addProperty("predictedClass", frame.predictedClass);
            float safeConfidence = Math.min(1.0f, Math.max(0.0f, frame.confidence));
            captcha.addProperty("confidence", safeConfidence);
            captcha.addProperty("solvedSuccessfully", solvedSuccessfully);

            captchas.add(captcha);
        }
        JsonObject body = new JsonObject();
        body.add("captchas", captchas);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(CAPTCHA_UPLOAD_URI)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                        ModUtils.LOGGER.info("[BackendApiClient] Карты капч успешно сохранена на сервере (" + resp.statusCode() + ")");
                    } else {
                        ModUtils.LOGGER.warn("[BackendApiClient] Ошибка ответа сервера при загрузке капч: " + resp.statusCode() + " " + resp.body());
                    }
                })
                .exceptionally(ex -> {
                    ModUtils.LOGGER.warn("[BackendApiClient] Ошибка отправки карты капч (сервер недоступен): " + ex.getMessage());
                    return null;
                });
        ModUtils.LOGGER.info(String.format("[BackendApiClient] Загрузка %d рамок капчи на сервер (solved=%s)...", frames.size(), solvedSuccessfully));
    }

    /**
     * Асинхронно отправляет событие телеметрии на эндпоинт POST /api/v1/telemetry
     */
    public static void sendTelemetry(String eventType, Map<String, Object> payload) {
        if (!isEnabled() || !ModUtils.CONFIG.backendApi.sendTelemetry) {
            return;
        }

        String modVersion = FabricLoader.getInstance().getModContainer("skyblockm-tweaks")
                .or(() -> FabricLoader.getInstance().getModContainer("skyblockm-tweaks-plus"))
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        String mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElseGet(() -> {
                    try {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null && client.getGameVersion() != null) {
                            return client.getGameVersion();
                        }
                    } catch (Throwable ignored) {}
                    return "unknown";
                });

        if (modVersion.length() > 32) modVersion = modVersion.substring(0, 32);
        if (mcVersion.length() > 32) mcVersion = mcVersion.substring(0, 32);

        JsonObject body = new JsonObject();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSession() != null) {
                String username = client.getSession().getUsername();
                if (username != null && !username.isBlank()) {
                    body.addProperty("username", username);
                }
            }
        } catch (Throwable ignored) {}

        body.addProperty("modVersion", modVersion);
        body.addProperty("mcVersion", mcVersion);
        body.addProperty("eventType", eventType);
        if (payload != null && !payload.isEmpty()) {
            body.add("payload", GSON.toJsonTree(payload));
        } else {
            body.add("payload", new JsonObject());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(TELEMETRY_URI)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200 || resp.statusCode() == 202) {
                        ModUtils.LOGGER.info("[BackendApiClient] Телеметрия принята сервером (" + resp.statusCode() + "): " + eventType);
                    } else {
                        ModUtils.LOGGER.warn("[BackendApiClient] Ошибка ответа сервера при отправке телеметрии " + eventType + ": " + resp.statusCode() + " " + resp.body());
                    }
                })
                .exceptionally(ex -> {
                    ModUtils.LOGGER.warn("[BackendApiClient] Ошибка отправки телеметрии (" + eventType + "): " + ex.getMessage());
                    return null;
                });
    }
}
