package despairscent.skyblockm.tweaks.features.itemdisplay.mixininner;

//? if >=1.21.8 {
/*import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;

public interface ILayerRenderStateAccessor {
    Transformation skyblockm$getTransform();
    RenderLayer skyblockm$getRenderLayer();
    int[] skyblockm$getTints();
    SpecialModelRenderer<?> skyblockm$getSpecialModelType();
}
*///?} else {
public interface ILayerRenderStateAccessor {
}
//?}
