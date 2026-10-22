package despairscent.skyblockm.tweaks.features.itemdisplay;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;

import java.util.concurrent.ConcurrentHashMap;

public class SpriteTranslucencyCache {
    private static final ConcurrentHashMap<Identifier, Boolean> CACHE = new ConcurrentHashMap<>();

    public static boolean isTranslucent(Sprite sprite) {
        if (sprite == null) return false;
        SpriteContents contents = sprite.getContents();
        if (contents == null) return false;
        Identifier id = contents.getId();
        if (id == null) return false;

        return CACHE.computeIfAbsent(id, k -> {
            String path = k.getPath().toLowerCase();
            return path.contains("translucent") || path.contains("overlay") || path.contains("glass") 
                    || path.contains("completed") || path.contains("water") || path.contains("ice");
        });
    }

    public static void clear() {
        CACHE.clear();
    }
}
