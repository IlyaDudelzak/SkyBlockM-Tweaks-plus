package despairscent.skyblockm.tweaks.features.itemdisplay.mixin;
import despairscent.skyblockm.tweaks.ModUtils;

import despairscent.skyblockm.tweaks.config.ItemDisplayHitboxConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(Entity.class)
public class ItemDisplayEntityMixin {

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void onCanHit(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.ENTITY_AABB) {
            if ((Object) this instanceof DisplayEntity.ItemDisplayEntity) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void onIsCollidable(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.ENTITY_AABB) {
            if ((Object) this instanceof DisplayEntity.ItemDisplayEntity) {
                cir.setReturnValue(true);
            }
        }
    }
}
