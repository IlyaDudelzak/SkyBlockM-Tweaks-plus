package despairscent.skyblockm.tweaks.features.itemdisplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Optional;
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
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                try {
                    //? if >=1.21 {
                    Identifier texturePath = Identifier.of(k.getNamespace(), "textures/" + k.getPath() + ".png");
                    //?} else {
                    /*Identifier texturePath = new Identifier(k.getNamespace(), "textures/" + k.getPath() + ".png");
                    *///?}
                    Optional<Resource> res = client.getResourceManager().getResource(texturePath);
                    if (res.isPresent()) {
                        try (InputStream in = res.get().getInputStream();
                             NativeImage img = NativeImage.read(in)) {
                            int w = img.getWidth();
                            int h = img.getHeight();
                            for (int y = 0; y < h; y++) {
                                for (int x = 0; x < w; x++) {
                                    int a = Byte.toUnsignedInt(img.getOpacity(x, y));
                                    if (a > 0 && a < 255) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            String path = k.getPath().toLowerCase();
            return path.contains("translucent") || path.contains("overlay") || path.contains("glass") || path.contains("completed");
        });
    }

    public static void clear() {
        CACHE.clear();
    }
}
