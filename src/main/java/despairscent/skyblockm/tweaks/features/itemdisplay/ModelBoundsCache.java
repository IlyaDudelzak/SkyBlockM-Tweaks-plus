package despairscent.skyblockm.tweaks.features.itemdisplay;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
//? if >=1.21.8 {
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public class ModelBoundsCache {
    public static void clear() {
    }

    public static Box getBounds(ItemStack stack) {
        return getBounds(stack, ItemDisplayContext.NONE);
    }

    public static Box getBounds(ItemStack stack, ItemDisplayContext context) {
        if (stack == null || stack.isEmpty()) return new Box(0, 0, 0, 0, 0, 0);
        
        try {
            ItemRenderState state = new ItemRenderState();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getItemModelManager() != null) {
                client.getItemModelManager().updateForNonLivingEntity(state, stack, context != null ? context : ItemDisplayContext.NONE, null);
                Box box = state.getModelBoundingBox();
                if (box != null) {
                    return box;
                }
            }
        } catch (Exception e) {
            // fallback
        }
        
        return new Box(0, 0, 0, 1, 1, 1);
    }
}
//?} else {
/*import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class ModelBoundsCache {
    private static final Map<BakedModel, Box> CACHE = new ConcurrentHashMap<>();

    public static void clear() {
        CACHE.clear();
    }

    public static Box getBounds(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new Box(0, 0, 0, 0, 0, 0);
        
        try {
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
            BakedModel model = itemRenderer.getModel(stack, null, null, 0);
            if (model == null) return new Box(0, 0, 0, 1, 1, 1);
            
            return CACHE.computeIfAbsent(model, ModelBoundsCache::computeBounds);
        } catch (Exception e) {
            // fallback
        }
        
        return new Box(0, 0, 0, 1, 1, 1);
    }

    private static Box computeBounds(BakedModel model) {
        try {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            float maxZ = -Float.MAX_VALUE;
            
            Random random = Random.create();
            boolean found = false;
            
            Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : dirs) {
                List<BakedQuad> quads = model.getQuads(null, dir, random);
                if (quads == null) continue;
                for (BakedQuad quad : quads) {
                    int[] vertexData = quad.getVertexData();
                    if (vertexData == null || vertexData.length < 32) continue;
                    for (int i = 0; i < 4; i++) {
                        float x = Float.intBitsToFloat(vertexData[i * 8 + 0]);
                        float y = Float.intBitsToFloat(vertexData[i * 8 + 1]);
                        float z = Float.intBitsToFloat(vertexData[i * 8 + 2]);
                        
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                        found = true;
                    }
                }
            }
            
            if (found && minX <= maxX && minY <= maxY && minZ <= maxZ) {
                return new Box(minX, minY, minZ, maxX, maxY, maxZ);
            }
        } catch (Exception ignored) {}
        
        return new Box(0, 0, 0, 1, 1, 1);
    }
}
*///?}
