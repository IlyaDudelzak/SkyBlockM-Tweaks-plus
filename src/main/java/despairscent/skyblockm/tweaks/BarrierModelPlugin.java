package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
//? if >=1.21.8 {
/*import net.minecraft.block.Blocks;
*///?} else {
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
//?}

public class BarrierModelPlugin implements ModelLoadingPlugin {
    //? if >=1.21.8 {
    /*@Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
            if (context.state().isOf(Blocks.BARRIER)) {
                return new BarrierBakedModel(model);
            }
            return model;
        });
    }
    *///?} elif =1.21.3 {
    /*@Override
    public void initialize(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (context.topLevelId() != null && context.topLevelId().id().getNamespace().equals("minecraft") && context.topLevelId().id().getPath().equals("barrier")) {
                return new BarrierBakedModel(model);
            }
            return model;
        });
    }
    *///?} else {
    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            //? if =1.21.1 {
            if (context.topLevelId() != null && context.topLevelId().id().getNamespace().equals("minecraft") && context.topLevelId().id().getPath().equals("barrier")) {
                return new BarrierBakedModel(model);
            }
            //?} else {
            /*if (context.id() != null && context.id().getNamespace().equals("minecraft") && (context.id().getPath().equals("barrier") || context.id().getPath().equals("block/barrier"))) {
                return new BarrierBakedModel(model);
            }
            *///?}
            return model;
        });
    }
    //?}
}
