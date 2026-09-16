package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.Collections;
import java.util.List;

public class CaptchaDetector {

    private static boolean checking = false;
    private static int checkTicksRemaining = 0;
    private static final int MAX_CHECK_TICKS = 120; // 6 секунд ожидания загрузки сущностей
    private static boolean captchaRoomActive = false;
    private static boolean captchaGridLogged = false;
    private static boolean captchaSolved = false;
    private static boolean solvingInProgress = false;

    private static String lastActionBarText = "";
    private static String lastTitleText = "";
    private static String lastSubtitleText = "";
    private static CaptchaClassifier.CaptchaClass targetCaptchaClass = CaptchaClassifier.CaptchaClass.UNKNOWN;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            startChecking();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            reset();
        });
    }

    public static void startChecking() {
        checking = true;
        checkTicksRemaining = MAX_CHECK_TICKS;
        captchaRoomActive = false;
        captchaGridLogged = false;
        captchaSolved = false;
        solvingInProgress = false;
        lastActionBarText = "";
        lastTitleText = "";
        lastSubtitleText = "";
        targetCaptchaClass = CaptchaClassifier.CaptchaClass.UNKNOWN;
        SmoothTargetBot.reset();
    }

    public static void reset() {
        checking = false;
        checkTicksRemaining = 0;
        captchaRoomActive = false;
        captchaGridLogged = false;
        captchaSolved = false;
        solvingInProgress = false;
        lastActionBarText = "";
        lastTitleText = "";
        lastSubtitleText = "";
        targetCaptchaClass = CaptchaClassifier.CaptchaClass.UNKNOWN;
        SmoothTargetBot.reset();
    }

    public static boolean isCaptchaRoomActive() {
        return captchaRoomActive;
    }

    public static String getLastActionBarText() {
        return lastActionBarText;
    }

    public static String getLastTitleText() {
        return lastTitleText;
    }

    public static String getLastSubtitleText() {
        return lastSubtitleText;
    }

    public static CaptchaClassifier.CaptchaClass getTargetCaptchaClass() {
        return targetCaptchaClass;
    }

    public static void onActionBarMessage(String text) {
        if (text == null) return;
        lastActionBarText = text;
        ModUtils.LOGGER.info("[CaptchaDetector] Action Bar: " + text);
        updateTargetClass(text);
    }

    public static void onTitleMessage(String text) {
        if (text == null) return;
        lastTitleText = text;
        ModUtils.LOGGER.info("[CaptchaDetector] Title: " + text);
        updateTargetClass(text);
    }

    public static void onSubtitleMessage(String text) {
        if (text == null) return;
        lastSubtitleText = text;
        ModUtils.LOGGER.info("[CaptchaDetector] Subtitle: " + text);
        updateTargetClass(text);
    }

    private static void updateTargetClass(String text) {
        CaptchaClassifier.CaptchaClass detected = parseTargetClass(text);
        if (detected != CaptchaClassifier.CaptchaClass.UNKNOWN) {
            targetCaptchaClass = detected;
            ModUtils.LOGGER.info("[CaptchaDetector] Определена цель капчи из текста: " + detected.getDisplayName());
        }
    }

    public static CaptchaClassifier.CaptchaClass parseTargetClass(String text) {
        if (text == null) return CaptchaClassifier.CaptchaClass.UNKNOWN;
        String upper = text.toUpperCase();
        if (upper.contains("СВИН") || upper.contains("PIG")) {  
            return CaptchaClassifier.CaptchaClass.PIGS;
        }
        if (upper.contains("СУНДУК") || upper.contains("СКРИ") || upper.contains("CHEST")) {
            return CaptchaClassifier.CaptchaClass.CHESTS;
        }
        if (upper.contains("ЗОМБ") || upper.contains("ZOMBIE")) {
            return CaptchaClassifier.CaptchaClass.ZOMBIES;
        }
        return CaptchaClassifier.CaptchaClass.UNKNOWN;
    }

    public static boolean isSolvingInProgress() {
        return solvingInProgress || SmoothTargetBot.isBusy();
    }

    public static boolean shouldBlockPlayerInput() {
        return ModUtils.CONFIG != null 
            && ModUtils.CONFIG.automaticCaptcha.enabled 
            && ModUtils.CONFIG.automaticCaptcha.disablePlayerInput 
            && isSolvingInProgress();
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        if (ModUtils.CONFIG != null && !ModUtils.CONFIG.automaticCaptcha.enabled) {
            return;
        }

        if (!DependencyDownloader.isReady()) {
            return; // Библиотеки еще не скачаны, пропускаем
        }

        // Проверяем наличие комнаты капчи каждые 5 тиков
        if (client.player.age % 5 == 0) {
            boolean inRoom = isCaptchaRoom(client.player, client.world);
            if (inRoom && !captchaRoomActive) {
                captchaRoomActive = true;
                captchaSolved = false;
                captchaGridLogged = false;
                solvingInProgress = false;
                onCaptchaRoomDetected(client.player, client.world);
            } else if (!inRoom && captchaRoomActive) {
                captchaRoomActive = false;
                captchaSolved = false;
                captchaGridLogged = false;
                solvingInProgress = false;
                targetCaptchaClass = CaptchaClassifier.CaptchaClass.UNKNOWN;
            }
        }

        // Если цель капчи еще не определена из Action Bar/Title, пробуем прочитать ее из Armor Stand голограмм
        if (captchaRoomActive && targetCaptchaClass == CaptchaClassifier.CaptchaClass.UNKNOWN) {
            CaptchaClassifier.CaptchaClass standTarget = getTargetClassFromArmorStands(client.player, client.world);
            if (standTarget != CaptchaClassifier.CaptchaClass.UNKNOWN) {
                targetCaptchaClass = standTarget;
                ModUtils.LOGGER.info("[CaptchaDetector] Определена цель капчи из стойки брони: " + standTarget.getDisplayName());
            }
        }

        // Если капча активна и еще не решена
        if (captchaRoomActive && !captchaSolved && !solvingInProgress) {
            ItemFrameEntity[][] framesGrid = getSortedFramesGrid(client.player, client.world);
            CaptchaClassifier.CaptchaClass[][] grid = getCaptchaGrid(client.player, client.world);

            if (grid != null && framesGrid != null) {
                if (!captchaGridLogged) {
                    String formatted = formatCaptchaGrid(grid);
                    System.out.println(formatted);
                    ModUtils.LOGGER.info("[CaptchaDetector] Распознанная сетка капчи:\n" + formatted);
                    captchaGridLogged = true;
                }

                // Если цель известна — запускаем авто-решение
                if (targetCaptchaClass != CaptchaClassifier.CaptchaClass.UNKNOWN) {
                    List<ItemFrameEntity> targetFrames = new java.util.ArrayList<>();
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            if (grid[r][c] == targetCaptchaClass) {
                                targetFrames.add(framesGrid[r][c]);
                            }
                        }
                    }

                    if (targetFrames.size() == 3) {
                        solvingInProgress = true;
                        ModUtils.LOGGER.info(String.format("[CaptchaDetector] Запуск решения капчи: кликаем по 3 рамкам [%s] с плавной доводкой", targetCaptchaClass.getDisplayName()));
                        SmoothTargetBot.queueTargets(targetFrames, 0.20f, 12, () -> {
                            ModUtils.LOGGER.info("[CaptchaDetector] Все 3 рамки успешно нажаты!");
                            captchaSolved = true;
                            solvingInProgress = false;
                        });
                    }
                }
            }
        }
    }

    private static double getHorizontalScore(ItemFrameEntity frame) {
        net.minecraft.util.math.Direction facing = frame.getHorizontalFacing();
        return -facing.getOffsetX() * frame.getZ() + facing.getOffsetZ() * frame.getX();
    }

    /**
     * Возвращает сетку рамок 3x3:
     * grid[0] = верхний ряд (слева направо)
     * grid[1] = средний ряд (слева направо)
     * grid[2] = нижний ряд (слева направо)
     */
    public static ItemFrameEntity[][] getSortedFramesGrid(ClientPlayerEntity player, ClientWorld world) {
        List<ItemFrameEntity> frames = new java.util.ArrayList<>(getFrames(player, world));
        if (frames.size() != 9) return null;

        // Сортируем все 9 рамок по Y сверху вниз (больший Y сначала)
        frames.sort((a, b) -> Double.compare(b.getY(), a.getY()));

        ItemFrameEntity[][] grid = new ItemFrameEntity[3][3];

        for (int row = 0; row < 3; row++) {
            List<ItemFrameEntity> rowFrames = new java.util.ArrayList<>(frames.subList(row * 3, (row + 1) * 3));
            // Сортируем рамки в ряду слева направо с точки зрения игрока
            rowFrames.sort((a, b) -> Double.compare(getHorizontalScore(a), getHorizontalScore(b)));
            for (int col = 0; col < 3; col++) {
                grid[row][col] = rowFrames.get(col);
            }
        }

        return grid;
    }

    private static final int[][] PERMUTATIONS_3_3_3;

    static {
        List<int[]> list = new java.util.ArrayList<>();
        int[] arr = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        generatePermutations(arr, 0, list);
        PERMUTATIONS_3_3_3 = list.toArray(new int[0][]);
    }

    private static void generatePermutations(int[] arr, int index, List<int[]> list) {
        if (index == arr.length) {
            list.add(arr.clone());
            return;
        }
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = index; i < arr.length; i++) {
            if (seen.add(arr[i])) {
                swap(arr, index, i);
                generatePermutations(arr, index + 1, list);
                swap(arr, index, i);
            }
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * Классифицирует все 9 карт в сетке 3x3 с гарантированным распределением (ровно 3 C, 3 P, 3 Z).
     * Возвращает null, если хотя бы одна карта еще не загрузилась или рамки не найдены.
     */
    public static CaptchaClassifier.CaptchaClass[][] getCaptchaGrid(ClientPlayerEntity player, ClientWorld world) {
        ItemFrameEntity[][] framesGrid = getSortedFramesGrid(player, world);
        if (framesGrid == null) return null;

        float[][] allScores = new float[9][];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                CaptchaClassifier.ClassificationResult res = CaptchaClassifier.classifyWithConfidence(framesGrid[r][c], world);
                if (res == null || res.rawScores() == null) {
                    return null; // Карта еще не прогрузилась
                }
                allScores[r * 3 + c] = res.rawScores();
                ModUtils.LOGGER.info(String.format("[CaptchaDetector] Рамка [%d,%d]: предсказано=%s (conf=%.2f, raw=[C:%.2f, P:%.2f, Z:%.2f])",
                        r, c, res.captchaClass().getDisplayName(), res.confidence(),
                        res.rawScores()[0], res.rawScores()[1], res.rawScores()[2]));
            }
        }

        // Поиск глобально оптимального распределения: ровно 3 Сундука, 3 Свиньи, 3 Зомби
        float bestScore = -Float.MAX_VALUE;
        int[] bestPerm = null;
        for (int[] perm : PERMUTATIONS_3_3_3) {
            float total = 0f;
            for (int i = 0; i < 9; i++) {
                total += allScores[i][perm[i]];
            }
            if (total > bestScore) {
                bestScore = total;
                bestPerm = perm;
            }
        }

        CaptchaClassifier.CaptchaClass[][] grid = new CaptchaClassifier.CaptchaClass[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int classIdx = bestPerm != null ? bestPerm[r * 3 + c] : 0;
                grid[r][c] = switch (classIdx) {
                    case 0 -> CaptchaClassifier.CaptchaClass.CHESTS;
                    case 1 -> CaptchaClassifier.CaptchaClass.PIGS;
                    case 2 -> CaptchaClassifier.CaptchaClass.ZOMBIES;
                    default -> CaptchaClassifier.CaptchaClass.UNKNOWN;
                };
            }
        }
        return grid;
    }

    /**
     * Форматирует сетку в строковый вид PPP\nZZZ\nCCC.
     */
    public static String formatCaptchaGrid(CaptchaClassifier.CaptchaClass[][] grid) {
        if (grid == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                sb.append(grid[r][c] != null ? grid[r][c].getCode() : '?');
            }
            if (r < 2) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Проверяет наличие набора сущностей капчи (9 рамок, 2 стойки брони, 1 слайм) в радиусе 32 блоков.
     */
    public static boolean isCaptchaRoom(ClientPlayerEntity player, ClientWorld world) {
        Box searchBox = player.getBoundingBox().expand(32.0);

        List<ItemFrameEntity> frames = world.getEntitiesByClass(ItemFrameEntity.class, searchBox, e -> true);
        List<ArmorStandEntity> armorStands = world.getEntitiesByClass(ArmorStandEntity.class, searchBox, e -> true);
        List<SlimeEntity> slimes = world.getEntitiesByClass(SlimeEntity.class, searchBox, e -> true);

        return frames.size() == 9 && armorStands.size() == 2 && slimes.size() == 1;
    }

    public static List<ItemFrameEntity> getFrames(ClientPlayerEntity player, ClientWorld world) {
        if (world == null || player == null) return Collections.emptyList();
        return world.getEntitiesByClass(ItemFrameEntity.class, player.getBoundingBox().expand(32.0), e -> true);
    }

    public static List<ArmorStandEntity> getArmorStands(ClientPlayerEntity player, ClientWorld world) {
        if (world == null || player == null) return Collections.emptyList();
        return world.getEntitiesByClass(ArmorStandEntity.class, player.getBoundingBox().expand(32.0), e -> true);
    }

    public static SlimeEntity getSlime(ClientPlayerEntity player, ClientWorld world) {
        if (world == null || player == null) return null;
        List<SlimeEntity> slimes = world.getEntitiesByClass(SlimeEntity.class, player.getBoundingBox().expand(32.0), e -> true);
        return slimes.isEmpty() ? null : slimes.get(0);
    }

    public static CaptchaClassifier.CaptchaClass getTargetClassFromArmorStands(ClientPlayerEntity player, ClientWorld world) {
        for (ArmorStandEntity stand : getArmorStands(player, world)) {
            Text name = stand.getCustomName();
            if (name != null) {
                CaptchaClassifier.CaptchaClass parsed = parseTargetClass(name.getString());
                if (parsed != CaptchaClassifier.CaptchaClass.UNKNOWN) {
                    return parsed;
                }
            }
        }
        return CaptchaClassifier.CaptchaClass.UNKNOWN;
    }

    private static void onCaptchaRoomDetected(ClientPlayerEntity player, ClientWorld world) {
        ModUtils.LOGGER.info("[CaptchaDetector] Обнаружена капча-комната: 9 рамок, 2 стойки для брони, 1 слайм.");
    }
}
