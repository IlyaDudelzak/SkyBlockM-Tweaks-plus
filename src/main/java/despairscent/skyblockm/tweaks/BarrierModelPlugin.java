package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;

public class BarrierModelPlugin implements ModelLoadingPlugin {
    @Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (context.topLevelId() != null && context.topLevelId().id().getNamespace().equals("minecraft") && context.topLevelId().id().getPath().equals("barrier")) {
                return new BarrierBakedModel(model);
            }
            return model;
        });
    }
}
