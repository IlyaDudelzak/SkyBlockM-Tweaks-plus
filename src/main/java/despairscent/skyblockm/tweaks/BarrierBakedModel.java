package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.mixininner.IMinecraftClientAccessor;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.random.Random;
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
        if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.enabled) {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        List<ItemDisplayBakingManager.BakedEntityInfo> displays = ItemDisplayBakingManager.getStaticDisplaysAt(pos);
        
        if (displays != null && !displays.isEmpty()) {
            boolean emittedAny = false;
            ItemColors itemColors = null;
            if (MinecraftClient.getInstance() instanceof IMinecraftClientAccessor accessor) {
                itemColors = accessor.skyblockm$getItemColors();
            }
            
            for (ItemDisplayBakingManager.BakedEntityInfo info : displays) {
                BakedModel itemModel = info.itemModel;
                
                if (itemModel != null && info.matrix != null) {
                    context.pushTransform(quad -> {
                        for (int i = 0; i < 4; i++) {
                            Vector3f vec = quad.copyPos(i, null);
                            vec.mulPosition(info.matrix);
                            quad.pos(i, vec);
                            
                            if (quad.hasNormal(i)) {
                                Vector3f norm = quad.copyNormal(i, null);
                                info.matrix.transformDirection(norm);
                                quad.normal(i, norm);
                            }
                        }
                        return true;
                    });
                    
                    Random random = randomSupplier.get();
                    random.setSeed(42L);
                    Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                    net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter emitter = context.getEmitter();
                    for (Direction dir : dirs) {
                        List<BakedQuad> quads = itemModel.getQuads(null, dir, random);
                        if (quads == null || quads.isEmpty()) continue;
                        for (BakedQuad quad : quads) {
                            emitter.fromVanilla(quad.getVertexData(), 0);
                            
                            int itemColor = -1;
                            if (quad.hasColor()) {
                                if (itemColors != null) {
                                    itemColor = itemColors.getColor(info.itemStack, quad.getColorIndex());
                                }
                                if (itemColor == -1) {
                                    try {
                                        if (info.itemStack.hasNbt() && info.itemStack.getSubNbt("display") != null) {
                                            net.minecraft.nbt.NbtCompound displayTag = info.itemStack.getSubNbt("display");
                                            if (displayTag.contains("color", 99)) {
                                                itemColor = 0xFF000000 | displayTag.getInt("color");
                                            }
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                            
                            if (itemColor != -1) {
                                for (int i = 0; i < 4; i++) {
                                    int currentColor = emitter.color(i);
                                    int a = ((currentColor >> 24) & 0xFF) * ((itemColor >> 24) & 0xFF) / 255;
                                    int r = ((currentColor >> 16) & 0xFF) * ((itemColor >> 16) & 0xFF) / 255;
                                    int g = ((currentColor >> 8) & 0xFF) * ((itemColor >> 8) & 0xFF) / 255;
                                    int b = (currentColor & 0xFF) * (itemColor & 0xFF) / 255;
                                    emitter.color(i, (a << 24) | (r << 16) | (g << 8) | b);
                                }
                            }
                            emitter.colorIndex(-1);
                            emitter.cullFace(null);
                            emitter.nominalFace(null);
                            emitter.emit();
                        }
                    }
                    
                    context.popTransform();
                    emittedAny = true;
                }
            }
            
            if (emittedAny) {
                return;
            }
        }
    }
}
