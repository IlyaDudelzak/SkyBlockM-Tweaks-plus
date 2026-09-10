package despairscent.skyblockm.tweaks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class ModelBoundsCache {
    public static Box getBounds(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return new Box(0,0,0,0,0,0);
        
        try {
            ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
            BakedModel model = itemRenderer.getModel(stack, null, null, 0);
            
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
                for (BakedQuad quad : quads) {
                    int[] vertexData = quad.getVertexData();
                    // Each vertex is typically 8 ints (32 bytes). X,Y,Z are the first 3 floats.
                    // So indices 0, 8, 16, 24 are X. 1, 9, 17, 25 are Y. 2, 10, 18, 26 are Z.
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
            
            if (found) {
                // Item models are usually defined in 0-16 voxel space, where 1 unit = 1/16th of a block.
                // Actually, the floats are usually 0.0 to 1.0!
                // But wait, the display transformation centers it?
                // Typically a standard item display centers the model at 0.5, 0.5, 0.5.
                // Let's just return the raw box and offset it in HitboxUtils.
                return new Box(minX, minY, minZ, maxX, maxY, maxZ);
            }
        } catch (Exception e) {
            // fallback
        }
        
        return new Box(0, 0, 0, 1, 1, 1);
    }
}
