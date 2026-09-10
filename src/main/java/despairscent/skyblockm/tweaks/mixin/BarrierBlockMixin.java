package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.config.ItemDisplayHitboxConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(AbstractBlock.class)
public class BarrierBlockMixin {

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    private void customOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        handleShape(state, world, pos, cir, false);
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void customCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        handleShape(state, world, pos, cir, true);
    }

    private void handleShape(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir, boolean isCollision) {
        if (!CONFIG.itemDisplayHitbox.enabled) return;
        
        if (state.isOf(Blocks.BARRIER) && world instanceof World w) {
            List<DisplayEntity.ItemDisplayEntity> entities = w.getEntitiesByClass(DisplayEntity.ItemDisplayEntity.class, new Box(pos), e -> true);
            if (!entities.isEmpty()) {
                if (CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.VOXEL_SHAPE) {
                    DisplayEntity.ItemDisplayEntity entity = entities.get(0);
                    Box box = entity.getBoundingBox();
                    
                    if (box.getAverageSideLength() > 0.01) {
                        VoxelShape shape = VoxelShapes.cuboid(
                                box.minX - pos.getX(),
                                box.minY - pos.getY(),
                                box.minZ - pos.getZ(),
                                box.maxX - pos.getX(),
                                box.maxY - pos.getY(),
                                box.maxZ - pos.getZ()
                        );
                        cir.setReturnValue(shape);
                    }
                } else if (CONFIG.itemDisplayHitbox.hitboxType == ItemDisplayHitboxConfig.HitboxType.ENTITY_AABB) {
                    cir.setReturnValue(VoxelShapes.empty());
                }
            } else if (isCollision && CONFIG.itemDisplayHitbox.antiRubberband) {
                // If they want to walk through all barriers, wait no... 
                // Only if there is an item_display nearby?
                // Actually, the above logic already makes it empty for ENTITY_AABB, or matching shape for VOXEL_SHAPE.
            }
        }
    }
}
