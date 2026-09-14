package despairscent.skyblockm.tweaks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
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
