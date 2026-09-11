package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

    @org.spongepowered.asm.mixin.injection.Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity display) {
            despairscent.skyblockm.tweaks.ItemDisplayBakingManager.onEntityRemoved(display);
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "setYaw", at = @At("RETURN"), require = 0)
    private void onSetYaw(float yaw, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity display) {
            despairscent.skyblockm.tweaks.ItemDisplayBakingManager.invalidateCache(display);
            if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && display instanceof despairscent.skyblockm.tweaks.IBakedDisplay baked && baked.skyblockm$isBaked()) {
                despairscent.skyblockm.tweaks.ItemDisplayBakingManager.onEntityDataChanged(display);
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "setPitch", at = @At("RETURN"), require = 0)
    private void onSetPitch(float pitch, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity display) {
            despairscent.skyblockm.tweaks.ItemDisplayBakingManager.invalidateCache(display);
            if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && display instanceof despairscent.skyblockm.tweaks.IBakedDisplay baked && baked.skyblockm$isBaked()) {
                despairscent.skyblockm.tweaks.ItemDisplayBakingManager.onEntityDataChanged(display);
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "setRotation", at = @At("RETURN"), require = 0)
    private void onSetRotation(float yaw, float pitch, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity display) {
            despairscent.skyblockm.tweaks.ItemDisplayBakingManager.invalidateCache(display);
            if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled && display instanceof despairscent.skyblockm.tweaks.IBakedDisplay baked && baked.skyblockm$isBaked()) {
                despairscent.skyblockm.tweaks.ItemDisplayBakingManager.onEntityDataChanged(display);
            }
        }
    }
}
