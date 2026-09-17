package despairscent.skyblockm.tweaks.features.splashes;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SplashManager {
    private static final Identifier SPLASHES_ID = Identifier.tryParse("skyblockm-tweaks:texts/splashes.txt");
    private static final List<String> SPLASHES = new ArrayList<>();

    private static final List<String> DEFAULT_SPLASHES = List.of(
            "SkyBlockM Tweaks+ активирован!",
            "Пчёлы не спят!",
            "Где мой автокрафт?!",
            "Снова десинхрон инвентаря...",
            "Хранится: 1.2M камня",
            "Нажмите ЛКМ, чтобы создать предмет",
            "Нажмите Shift+ЛКМ, чтобы посмотреть где предмет создается",
            "413K булыжника в сети!",
            "Item Display запечён!",
            "Опять реклама в чате...",
            "Заблокировано блокировщиком рекламы!",
            "Пчеловод 100 уровня",
            "Чистый заголовок терминала!",
            "Гены пчёл в порядке",
            "PojavLauncher одобряет!",
            "Слайдер на месте!",
            "1500 МБ хватит каждому (нет)",
            "Больше FPS богу FPS!",
            "Куплю матку пчелы дорого!",
            "Кому нужен термальный центробежный экстрактор?",
            "Интерфейсы настроены!",
            "Не забудь скрафтить шаблоны!",
            "Где кабель?!",
            "Автокрафт запущен...",
            "Блоков в сети больше, чем звёзд на небе",
            "Компактный геном для настоящих селекционеров",
            "Шрифт терминала уменьшен в 1.7 раза!",
            "Плюс вместо количества!",
            "Всё стабильно, сервер не упал!",
            "Заходи на JustMC!"
    );

    static {
        SPLASHES.addAll(DEFAULT_SPLASHES);
    }

    public static void loadSplashes(ResourceManager manager) {
        List<String> loaded = new ArrayList<>();
        if (manager != null && SPLASHES_ID != null) {
            try {
                Optional<Resource> resource = manager.getResource(SPLASHES_ID);
                if (resource.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                loaded.add(line);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback to reading directly from classpath if manager didn't load any
        if (loaded.isEmpty()) {
            try (InputStream in = SplashManager.class.getResourceAsStream("/assets/skyblockm-tweaks/texts/splashes.txt")) {
                if (in != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                loaded.add(line);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        synchronized (SPLASHES) {
            SPLASHES.clear();
            if (!loaded.isEmpty()) {
                SPLASHES.addAll(loaded);
            } else {
                SPLASHES.addAll(DEFAULT_SPLASHES);
            }
        }
    }

    public static List<String> getSplashes() {
        synchronized (SPLASHES) {
            return new ArrayList<>(SPLASHES);
        }
    }

    public static boolean hasSplashes() {
        synchronized (SPLASHES) {
            return !SPLASHES.isEmpty();
        }
    }

    public static String getRandomSplash() {
        synchronized (SPLASHES) {
            if (SPLASHES.isEmpty()) return null;
            return SPLASHES.get(ThreadLocalRandom.current().nextInt(SPLASHES.size()));
        }
    }
}
