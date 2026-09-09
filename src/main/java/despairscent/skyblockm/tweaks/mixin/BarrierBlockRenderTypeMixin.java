package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.config.ItemDisplayHitboxConfig;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(BarrierBlock.class)
public class BarrierBlockRenderTypeMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(BlockState state, CallbackInfoReturnable<BlockRenderType> cir) {
        if (CONFIG.itemDisplayHitbox.enabled) { // Maybe add a config for optimization
            cir.setReturnValue(BlockRenderType.MODEL);
        }
    }
}
