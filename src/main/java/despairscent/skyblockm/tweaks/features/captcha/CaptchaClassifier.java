package despairscent.skyblockm.tweaks.features.captcha;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import net.minecraft.client.MinecraftClient;
//? if >=1.20.5 {
import net.minecraft.component.type.MapIdComponent;
//?}
import net.minecraft.item.map.MapState;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

public class CaptchaClassifier {

    public enum CaptchaClass {
        CHESTS("Сундуки"),
        PIGS("Свиньи"),
        ZOMBIES("Зомби"),
        UNKNOWN("Неизвестно");

        private final String displayName;

        CaptchaClass(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        public char getCode() {
            return switch (this) {
                case PIGS -> 'P';
                case ZOMBIES -> 'Z';
                case CHESTS -> 'C';
                default -> '?';
            };
        }
    }

    public record ClassificationResult(CaptchaClass captchaClass, float confidence, float[] rawScores) {}

    private static final int[][] BASE_COLORS = {
            {0, 0, 0}, {127, 178, 56}, {247, 233, 163}, {199, 199, 199}, {255, 0, 0},
            {160, 160, 255}, {167, 167, 167}, {0, 124, 0}, {255, 255, 255}, {164, 168, 184},
            {151, 109, 77}, {112, 112, 112}, {64, 64, 255}, {143, 119, 72}, {255, 252, 245},
            {216, 127, 51}, {178, 76, 216}, {102, 153, 216}, {229, 229, 51}, {127, 204, 25},
            {242, 127, 165}, {76, 76, 76}, {153, 153, 153}, {76, 127, 153}, {127, 63, 178},
            {51, 76, 178}, {102, 76, 51}, {102, 127, 51}, {153, 51, 51}, {25, 25, 25},
            {250, 238, 77}, {92, 219, 213}, {74, 128, 255}, {0, 217, 58}, {129, 86, 49},
            {112, 2, 0}, {209, 177, 161}, {159, 82, 36}, {149, 87, 108}, {112, 108, 138},
            {186, 133, 36}, {103, 117, 53}, {160, 77, 78}, {57, 41, 35}, {135, 107, 98},
            {87, 92, 92}, {122, 73, 88}, {76, 62, 92}, {76, 50, 35}, {76, 82, 42},
            {142, 60, 46}, {37, 22, 16}, {189, 48, 49}, {148, 63, 97}, {92, 25, 29},
            {22, 126, 134}, {58, 142, 140}, {86, 44, 62}, {20, 180, 133}, {100, 100, 100},
            {216, 175, 147}, {127, 167, 150}
    };

    private static final float[] MULTIPLIERS = {0.705f, 0.862f, 1.0f, 0.529f};
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private static OrtEnvironment env;
    private static OrtSession session;
    private static boolean initFailed = false;

    public static synchronized void resetInit() {
        initFailed = false;
    }

    public static synchronized boolean init() {
        if (session != null) return true;
        if (initFailed) return false;

        try {
            env = OrtEnvironment.getEnvironment();
            try (InputStream is = CaptchaClassifier.class.getResourceAsStream("/assets/skyblockm-tweaks/captcha_model.onnx")) {
                if (is == null) {
                    System.err.println("[CaptchaClassifier] ONNX model resource not found!");
                    initFailed = true;
                    return false;
                }
                java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("skyblockm_captcha_", ".onnx");
                tempFile.toFile().deleteOnExit();
                java.nio.file.Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                session = env.createSession(tempFile.toAbsolutePath().toString(), new OrtSession.SessionOptions());
            }
            return true;
        } catch (Throwable t) {
            System.err.println("[CaptchaClassifier] Failed to initialize ONNX Runtime session: " + t.getMessage());
            t.printStackTrace();
            initFailed = true;
            return false;
        }
    }

    public static boolean hasLoadedMapColors(byte[] colors) {
        if (colors == null || colors.length < 128 * 128) return false;
        int nonZero = 0;
        for (int i = 0; i < colors.length; i += 8) {
            if (colors[i] != 0) {
                nonZero++;
                if (nonZero >= 20) return true;
            }
        }
        return false;
    }

    /**
     * Распознает класс капчи с уровнем уверенности по предмету карты в ItemStack.
     */
    public static ClassificationResult classifyWithConfidence(net.minecraft.item.ItemStack stack, net.minecraft.client.world.ClientWorld world) {
        if (stack == null || stack.isEmpty() || world == null) return null;
        //? if >=1.20.5 {
        net.minecraft.component.type.MapIdComponent mapId = stack.get(net.minecraft.component.DataComponentTypes.MAP_ID);
        if (mapId == null) return null;
        MapState mapState = world.getMapState(mapId);
        //?} else {
        /*MapState mapState = net.minecraft.item.FilledMapItem.getMapState(stack, world);
        *///?}
        if (mapState == null || !hasLoadedMapColors(mapState.colors)) {
            return null;
        }
        return classifyWithConfidence(mapState.colors);
    }

    /**
     * Распознает класс капчи с уровнем уверенности из сущности рамки ItemFrameEntity.
     */
    public static ClassificationResult classifyWithConfidence(net.minecraft.entity.decoration.ItemFrameEntity frame, net.minecraft.client.world.ClientWorld world) {
        if (frame == null || world == null) return null;
        return classifyWithConfidence(frame.getHeldItemStack(), world);
    }

    /**
     * Распознает класс капчи по предмету карты в ItemStack.
     */
    public static CaptchaClass classify(net.minecraft.item.ItemStack stack, net.minecraft.client.world.ClientWorld world) {
        ClassificationResult res = classifyWithConfidence(stack, world);
        return res != null ? res.captchaClass() : CaptchaClass.UNKNOWN;
    }

    /**
     * Распознает класс капчи из сущности рамки ItemFrameEntity.
     */
    public static CaptchaClass classify(net.minecraft.entity.decoration.ItemFrameEntity frame, net.minecraft.client.world.ClientWorld world) {
        ClassificationResult res = classifyWithConfidence(frame, world);
        return res != null ? res.captchaClass() : CaptchaClass.UNKNOWN;
    }

    /**
     * Распознает класс капчи по ID карты в текущем мире Minecraft.
     */
    public static CaptchaClass classify(int mapId) {
        ClassificationResult result = classifyWithConfidence(mapId);
        return result != null ? result.captchaClass() : CaptchaClass.UNKNOWN;
    }

    /**
     * Распознает класс капчи с уровнем уверенности по ID карты.
     */
    public static ClassificationResult classifyWithConfidence(int mapId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;

        //? if >=1.20.5 {
        MapState mapState = client.world.getMapState(new MapIdComponent(mapId));
        //?} else {
        /*MapState mapState = client.world.getMapState("map_" + mapId);
        *///?}
        if (mapState == null || mapState.colors == null || mapState.colors.length == 0) {
            return null;
        }

        return classifyWithConfidence(mapState.colors);
    }

    /**
     * Распознает класс капчи напрямую из массива байтов карты (128x128 = 16384 байт).
     */
    public static CaptchaClass classify(byte[] mapBytes) {
        ClassificationResult result = classifyWithConfidence(mapBytes);
        return result != null ? result.captchaClass() : CaptchaClass.UNKNOWN;
    }

    /**
     * Распознает класс капчи с уровнем уверенности напрямую из массива байтов карты.
     */
    public static ClassificationResult classifyWithConfidence(byte[] mapBytes) {
        if (mapBytes == null || mapBytes.length < 128 * 128) {
            return null;
        }

        if (!init()) {
            return null;
        }

        try {
            float[] tensorData = new float[3 * 128 * 128];

            for (int i = 0; i < 128 * 128; i++) {
                int colorIndex = mapBytes[i] & 0xFF;
                int argb = net.minecraft.block.MapColor.getRenderColor(colorIndex);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                // Normalization: (color/255.0 - mean) / std
                tensorData[i] = ((r / 255.0f) - MEAN[0]) / STD[0];                   // R channel
                tensorData[16384 + i] = ((g / 255.0f) - MEAN[1]) / STD[1];           // G channel
                tensorData[32768 + i] = ((b / 255.0f) - MEAN[2]) / STD[2];           // B channel
            }

            long[] shape = new long[]{1, 3, 128, 128};
            FloatBuffer buffer = FloatBuffer.wrap(tensorData);

            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
                 OrtSession.Result output = session.run(Collections.singletonMap("input", inputTensor))) {

                float[][] scores = (float[][]) output.get(0).getValue();
                float[] logits = scores[0]; // [chests, pigs, zombies]

                // Softmax
                float maxLogit = Math.max(logits[0], Math.max(logits[1], logits[2]));
                float sum = 0.0f;
                float[] probs = new float[3];
                for (int c = 0; c < 3; c++) {
                    probs[c] = (float) Math.exp(logits[c] - maxLogit);
                    sum += probs[c];
                }
                for (int c = 0; c < 3; c++) {
                    probs[c] /= sum;
                }

                int maxIdx = 0;
                for (int c = 1; c < 3; c++) {
                    if (probs[c] > probs[maxIdx]) {
                        maxIdx = c;
                    }
                }

                CaptchaClass detected = switch (maxIdx) {
                    case 0 -> CaptchaClass.CHESTS;
                    case 1 -> CaptchaClass.PIGS;
                    case 2 -> CaptchaClass.ZOMBIES;
                    default -> CaptchaClass.UNKNOWN;
                };

                return new ClassificationResult(detected, probs[maxIdx], logits);
            }
        } catch (Exception e) {
            System.err.println("[CaptchaClassifier] Inference error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
