package despairscent.skyblockm.tweaks.features.compactgenome;

import net.minecraft.text.Text;
import despairscent.skyblockm.tweaks.ModUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class GenomeCompacter {

    private static final Map<GenomeType<?>, String> compactTypes = new HashMap<>();

    private static final Map<GenomeType<?>, Map<Object, Object>> compactValues = new HashMap<>();
    
    static void register(GenomeType<?> type, String keyOrLiteral) {
        compactTypes.put(type, keyOrLiteral);
    }
    
    static <G> void register(GenomeType<G> type, G value, Object keyOrLiteral) {
        compactValues.computeIfAbsent(type, t -> new HashMap<>()).put(value, keyOrLiteral);
    }

    static Text get(GenomeType<?> type) {
        String val = compactTypes.get(type);
        if (val != null) {
            if (val.startsWith("compactGenome.")) {
                return ModUtils.i18n(val);
            }
            return Text.literal(val);
        }
        return Text.literal(String.valueOf(type));
    }

    static Text get(GenomeType<?> type, Object value) {
        Object val = compactValues.getOrDefault(type, Collections.emptyMap()).get(value);
        if (val instanceof String str) {
            if (str.startsWith("compactGenome.")) {
                return ModUtils.i18n(str);
            }
            return Text.literal(str);
        }
        return Text.literal(String.valueOf(value));
    }

    static {
        register(Genomes.TEMPERATURE, "compactGenome.type.temperature");
        register(Genomes.HUMIDITY, "compactGenome.type.humidity");
        register(Genomes.FLOWERS, "compactGenome.type.flowers");
        register(Genomes.SPEED, "compactGenome.type.speed");
        register(Genomes.LIFESPAN, "compactGenome.type.lifespan");
        register(Genomes.FERTILITY, "compactGenome.type.fertility");
        register(Genomes.NOCTURNAL, "compactGenome.type.nocturnal");
        register(Genomes.FLYER, "compactGenome.type.flyer");
        register(Genomes.EFFECT, "compactGenome.type.effect");

        register(Genomes.TEMPERATURE, Genomes.Temperature.NORMAL, "compactGenome.value.temperature.normal");
        register(Genomes.TEMPERATURE, Genomes.Temperature.WARM, "compactGenome.value.temperature.warm");
        register(Genomes.TEMPERATURE, Genomes.Temperature.COLD, "compactGenome.value.temperature.cold");
        register(Genomes.TEMPERATURE, Genomes.Temperature.HELLISH, "compactGenome.value.temperature.hellish");

        register(Genomes.HUMIDITY, Genomes.Humidity.NORMAL, "compactGenome.value.humidity.normal");
        register(Genomes.HUMIDITY, Genomes.Humidity.DAMP, "compactGenome.value.humidity.damp");
        register(Genomes.HUMIDITY, Genomes.Humidity.ARID, "compactGenome.value.humidity.arid");

        register(Genomes.FLOWERS, Genomes.Flowers.FLOWERS, "compactGenome.value.flowers.flowers");
        register(Genomes.FLOWERS, Genomes.Flowers.CAVE, "compactGenome.value.flowers.cave");
        register(Genomes.FLOWERS, Genomes.Flowers.JUNGLE, "compactGenome.value.flowers.jungle");
        register(Genomes.FLOWERS, Genomes.Flowers.NETHER, "compactGenome.value.flowers.nether");

        register(Genomes.SPEED, Genomes.Speed.SLOWEST, "1");
        register(Genomes.SPEED, Genomes.Speed.SLOWER, "2");
        register(Genomes.SPEED, Genomes.Speed.SLOW, "3");
        register(Genomes.SPEED, Genomes.Speed.NORMAL, "4");
        register(Genomes.SPEED, Genomes.Speed.FAST, "5");
        register(Genomes.SPEED, Genomes.Speed.FASTER, "6");
        register(Genomes.SPEED, Genomes.Speed.FASTEST, "7");

        register(Genomes.LIFESPAN, Genomes.Lifespan.SHORTEST, "1");
        register(Genomes.LIFESPAN, Genomes.Lifespan.SHORTER, "2");
        register(Genomes.LIFESPAN, Genomes.Lifespan.SHORT, "3");
        register(Genomes.LIFESPAN, Genomes.Lifespan.NORMAL, "4");
        register(Genomes.LIFESPAN, Genomes.Lifespan.LONG, "5");
        register(Genomes.LIFESPAN, Genomes.Lifespan.LONGER, "6");
        register(Genomes.LIFESPAN, Genomes.Lifespan.LONGEST, "7");

        register(Genomes.FERTILITY, Genomes.Fertility.ONE, "1");
        register(Genomes.FERTILITY, Genomes.Fertility.TWO, "2");
        register(Genomes.FERTILITY, Genomes.Fertility.THREE, "3");

        register(Genomes.NOCTURNAL, Genomes.Nocturnal.NO, "-");
        register(Genomes.NOCTURNAL, Genomes.Nocturnal.YES, "+");

        register(Genomes.FLYER, Genomes.Flyer.NO, "-");
        register(Genomes.FLYER, Genomes.Flyer.YES, "+");

        register(Genomes.EFFECT, Genomes.Effect.NONE, "compactGenome.value.effect.none");
        register(Genomes.EFFECT, Genomes.Effect.POISON, "compactGenome.value.effect.poison");
        register(Genomes.EFFECT, Genomes.Effect.REGENERATION, "compactGenome.value.effect.regeneration");
        register(Genomes.EFFECT, Genomes.Effect.EXPERIENCE, "compactGenome.value.effect.experience");
        register(Genomes.EFFECT, Genomes.Effect.FREEZE, "compactGenome.value.effect.freeze");
        register(Genomes.EFFECT, Genomes.Effect.FLAME, "compactGenome.value.effect.flame");
    }

}
