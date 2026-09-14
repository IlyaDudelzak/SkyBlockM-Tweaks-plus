package despairscent.skyblockm.tweaks.mixininner;

import net.minecraft.client.render.item.ItemRenderState;

public interface IItemRenderStateAccessor {
    int skyblockm$getLayerCount();
    ItemRenderState.LayerRenderState[] skyblockm$getLayers();
}
