package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

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

    @Redirect(method = "getCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private static EntityHitResult updateTargetedEntityRaycastRedirect(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double d) {
        if (CONFIG.storageTargetingFix.enabled) {
            return ProjectileUtil.raycast(entity, min, max, box, e ->
                    !(e instanceof ItemFrameEntity itemFrame &&
                            itemFrame.getHorizontalFacing() == Direction.DOWN && itemFrame.isInvisible() &&
                            !(itemFrame.getYaw() == 0 && itemFrame.getPitch() == 90)
                    ) && predicate.test(e), d);
        }
        return ProjectileUtil.raycast(entity, min, max, box, predicate, d);
    }
}
