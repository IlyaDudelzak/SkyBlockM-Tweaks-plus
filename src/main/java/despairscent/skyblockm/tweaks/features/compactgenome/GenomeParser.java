package despairscent.skyblockm.tweaks.features.compactgenome;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

record GenomeParser<T>(GenomeType<T> type, Function<String, T> valueParser) {

    private final static Map<String, GenomeParser<?>> typeParsers = new HashMap<>();

    static void register(String typeStr, GenomeParser<?> typeParser) {
        typeParsers.put(typeStr, typeParser);
    }

    static GenomeParser<?> get(String typeStr) {
        return typeParsers.get(typeStr);
    }
    
    static {
        GenomeParser<Genomes.Temperature> tempParser = new GenomeParser<>(Genomes.TEMPERATURE, s -> switch (s) {
            case "Лес, луг, болото", "Ліс, луг, болото", "Forest, plains, swamp", "Forest, Plains, Swamp" -> Genomes.Temperature.NORMAL;
            case "Джунгли, саванна", "Джунглі, савана", "Jungle, savanna", "Jungle, Savanna" -> Genomes.Temperature.WARM;
            case "Тайга", "Taiga" -> Genomes.Temperature.COLD;
            case "Адские биомы", "Пекельні біоми", "Nether biomes", "Nether Biomes" -> Genomes.Temperature.HELLISH;
            default -> null;
        });
        register("Подходящая температура", tempParser);
        register("Підходяща температура", tempParser);
        register("Відповідна температура", tempParser);
        register("Suitable temperature", tempParser);
        register("Temperature", tempParser);

        GenomeParser<Genomes.Humidity> humidityParser = new GenomeParser<>(Genomes.HUMIDITY, s -> switch (s) {
            case "Лес, луг, тайга", "Ліс, луг, тайга", "Forest, plains, taiga", "Forest, Plains, Taiga" -> Genomes.Humidity.NORMAL;
            case "Джунгли, болото", "Джунглі, болото", "Jungle, swamp", "Jungle, Swamp" -> Genomes.Humidity.ARID;
            case "Пустыня, меза, саванна, ад", "Пустеля, меза, савана, пекло", "Desert, mesa, savanna, nether", "Desert, Mesa, Savanna, Nether" -> Genomes.Humidity.DAMP;
            default -> null;
        });
        register("Подходящая влажность", humidityParser);
        register("Підходяща вологість", humidityParser);
        register("Відповідна вологість", humidityParser);
        register("Suitable humidity", humidityParser);
        register("Humidity", humidityParser);

        GenomeParser<Genomes.Flowers> flowersParser = new GenomeParser<>(Genomes.FLOWERS, s -> switch (s) {
            case "Обычные цветы", "Звичайні квіти", "Normal flowers", "Vanilla flowers", "Flowers" -> Genomes.Flowers.FLOWERS;
            case "Лианы, какао-бобы, папоротники", "Ліани, какао-боби, папороті", "Vines, cocoa beans, ferns" -> Genomes.Flowers.JUNGLE;
            case "Спороцвет, бросянки", "Спороквіт, росички", "Spore blossom, droseras", "Cave" -> Genomes.Flowers.CAVE;
            case "Адские грибы", "Пекельні гриби", "Nether mushrooms" -> Genomes.Flowers.NETHER;
            default -> null;
        });
        register("Подходящие цветы", flowersParser);
        register("Підходящі квіти", flowersParser);
        register("Відповідні квіти", flowersParser);
        register("Suitable flowers", flowersParser);
        register("Flowers", flowersParser);

        GenomeParser<Genomes.Speed> speedParser = new GenomeParser<>(Genomes.SPEED, s -> switch (s) {
            case "Самая медленная", "Найповільніша", "Slowest" -> Genomes.Speed.SLOWEST;
            case "Более медленная", "Повільніша", "Slower" -> Genomes.Speed.SLOWER;
            case "Медленная", "Повільна", "Slow" -> Genomes.Speed.SLOW;
            case "Нормальная", "Нормальна", "Normal" -> Genomes.Speed.NORMAL;
            case "Быстрая", "Швидка", "Fast" -> Genomes.Speed.FAST;
            case "Более быстрая", "Швидша", "Faster" -> Genomes.Speed.FASTER;
            case "Наибыстрейшая", "Найшвидша", "Fastest" -> Genomes.Speed.FASTEST;
            default -> null;
        });
        register("Скорость работы", speedParser);
        register("Швидкість роботи", speedParser);
        register("Production speed", speedParser);
        register("Work speed", speedParser);
        register("Speed", speedParser);

        GenomeParser<Genomes.Lifespan> lifespanParser = new GenomeParser<>(Genomes.LIFESPAN, s -> switch (s) {
            case "Наикратчайшее", "Найкоротше", "Shortest" -> Genomes.Lifespan.SHORTEST;
            case "Более короткое", "Коротше", "Shorter" -> Genomes.Lifespan.SHORTER;
            case "Короткое", "Коротке", "Short" -> Genomes.Lifespan.SHORT;
            case "Нормальное", "Нормальне", "Normal" -> Genomes.Lifespan.NORMAL;
            case "Длинное", "Довге", "Long" -> Genomes.Lifespan.LONG;
            case "Более длинное", "Довше", "Longer" -> Genomes.Lifespan.LONGER;
            case "Самое длинное", "Найдовше", "Longest" -> Genomes.Lifespan.LONGEST;
            default -> null;
        });
        register("Время жизни", lifespanParser);
        register("Час життя", lifespanParser);
        register("Тривалість життя", lifespanParser);
        register("Lifespan", lifespanParser);
        register("Life span", lifespanParser);

        GenomeParser<Genomes.Fertility> fertilityParser = new GenomeParser<>(Genomes.FERTILITY, s -> switch (s) {
            case "1 трутень", "1 drone", "1 Drone" -> Genomes.Fertility.ONE;
            case "2 трутня", "2 трутні", "2 drones", "2 Drones" -> Genomes.Fertility.TWO;
            case "3 трутня", "3 трутні", "3 drones", "3 Drones" -> Genomes.Fertility.THREE;
            default -> null;
        });
        register("Плодовитость", fertilityParser);
        register("Плодючість", fertilityParser);
        register("Плодовитість", fertilityParser);
        register("Родючість", fertilityParser);
        register("Fertility", fertilityParser);

        GenomeParser<Genomes.Nocturnal> nocturnalParser = new GenomeParser<>(Genomes.NOCTURNAL, s -> switch (s) {
            case "Нет", "Ні", "No", "False" -> Genomes.Nocturnal.NO;
            case "Да", "Так", "Yes", "True" -> Genomes.Nocturnal.YES;
            default -> null;
        });
        register("Активность в ночное время", nocturnalParser);
        register("Активність у нічний час", nocturnalParser);
        register("Активність в нічний час", nocturnalParser);
        register("Активність вночі", nocturnalParser);
        register("Nocturnal activity", nocturnalParser);
        register("Nocturnal", nocturnalParser);
        register("Activity at night", nocturnalParser);

        GenomeParser<Genomes.Flyer> flyerParser = new GenomeParser<>(Genomes.FLYER, s -> switch (s) {
            case "Нет", "Ні", "No", "False" -> Genomes.Flyer.NO;
            case "Да", "Так", "Yes", "True" -> Genomes.Flyer.YES;
            default -> null;
        });
        register("Активность во время дождя", flyerParser);
        register("Активність під час дощу", flyerParser);
        register("Активність під час дощів", flyerParser);
        register("Rain activity", flyerParser);
        register("Activity during rain", flyerParser);
        register("Tolerates rain", flyerParser);
        register("Flyer", flyerParser);

        GenomeParser<Genomes.Effect> effectParser = new GenomeParser<>(Genomes.EFFECT, s -> switch (s) {
            case "Нет", "Ні", "None", "No" -> Genomes.Effect.NONE;
            case "Отравляет сущностей рядом", "Отруює істот поруч", "Отруює сутностей поруч", "Poisons nearby entities" -> Genomes.Effect.POISON;
            case "Регенерирует игроков рядом", "Регенерує гравців поруч", "Regenerates nearby players" -> Genomes.Effect.REGENERATION;
            case "Даёт опыт игрокам рядом", "Дає досвід гравцям поруч", "Gives experience to nearby players" -> Genomes.Effect.EXPERIENCE;
            case "Превращает воду рядом в лёд", "Перетворює воду поруч на лід", "Freezes nearby water" -> Genomes.Effect.FREEZE;
            case "Поджигает сущностей рядом", "Підпалює істот поруч", "Підпалює сутностей поруч", "Ignites nearby entities" -> Genomes.Effect.FLAME;
            default -> null;
        });
        register("Эффект", effectParser);
        register("Ефект", effectParser);
        register("Effect", effectParser);
    }

}
