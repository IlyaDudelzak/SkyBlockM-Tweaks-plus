package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.IBakedDisplay;
import despairscent.skyblockm.tweaks.ItemDisplayBakingManager;
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



    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        ItemDisplayBakingManager.removeEntityById(entityId);
    }
}
