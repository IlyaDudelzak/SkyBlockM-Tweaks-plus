package despairscent.skyblockm.tweaks.mixininner;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;

public interface ILayerRenderStateAccessor {
    Transformation skyblockm$getTransform();
    RenderLayer skyblockm$getRenderLayer();
    int[] skyblockm$getTints();
    SpecialModelRenderer<?> skyblockm$getSpecialModelType();
}
