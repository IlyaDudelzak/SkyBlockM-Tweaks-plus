package despairscent.skyblockm.tweaks.features.itemdisplay;
import despairscent.skyblockm.tweaks.ModUtils;

//? if >=1.21.8 {

/*import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
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
                    BlockRenderLayer layer = quadInfo.renderLayer != null ? quadInfo.renderLayer : BlockRenderLayer.TRANSLUCENT;

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
                        if ((itemColor & 0xFF000000) == 0) {
                            itemColor |= 0xFF000000;
                        }
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
*///?} elif =1.21.3 {
/*
import despairscent.skyblockm.tweaks.core.mixininner.IMinecraftClientAccessor;
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

    private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.util.Identifier, Boolean> TRANSLUCENT_SPRITE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isSpriteTranslucent(net.minecraft.client.texture.Sprite sprite) {
        if (sprite == null) return false;
        net.minecraft.client.texture.SpriteContents contents = sprite.getContents();
        if (contents == null) return false;
        net.minecraft.util.Identifier id = contents.getId();
        if (id == null) return false;
        return TRANSLUCENT_SPRITE_CACHE.computeIfAbsent(id, k -> {
            try {
                net.minecraft.client.texture.NativeImage img = contents.image;
                if (img != null) {
                    int w = contents.getWidth();
                    int h = contents.getHeight();
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int a = Byte.toUnsignedInt(img.getOpacity(x, y));
                            if (a > 0 && a < 255) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
            return false;
        });
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
                    initMaterials(renderer);

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
                            RenderMaterial quadMaterial = MATERIAL_CUTOUT;
                            if (isSpriteTranslucent(quad.getSprite())) {
                                quadMaterial = MATERIAL_TRANSLUCENT;
                            } else if (info.itemStack != null && info.itemStack.getItem() instanceof BlockItem blockItem) {
                                RenderLayer layer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                if (layer == RenderLayer.getTranslucent()) {
                                    quadMaterial = MATERIAL_TRANSLUCENT;
                                }
                            }
                            emitter.fromVanilla(quad, quadMaterial, null);

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
*///?} elif =1.21.1 {
import despairscent.skyblockm.tweaks.core.mixininner.IMinecraftClientAccessor;
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
//?} elif =1.20.4 {
/*
import despairscent.skyblockm.tweaks.core.mixininner.IMinecraftClientAccessor;
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

    private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.util.Identifier, Boolean> TRANSLUCENT_SPRITE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isSpriteTranslucent(net.minecraft.client.texture.Sprite sprite) {
        if (sprite == null) return false;
        net.minecraft.client.texture.SpriteContents contents = sprite.getContents();
        if (contents == null) return false;
        net.minecraft.util.Identifier id = contents.getId();
        if (id == null) return false;
        return TRANSLUCENT_SPRITE_CACHE.computeIfAbsent(id, k -> {
            try {
                net.minecraft.client.texture.NativeImage img = contents.image;
                if (img != null) {
                    int w = contents.getWidth();
                    int h = contents.getHeight();
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int a = Byte.toUnsignedInt(img.getOpacity(x, y));
                            if (a > 5 && a < 250) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
            return false;
        });
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
                    initMaterials(renderer);

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
                            RenderMaterial quadMaterial = MATERIAL_CUTOUT;
                            if (isSpriteTranslucent(quad.getSprite())) {
                                quadMaterial = MATERIAL_TRANSLUCENT;
                            } else if (info.itemStack != null && info.itemStack.getItem() instanceof BlockItem blockItem) {
                                RenderLayer layer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                if (layer == RenderLayer.getTranslucent()) {
                                    quadMaterial = MATERIAL_TRANSLUCENT;
                                }
                            }
                            emitter.fromVanilla(quad, quadMaterial, null);

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
*///?} else {
/*
import despairscent.skyblockm.tweaks.core.mixininner.IMinecraftClientAccessor;
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

    private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.util.Identifier, Boolean> TRANSLUCENT_SPRITE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isSpriteTranslucent(net.minecraft.client.texture.Sprite sprite) {
        if (sprite == null) return false;
        net.minecraft.client.texture.SpriteContents contents = sprite.getContents();
        if (contents == null) return false;
        net.minecraft.util.Identifier id = contents.getId();
        if (id == null) return false;
        return TRANSLUCENT_SPRITE_CACHE.computeIfAbsent(id, k -> {
            try {
                net.minecraft.client.texture.NativeImage img = contents.image;
                if (img != null) {
                    int w = contents.getWidth();
                    int h = contents.getHeight();
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int a = Byte.toUnsignedInt(img.getOpacity(x, y));
                            if (a > 0 && a < 255) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
            return false;
        });
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
                    initMaterials(renderer);

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
                            RenderMaterial quadMaterial = MATERIAL_CUTOUT;
                            if (isSpriteTranslucent(quad.getSprite())) {
                                quadMaterial = MATERIAL_TRANSLUCENT;
                            } else if (info.itemStack != null && info.itemStack.getItem() instanceof BlockItem blockItem) {
                                RenderLayer layer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                if (layer == RenderLayer.getTranslucent()) {
                                    quadMaterial = MATERIAL_TRANSLUCENT;
                                }
                            }
                            emitter.fromVanilla(quad, quadMaterial, null);

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
*///?}
