package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.mixininner.IMinecraftClientAccessor;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class BarrierBakedModel extends ForwardingBakedModel {
    private static RenderMaterial MATERIAL_TRANSLUCENT;
    private static RenderMaterial MATERIAL_CUTOUT;
    private static RenderMaterial MATERIAL_SOLID;

    public BarrierBakedModel(BakedModel base) {
        this.wrapped = base;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    private static synchronized void initMaterials(Renderer renderer) {
        if (MATERIAL_TRANSLUCENT == null && renderer != null) {
            MATERIAL_TRANSLUCENT = renderer.materialFinder()
                    .blendMode(BlendMode.TRANSLUCENT)
                    .ambientOcclusion(TriState.FALSE)
                    .find();
            MATERIAL_CUTOUT = renderer.materialFinder()
                    .blendMode(BlendMode.CUTOUT_MIPPED)
                    .ambientOcclusion(TriState.FALSE)
                    .find();
            MATERIAL_SOLID = renderer.materialFinder()
                    .blendMode(BlendMode.SOLID)
                    .ambientOcclusion(TriState.FALSE)
                    .find();
        }
    }

    private static RenderMaterial getMaterialFor(ItemDisplayBakingManager.BakedEntityInfo info, Renderer renderer) {
        initMaterials(renderer);
        if (info == null || info.itemStack == null || info.itemStack.isEmpty()) {
            return MATERIAL_TRANSLUCENT;
        }
        if (info.itemStack.getItem() instanceof BlockItem blockItem) {
            RenderLayer layer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
            if (layer == RenderLayer.getTranslucent()) {
                return MATERIAL_TRANSLUCENT;
            } else if (layer == RenderLayer.getCutout() || layer == RenderLayer.getCutoutMipped()) {
                return MATERIAL_CUTOUT;
            } else {
                return MATERIAL_CUTOUT;
            }
        }
        return MATERIAL_TRANSLUCENT;
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

            Renderer renderer = RendererAccess.INSTANCE.getRenderer();

            for (ItemDisplayBakingManager.BakedEntityInfo info : displays) {
                BakedModel itemModel = info.itemModel;

                if (itemModel != null && info.matrix != null) {
                    RenderMaterial material = getMaterialFor(info, renderer);

                    context.pushTransform(quad -> {
                        // 1. Transform vertex positions
                        for (int i = 0; i < 4; i++) {
                            Vector3f vec = quad.copyPos(i, null);
                            vec.mulPosition(info.matrix);
                            quad.pos(i, vec);
                        }

                        // 2. Fix winding order if matrix reflected/inverted handedness
                        if (info.matrix.determinant() < 0) {
                            Vector3f p1 = quad.copyPos(1, null);
                            Vector3f p3 = quad.copyPos(3, null);
                            quad.pos(1, p3);
                            quad.pos(3, p1);

                            float u1 = quad.u(1), v1 = quad.v(1);
                            quad.uv(1, quad.u(3), quad.v(3));
                            quad.uv(3, u1, v1);

                            int c1 = quad.color(1);
                            quad.color(1, quad.color(3));
                            quad.color(3, c1);

                            int l1 = quad.lightmap(1);
                            quad.lightmap(1, quad.lightmap(3));
                            quad.lightmap(3, l1);
                        }

                        // 3. Clear nominalFace so Indium computes true faceNormal from transformed positions
                        quad.nominalFace(null);

                        // 4. Compute and set vertex normals
                        Vector3f p0 = quad.copyPos(0, null);
                        Vector3f p1 = quad.copyPos(1, null);
                        Vector3f p2 = quad.copyPos(2, null);
                        Vector3f p3 = quad.copyPos(3, null);
                        Vector3f v0 = new Vector3f(p2).sub(p0);
                        Vector3f v1 = new Vector3f(p3).sub(p1);
                        Vector3f norm = v0.cross(v1).normalize();
                        if (norm.isFinite() && norm.lengthSquared() > 0.001f) {
                            quad.normal(0, norm);
                            quad.normal(1, norm);
                            quad.normal(2, norm);
                            quad.normal(3, norm);
                        }

                        return true;
                    });

                    Random random = randomSupplier.get();
                    random.setSeed(42L);
                    Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                    QuadEmitter emitter = context.getEmitter();
                    for (Direction dir : dirs) {
                        List<BakedQuad> quads = itemModel.getQuads(null, dir, random);
                        if (quads == null || quads.isEmpty()) continue;
                        for (BakedQuad quad : quads) {
                            emitter.fromVanilla(quad, material, null);

                            int itemColor = -1;
                            if (quad.hasColor()) {
                                if (itemColors != null) {
                                    itemColor = itemColors.getColor(info.itemStack, quad.getColorIndex());
                                }
                                if (itemColor == -1) {
                                    try {
                                        net.minecraft.component.type.DyedColorComponent dyed = info.itemStack.get(net.minecraft.component.DataComponentTypes.DYED_COLOR);
                                        if (dyed != null) {
                                            itemColor = 0xFF000000 | dyed.rgb();
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
