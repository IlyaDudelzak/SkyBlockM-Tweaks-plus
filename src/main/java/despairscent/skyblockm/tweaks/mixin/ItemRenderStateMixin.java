package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.mixininner.IItemRenderStateAccessor;
import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemRenderState.class)
public class ItemRenderStateMixin implements IItemRenderStateAccessor {
    @Shadow
    private int layerCount;

    @Shadow
    private ItemRenderState.LayerRenderState[] layers;

    @Override
    public int skyblockm$getLayerCount() {
        return this.layerCount;
    }

    @Override
    public ItemRenderState.LayerRenderState[] skyblockm$getLayers() {
        return this.layers;
    }
}
