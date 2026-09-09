package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.HitboxUtils;
import despairscent.skyblockm.tweaks.ItemDisplayBakingManager;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(DisplayEntity.class)
public class DisplayEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (CONFIG.itemDisplayHitbox.enabled) {
            if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
                Box box = ItemDisplayBakingManager.getCachedBox(display);
                if (box != null) {
                    display.setBoundingBox(box);
                }
                
                // Try baking into chunk
                if (CONFIG.itemDisplayHitbox.optimizeAsBlocks) {
                    ItemDisplayBakingManager.updateEntity(display);
                }
            }
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.optimizeAsBlocks && (Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            // If it is baked into a chunk, DON'T render it as a dynamic entity!
            if (ItemDisplayBakingManager.isBaked(display)) {
                cir.setReturnValue(false);
            }
        }
    }
}
