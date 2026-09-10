package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (CONFIG.itemDisplayHitbox.enabled) {
            // Vanilla pushOutOfBlocks wrongly assumes any block with a collision shape is a full 1x1x1 block
            // This causes it to aggressively push players out of partial custom hitboxes.
            ci.cancel();
        }
    }
}
