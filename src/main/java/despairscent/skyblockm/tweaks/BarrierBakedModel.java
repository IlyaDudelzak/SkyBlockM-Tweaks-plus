package despairscent.skyblockm.tweaks;

import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Predicate;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class BarrierBakedModel extends WrapperBlockStateModel {

    public BarrierBakedModel(BlockStateModel base) {
        super(base);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Random random, Predicate<Direction> cullTest) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.enabled) {
            super.emitQuads(emitter, blockView, pos, state, random, cullTest);
            return;
        }

        List<ItemDisplayBakingManager.BakedEntityInfo> displays = ItemDisplayBakingManager.getStaticDisplaysAt(pos);

        if (displays != null && !displays.isEmpty()) {
            boolean emittedAny = false;

            for (ItemDisplayBakingManager.BakedEntityInfo info : displays) {
                if (info.quads == null || info.quads.isEmpty()) continue;

                for (ItemDisplayBakingManager.BakedQuadInfo quadInfo : info.quads) {
                    BlockRenderLayer layer = quadInfo.renderLayer != null ? quadInfo.renderLayer : BlockRenderLayer.CUTOUT;

                    emitter.pushTransform(quad -> {
                        // 1. Transform vertex positions
                        for (int i = 0; i < 4; i++) {
                            Vector3f vec = quad.copyPos(i, null);
                            vec.mulPosition(quadInfo.matrix);
                            quad.pos(i, vec);
                        }

                        // 2. Fix winding order if matrix reflected/inverted handedness
                        if (quadInfo.matrix.determinant() < 0) {
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

                        // 3. Clear nominalFace so renderer computes true faceNormal from transformed positions
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

                    emitter.fromBakedQuad(quadInfo.quad);
                    emitter.renderLayer(layer);
                    emitter.ambientOcclusion(TriState.FALSE);

                    int itemColor = quadInfo.tintColor;
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
                    emitter.tintIndex(-1);
                    emitter.emit();

                    emitter.popTransform();
                    emittedAny = true;
                }
            }

            if (emittedAny) {
                return;
            }
        }

        super.emitQuads(emitter, blockView, pos, state, random, cullTest);
    }
}
