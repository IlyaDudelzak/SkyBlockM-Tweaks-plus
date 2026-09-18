package despairscent.skyblockm.tweaks.mixin;
import despairscent.skyblockm.tweaks.ModUtils;

import despairscent.skyblockm.tweaks.features.itemdisplay.HitboxUtils;
import despairscent.skyblockm.tweaks.features.itemdisplay.IBakedDisplay;
import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;
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

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (CONFIG == null) return;
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled) {
                ItemDisplayBakingManager.updateEntity(display);
            }

            if (CONFIG.itemDisplayHitbox != null && CONFIG.itemDisplayHitbox.enabled) {
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

    @Inject(method = "onTrackedDataSet", at = @At("TAIL"))
    private void onTrackedDataSet(net.minecraft.entity.data.TrackedData<?> data, CallbackInfo ci) {
        // Skip hitbox dimension updates (e.g. from EntityCulling)
        if (WIDTH.equals(data) || HEIGHT.equals(data)) {
            return;
        }
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            ItemDisplayBakingManager.invalidateCache(display);
            if (this.skyblockm$isBaked) {
                ItemDisplayBakingManager.removeEntity(display);
            }
        }
    }

    @Inject(method = "updateTrackedPositionAndAngles(DDDFFI)V", at = @At("HEAD"), require = 0)
    private void onUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(display.getX() - x) > 0.01 || Math.abs(display.getY() - y) > 0.01 || Math.abs(display.getZ() - z) > 0.01 ||
                Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(display.getYaw() - yaw)) > 0.05f ||
                Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(display.getPitch() - pitch)) > 0.05f) {
                ItemDisplayBakingManager.invalidateCache(display);
                if (this.skyblockm$isBaked) {
                    ItemDisplayBakingManager.removeEntity(display);
                }
            }
        }
    }

    @Inject(method = "shouldRender(D)Z", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(double distance, CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && (Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            // If it is baked into a chunk, DON'T render it as a dynamic entity!
            if (ItemDisplayBakingManager.shouldHideEntity(display)) {
                cir.setReturnValue(false);
            }
        }
    }
}
