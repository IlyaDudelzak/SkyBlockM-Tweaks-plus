package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;

public class BarrierModelPlugin implements ModelLoadingPlugin {
    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (context.id() != null && context.id().getNamespace().equals("minecraft") && (context.id().getPath().equals("barrier") || context.id().getPath().equals("block/barrier"))) {
                return new BarrierBakedModel(model);
            }
            return model;
        });
    }
}
