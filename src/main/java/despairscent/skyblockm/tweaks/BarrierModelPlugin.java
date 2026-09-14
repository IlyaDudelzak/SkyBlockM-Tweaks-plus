package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Blocks;

public class BarrierModelPlugin implements ModelLoadingPlugin {
    @Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
            if (context.state().isOf(Blocks.BARRIER)) {
                return new BarrierBakedModel(model);
            }
            return model;
        });
    }
}
