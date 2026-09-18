package despairscent.skyblockm.tweaks.mixin;
import despairscent.skyblockm.tweaks.ModUtils;

import despairscent.skyblockm.tweaks.features.itemdisplay.IBakedDisplay;
import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(value = ClientWorld.class, priority = 100)
public class ClientWorldMixin {
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        ItemDisplayBakingManager.onBlockChanged(pos, state);
    }

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void onTickEntity(Entity entity, CallbackInfo ci) {
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && entity instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        ItemDisplayBakingManager.removeEntityById(entityId);
    }
}
