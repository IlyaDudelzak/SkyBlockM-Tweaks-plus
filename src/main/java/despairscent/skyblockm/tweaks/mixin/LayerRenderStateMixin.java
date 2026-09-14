package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.mixininner.ILayerRenderStateAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemRenderState.LayerRenderState.class)
public class LayerRenderStateMixin implements ILayerRenderStateAccessor {
    @Shadow
    Transformation transform;

    @Shadow
    private RenderLayer renderLayer;

    @Shadow
    private int[] tints;

    @Shadow
    private SpecialModelRenderer<Object> specialModelType;

    @Override
    public Transformation skyblockm$getTransform() {
        return this.transform;
    }

    @Override
    public RenderLayer skyblockm$getRenderLayer() {
        return this.renderLayer;
    }

    @Override
    public int[] skyblockm$getTints() {
        return this.tints;
    }

    @Override
    public SpecialModelRenderer<?> skyblockm$getSpecialModelType() {
        return this.specialModelType;
    }
}
