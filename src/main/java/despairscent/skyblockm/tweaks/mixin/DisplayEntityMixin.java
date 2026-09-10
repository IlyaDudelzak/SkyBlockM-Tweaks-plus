package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.HitboxUtils;
import despairscent.skyblockm.tweaks.IBakedDisplay;
import despairscent.skyblockm.tweaks.ItemDisplayBakingManager;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(DisplayEntity.class)
public class DisplayEntityMixin implements IBakedDisplay {

    @Unique
    private boolean skyblockm$isBaked = false;

    @Override
    public boolean skyblockm$isBaked() {
        return this.skyblockm$isBaked;
    }

    @Override
    public void skyblockm$setBaked(boolean baked) {
        this.skyblockm$isBaked = baked;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (CONFIG.itemDisplayBaking.enabled) {
                if (this.skyblockm$isBaked) {
                    ci.cancel();
                    return;
                }

                ItemDisplayBakingManager.updateEntity(display);
                
                if (this.skyblockm$isBaked) {
                    ci.cancel();
                    return;
                }
            }

            if (CONFIG.itemDisplayHitbox.enabled) {
                Box box = ItemDisplayBakingManager.getCachedBox(display);
                if (box != null) {
                    display.setBoundingBox(box);
                }
            }
        }
    }

    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void onTrackedDataSet(net.minecraft.entity.data.TrackedData<?> data, CallbackInfo ci) {
        if (CONFIG.itemDisplayBaking.enabled) {
            if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
                ItemDisplayBakingManager.onEntityDataChanged(display);
            }
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.itemDisplayBaking.enabled && (Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            // If it is baked into a chunk, DON'T render it as a dynamic entity!
            if (ItemDisplayBakingManager.shouldHideEntity(display)) {
                cir.setReturnValue(false);
            }
        }
    }
}
