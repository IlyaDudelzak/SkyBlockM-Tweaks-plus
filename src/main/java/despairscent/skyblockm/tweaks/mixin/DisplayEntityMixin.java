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
public abstract class DisplayEntityMixin implements IBakedDisplay {

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

    @org.spongepowered.asm.mixin.Shadow
    @org.spongepowered.asm.mixin.Final
    private static net.minecraft.entity.data.TrackedData<Float> WIDTH;

    @org.spongepowered.asm.mixin.Shadow
    @org.spongepowered.asm.mixin.Final
    private static net.minecraft.entity.data.TrackedData<Float> HEIGHT;

    @org.spongepowered.asm.mixin.Shadow
    private DisplayEntity.RenderState renderState;

    @org.spongepowered.asm.mixin.Shadow
    protected abstract DisplayEntity.RenderState copyRenderState();

    @org.spongepowered.asm.mixin.Shadow
    protected abstract void refreshData(boolean interpolate, float lerpProgress);

    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void onTrackedDataSet(net.minecraft.entity.data.TrackedData<?> data, CallbackInfo ci) {
        // Skip hitbox dimension updates (e.g. from EntityCulling)
        if (WIDTH.equals(data) || HEIGHT.equals(data)) {
            return;
        }
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            try {
                this.renderState = this.copyRenderState();
                this.refreshData(false, 0.0f);
            } catch (Exception ignored) {}
            ItemDisplayBakingManager.invalidateCache(display);
            if (CONFIG.itemDisplayBaking.enabled) {
                ItemDisplayBakingManager.onEntityDataChanged(display);
            }
        }
    }

    @Inject(method = "updateTrackedPositionAndAngles(DDDFFI)V", at = @At("TAIL"), require = 0)
    private void onUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            display.setPosition(x, y, z);
            display.setYaw(yaw);
            display.setPitch(pitch);
            ItemDisplayBakingManager.invalidateCache(display);
            if (CONFIG.itemDisplayBaking.enabled) {
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
