package despairscent.skyblockm.tweaks.features.itemdisplay.mixin;
import despairscent.skyblockm.tweaks.ModUtils;

import despairscent.skyblockm.tweaks.features.itemdisplay.IBakedDisplay;
import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(Entity.class)
public abstract class EntityMixin {

	@Shadow public abstract EntityType<?> getType();

	@Redirect(method = "isInvisibleTo",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z"))
	private boolean redirectSpectatorCheck(PlayerEntity instance) {
		if (CONFIG != null && CONFIG.hideHiddenArmorStands != null && CONFIG.hideHiddenArmorStands.enabled && this.getType() == EntityType.ARMOR_STAND) {
			return false;
		}
		return instance.isSpectator();
	}

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            ItemDisplayBakingManager.onEntityRemoved(display);
        }
    }

    @Inject(method = "setYaw", at = @At("HEAD"), require = 0)
    private void onSetYaw(float yaw, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(MathHelper.wrapDegrees(display.getYaw() - yaw)) > 0.05f) {
                ItemDisplayBakingManager.invalidateCache(display);
                if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
                    ItemDisplayBakingManager.onEntityDataChanged(display);
                }
            }
        }
    }

    @Inject(method = "setPitch", at = @At("HEAD"), require = 0)
    private void onSetPitch(float pitch, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(MathHelper.wrapDegrees(display.getPitch() - pitch)) > 0.05f) {
                ItemDisplayBakingManager.invalidateCache(display);
                if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
                    ItemDisplayBakingManager.onEntityDataChanged(display);
                }
            }
        }
    }

    //? if <1.20.2 {
    /*@Inject(method = "updateTrackedPositionAndAngles(DDDFFIZ)V", at = @At("HEAD"), require = 0)
    private void onUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps, boolean interpolate, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(display.getX() - x) > 0.01 || Math.abs(display.getY() - y) > 0.01 || Math.abs(display.getZ() - z) > 0.01 ||
                Math.abs(MathHelper.wrapDegrees(display.getYaw() - yaw)) > 0.05f ||
                Math.abs(MathHelper.wrapDegrees(display.getPitch() - pitch)) > 0.05f) {
                ItemDisplayBakingManager.invalidateCache(display);
                if (display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
                    ItemDisplayBakingManager.removeEntity(display);
                }
            }
        }
    }
    *///?} else if >=1.21.11 {
    /*@Inject(method = "updateTrackedPositionAndAngles(Lnet/minecraft/util/math/Vec3d;FF)V", at = @At("HEAD"), require = 0)
    private void onUpdateTrackedPositionAndAngles(net.minecraft.util.math.Vec3d pos, float yaw, float pitch, CallbackInfo ci) {
        if ((Object) this instanceof DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(display.getX() - pos.x) > 0.01 || Math.abs(display.getY() - pos.y) > 0.01 || Math.abs(display.getZ() - pos.z) > 0.01 ||
                Math.abs(MathHelper.wrapDegrees(display.getYaw() - yaw)) > 0.05f ||
                Math.abs(MathHelper.wrapDegrees(display.getPitch() - pitch)) > 0.05f) {
                ItemDisplayBakingManager.invalidateCache(display);
                if (display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
                    ItemDisplayBakingManager.removeEntity(display);
                }
            }
        }
    }
    *///?}
}
