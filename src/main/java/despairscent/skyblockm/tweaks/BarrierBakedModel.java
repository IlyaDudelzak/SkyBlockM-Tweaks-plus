package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class BarrierBakedModel extends ForwardingBakedModel {
    public BarrierBakedModel(BakedModel base) {
        this.wrapped = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        if (!CONFIG.itemDisplayHitbox.enabled || !CONFIG.itemDisplayHitbox.optimizeAsBlocks) {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        List<ItemDisplayBakingManager.BakedEntityInfo> displays = ItemDisplayBakingManager.getStaticDisplaysAt(pos);
        
        if (displays != null && !displays.isEmpty()) {
            boolean emittedAny = false;
            
            for (ItemDisplayBakingManager.BakedEntityInfo info : displays) {
                DisplayEntity.ItemDisplayEntity display = info.entity;
                var data = display.getData();
                BakedModel itemModel = info.itemModel;
                
                if (data != null && itemModel != null) {
                    var renderState = display.getRenderState();
                    if (renderState != null) {
                        context.pushTransform(quad -> {
                            Matrix4f matrix = new Matrix4f();
                            
                            float rx = (float) (display.getX() - pos.getX());
                            float ry = (float) (display.getY() - pos.getY());
                            float rz = (float) (display.getZ() - pos.getZ());
                            matrix.translate(rx, ry, rz);
                            
                            matrix.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-display.getYaw()));
                            matrix.rotate(RotationAxis.POSITIVE_X.rotationDegrees(display.getPitch()));
                            
                            matrix.mul(renderState.transformation().interpolate(1.0f).getMatrix());
                            
                            matrix.rotate(RotationAxis.POSITIVE_Y.rotation((float)Math.PI));
                            
                            net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
                            itemModel.getTransformation().getTransformation(data.itemTransform()).apply(false, ms);
                            matrix.mul(ms.peek().getPositionMatrix());
                            
                            matrix.translate(-0.5f, -0.5f, -0.5f);
                            
                            for (int i = 0; i < 4; i++) {
                                Vector3f vec = quad.copyPos(i, null);
                                vec.mulPosition(matrix);
                                quad.pos(i, vec);
                                
                                if (quad.hasNormal(i)) {
                                    Vector3f norm = quad.copyNormal(i, null);
                                    matrix.transformDirection(norm);
                                    quad.normal(i, norm);
                                }
                            }
                            
                            return true;
                        });
                        
                        Random random = randomSupplier.get();
                        Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                        net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter emitter = context.getEmitter();
                        for (Direction dir : dirs) {
                            List<BakedQuad> quads = itemModel.getQuads(null, dir, random);
                            for (BakedQuad quad : quads) {
                                emitter.fromVanilla(quad.getVertexData(), 0);
                                emitter.colorIndex(quad.getColorIndex());
                                emitter.nominalFace(quad.getFace());
                                emitter.cullFace(dir);
                                emitter.emit();
                            }
                        }
                        
                        context.popTransform();
                        emittedAny = true;
                    }
                }
            }
            
            if (emittedAny) {
                return;
            }
        }
    }
}
