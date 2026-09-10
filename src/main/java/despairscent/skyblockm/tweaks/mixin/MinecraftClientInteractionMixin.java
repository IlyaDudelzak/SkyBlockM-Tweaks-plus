package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.config.ItemDisplayHitboxConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(MinecraftClient.class)
public class MinecraftClientInteractionMixin {

    @Shadow public HitResult crosshairTarget;

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void redirectAttackToBlock(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.ENTITY_AABB) {
            if (this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof DisplayEntity.ItemDisplayEntity display) {
                BlockPos pos = display.getBlockPos();
                this.crosshairTarget = new BlockHitResult(entityHit.getPos(), Direction.UP, pos, false);
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void redirectInteractToBlock(CallbackInfo ci) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.ENTITY_AABB) {
            if (this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof DisplayEntity.ItemDisplayEntity display) {
                BlockPos pos = display.getBlockPos();
                this.crosshairTarget = new BlockHitResult(entityHit.getPos(), Direction.UP, pos, false);
            }
        }
    }
}
