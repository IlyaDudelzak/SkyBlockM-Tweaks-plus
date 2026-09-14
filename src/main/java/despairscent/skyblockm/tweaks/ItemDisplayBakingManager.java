package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.mixininner.IItemRenderStateAccessor;
import despairscent.skyblockm.tweaks.mixininner.ILayerRenderStateAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ItemDisplayBakingManager {
    public static class BakedQuadInfo {
        public final BakedQuad quad;
        public final Matrix4f matrix;
        public final BlockRenderLayer renderLayer;
        public final int tintColor;

        public BakedQuadInfo(BakedQuad quad, Matrix4f matrix, BlockRenderLayer renderLayer, int tintColor) {
            this.quad = quad;
            this.matrix = matrix;
            this.renderLayer = renderLayer;
            this.tintColor = tintColor;
        }
    }

    public static class BakedEntityInfo {
        public final int entityId;
        public final BlockPos pos;
        public final ItemStack itemStack;
        public final List<BakedQuadInfo> quads;
        public final Matrix4f matrix;
        public final Object itemTransform;
        public final Object renderState;
        public final Object data;
        public final float yaw;
        public final float pitch;
        public final double x, y, z;
        public final net.minecraft.util.math.AffineTransformation affineTransformation;

        public BakedEntityInfo(DisplayEntity.ItemDisplayEntity display, BlockPos pos) {
            this.entityId = display.getId();
            this.pos = pos;
            this.yaw = display.getYaw();
            this.pitch = display.getPitch();
            this.x = display.getX();
            this.y = display.getY();
            this.z = display.getZ();

            var rs = display.getRenderState();
            this.renderState = rs;
            net.minecraft.util.math.AffineTransformation affine = null;
            if (rs != null) {
                try {
                    affine = rs.transformation().interpolate(1.0f);
                } catch (Exception ignored) {}
            }
            this.affineTransformation = affine;
            this.data = display.getData();
            
            ItemStack stack = ItemStack.EMPTY;
            Object transform = null;
            List<BakedQuadInfo> bakedQuads = new ArrayList<>();
            MatrixStack ms = new MatrixStack();
            try {
                var d = display.getData();
                if (d != null && !d.itemStack().isEmpty()) {
                    stack = d.itemStack().copy();
                    transform = d.itemTransform();

                    float rx = (float) (display.getX() - pos.getX());
                    float ry = (float) (display.getY() - pos.getY());
                    float rz = (float) (display.getZ() - pos.getZ());
                    ms.translate(rx, ry, rz);
                    
                    org.joml.Quaternionf billboardRot = new org.joml.Quaternionf().rotationYXZ(
                        (float) Math.toRadians(-display.getYaw()),
                        (float) Math.toRadians(display.getPitch()),
                        0.0f
                    );
                    ms.multiply(billboardRot);
                    
                    if (affine != null) {
                        ms.multiplyPositionMatrix(affine.getMatrix());
                    }
                    
                    ms.multiply(RotationAxis.POSITIVE_Y.rotation((float)Math.PI));
                    Matrix4f baseMatrix = new Matrix4f(ms.peek().getPositionMatrix());

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.getItemModelManager() != null) {
                        ItemRenderState itemRenderState = new ItemRenderState();
                        client.getItemModelManager().updateForNonLivingEntity(itemRenderState, stack, d.itemTransform(), display);

                        if (itemRenderState instanceof IItemRenderStateAccessor itemAccessor) {
                            int layerCount = itemAccessor.skyblockm$getLayerCount();
                            ItemRenderState.LayerRenderState[] layers = itemAccessor.skyblockm$getLayers();
                            for (int l = 0; l < layerCount && l < layers.length; l++) {
                                ItemRenderState.LayerRenderState layer = layers[l];
                                if (layer instanceof ILayerRenderStateAccessor layerAccessor) {
                                    if (layerAccessor.skyblockm$getSpecialModelType() != null) {
                                        continue;
                                    }
                                    net.minecraft.client.render.model.json.Transformation trans = layerAccessor.skyblockm$getTransform();
                                    MatrixStack layerMs = new MatrixStack();
                                    layerMs.multiplyPositionMatrix(baseMatrix);
                                    if (trans != null) {
                                        trans.apply(d.itemTransform().isLeftHand(), layerMs.peek());
                                    }
                                    Matrix4f layerMatrix = new Matrix4f(layerMs.peek().getPositionMatrix());

                                    BlockRenderLayer layerBlockRenderLayer = BlockRenderLayer.CUTOUT_MIPPED;
                                    if (stack.getItem() instanceof BlockItem blockItem) {
                                        layerBlockRenderLayer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                    }
                                    int[] tints = layerAccessor.skyblockm$getTints();
                                    List<BakedQuad> layerQuads = layer.getQuads();
                                    if (layerQuads != null) {
                                        for (BakedQuad quad : layerQuads) {
                                            int tintColor = -1;
                                            if (quad.hasTint() && quad.tintIndex() >= 0 && tints != null && quad.tintIndex() < tints.length) {
                                                tintColor = tints[quad.tintIndex()];
                                            }
                                            bakedQuads.add(new BakedQuadInfo(quad, layerMatrix, layerBlockRenderLayer, tintColor));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            this.itemStack = stack;
            this.itemTransform = transform;
            this.quads = bakedQuads;
            this.matrix = ms.peek().getPositionMatrix();
        }

        public boolean matches(DisplayEntity.ItemDisplayEntity display) {
            return this.entityId == display.getId();
        }

        public boolean hasSameVisual(DisplayEntity.ItemDisplayEntity display) {
            if (Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(this.yaw - display.getYaw())) > 0.01f ||
                Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(this.pitch - display.getPitch())) > 0.01f) {
                return false;
            }
            if (Math.abs(this.x - display.getX()) > 0.001 || Math.abs(this.y - display.getY()) > 0.001 || Math.abs(this.z - display.getZ()) > 0.001) {
                return false;
            }
            var d = display.getData();
            if (d == null) return false;
            ItemStack otherStack = d.itemStack();
            if (otherStack.isEmpty()) return false;
            if (!ItemStack.areEqual(this.itemStack, otherStack) || !java.util.Objects.equals(this.itemTransform, d.itemTransform())) {
                return false;
            }
            var currentRs = display.getRenderState();
            net.minecraft.util.math.AffineTransformation currentAffine = null;
            if (currentRs != null) {
                try {
                    currentAffine = currentRs.transformation().interpolate(1.0f);
                } catch (Exception ignored) {}
            }
            if (!java.util.Objects.equals(this.affineTransformation, currentAffine)) {
                return false;
            }
            return true;
        }
    }

    private static class BoxCacheEntry {
        Object renderState;
        Object data;
        double x, y, z;
        float yaw, pitch;
        Box cachedBox;
    }

    private static final ConcurrentHashMap<BlockPos, CopyOnWriteArrayList<BakedEntityInfo>> STATIC_DISPLAYS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, BlockPos> ENTITY_ID_TO_POS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, BoxCacheEntry> BOX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> PENDING_SECTION_REBUILDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> SECTION_LAST_REBUILD = new ConcurrentHashMap<>();

    public static Box getCachedBox(DisplayEntity.ItemDisplayEntity display) {
        int id = display.getId();
        BoxCacheEntry entry = BOX_CACHE.computeIfAbsent(id, k -> new BoxCacheEntry());
        
        boolean posChanged = entry.x != display.getX() || entry.y != display.getY() || entry.z != display.getZ() ||
                             entry.yaw != display.getYaw() || entry.pitch != display.getPitch();
        
        boolean stateChanged = entry.renderState != display.getRenderState() || entry.data != display.getData();
        
        if (stateChanged || posChanged || entry.cachedBox == null) {
            entry.cachedBox = HitboxUtils.calculateDisplayBox(display);
            entry.x = display.getX();
            entry.y = display.getY();
            entry.z = display.getZ();
            entry.yaw = display.getYaw();
            entry.pitch = display.getPitch();
            entry.renderState = display.getRenderState();
            entry.data = display.getData();
        }
        
        return entry.cachedBox;
    }

    public static boolean isBakeable(DisplayEntity.ItemDisplayEntity display) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.enabled) {
            return false;
        }
        if (display.getWorld() == null) {
            return false;
        }
        BlockPos pos = display.getBlockPos();
        if (!display.getWorld().getBlockState(pos).isOf(net.minecraft.block.Blocks.BARRIER)) {
            return false;
        }
        var rs = display.getRenderState();
        if (rs != null && rs.billboardConstraints() != DisplayEntity.BillboardMode.FIXED) {
            return false;
        }
        var d = display.getData();
        if (d == null || d.itemStack().isEmpty()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getItemModelManager() == null) {
            return false;
        }
        ItemRenderState itemRenderState = new ItemRenderState();
        try {
            client.getItemModelManager().updateForNonLivingEntity(itemRenderState, d.itemStack(), d.itemTransform(), display);
        } catch (Exception e) {
            return false;
        }
        if (itemRenderState.isEmpty()) {
            return false;
        }
        if (itemRenderState instanceof IItemRenderStateAccessor itemAccessor) {
            int layerCount = itemAccessor.skyblockm$getLayerCount();
            ItemRenderState.LayerRenderState[] layers = itemAccessor.skyblockm$getLayers();
            for (int l = 0; l < layerCount && l < layers.length; l++) {
                if (layers[l] instanceof ILayerRenderStateAccessor layerAccessor) {
                    if (layerAccessor.skyblockm$getSpecialModelType() != null) {
                        return false;
                    }
                }
            }
        }

        Box box = getCachedBox(display);
        if (box == null) return false;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        return (box.maxX - box.minX) <= maxSize && 
               (box.maxY - box.minY) <= maxSize && 
               (box.maxZ - box.minZ) <= maxSize;
    }

    public static boolean shouldHideEntity(DisplayEntity.ItemDisplayEntity display) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.enabled) {
            return false;
        }
        if (!CONFIG.itemDisplayBaking.hideBakeableEntities) {
            return false;
        }
        if (display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
            return true;
        }
        return false;
    }

    public static void invalidateCache(DisplayEntity.ItemDisplayEntity display) {
        if (display != null) {
            BOX_CACHE.remove(display.getId());
        }
    }

    public static void updateEntity(DisplayEntity.ItemDisplayEntity display) {
        if (!isBakeable(display)) {
            if (display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
                removeEntity(display);
            }
            return;
        }

        BlockPos pos = display.getBlockPos();
        CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.computeIfAbsent(pos, k -> new CopyOnWriteArrayList<>());
        for (BakedEntityInfo info : list) {
            if (info.matches(display)) {
                if (info.hasSameVisual(display)) {
                    if (display instanceof IBakedDisplay baked) {
                        baked.skyblockm$setBaked(true);
                    }
                    return;
                } else {
                    list.remove(info);
                    break;
                }
            }
        }

        BakedEntityInfo newInfo = new BakedEntityInfo(display, pos);
        list.add(newInfo);
        ENTITY_ID_TO_POS.put(display.getId(), pos);
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(true);
        }
        markSectionDirty(pos);
    }

    public static void markSectionDirty(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        
        long now = System.currentTimeMillis();
        long settleDelayMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkSettleTime * 1000.0));
        
        PENDING_SECTION_REBUILDS.put(sectionLong, now + settleDelayMs);
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(false);
        }
        int id = display.getId();
        invalidateCache(display);
        BlockPos pos = ENTITY_ID_TO_POS.remove(id);
        if (pos == null) {
            pos = display.getBlockPos();
        }
        CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.get(pos);
        if (list != null) {
            if (list.removeIf(info -> info.matches(display))) {
                if (list.isEmpty()) {
                    STATIC_DISPLAYS.remove(pos);
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void removeEntityById(int entityId) {
        BOX_CACHE.remove(entityId);
        BlockPos pos = ENTITY_ID_TO_POS.remove(entityId);
        if (pos == null) {
            return;
        }
        CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.get(pos);
        if (list != null) {
            if (list.removeIf(info -> info.entityId == entityId)) {
                if (list.isEmpty()) {
                    STATIC_DISPLAYS.remove(pos);
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void onEntityRemoved(DisplayEntity.ItemDisplayEntity display) {
        removeEntity(display);
    }

    public static void onEntityDataChanged(DisplayEntity.ItemDisplayEntity display) {
        updateEntity(display);
    }

    public static void onBlockChanged(BlockPos pos, BlockState newState) {
        if (!newState.isOf(net.minecraft.block.Blocks.BARRIER)) {
            CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.remove(pos);
            if (list != null) {
                for (BakedEntityInfo info : list) {
                    ENTITY_ID_TO_POS.remove(info.entityId);
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void tickRebuilds() {
        if (PENDING_SECTION_REBUILDS.isEmpty()) return;
        long now = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;
        
        var iterator = PENDING_SECTION_REBUILDS.entrySet().iterator();
        int processed = 0;
        while (iterator.hasNext() && processed < 8) {
            var entry = iterator.next();
            if (now >= entry.getValue()) {
                long sectionLong = entry.getKey();
                int cx = ChunkSectionPos.unpackX(sectionLong);
                int cy = ChunkSectionPos.unpackY(sectionLong);
                int cz = ChunkSectionPos.unpackZ(sectionLong);
                
                int bx = (cx << 4) + 8;
                int by = (cy << 4) + 8;
                int bz = (cz << 4) + 8;
                client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
                SECTION_LAST_REBUILD.put(sectionLong, now);
                
                iterator.remove();
                processed++;
            }
        }
    }

    public static void clear() {
        STATIC_DISPLAYS.clear();
        ENTITY_ID_TO_POS.clear();
        BOX_CACHE.clear();
        PENDING_SECTION_REBUILDS.clear();
        SECTION_LAST_REBUILD.clear();
        ModelBoundsCache.clear();
    }

    public static void onResourceReload() {
        clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof IBakedDisplay baked) {
                    baked.skyblockm$setBaked(false);
                }
            }
            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }
        }
    }

    public static List<BakedEntityInfo> getStaticDisplaysAt(BlockPos pos) {
        return STATIC_DISPLAYS.get(pos);
    }
    
    public static boolean isBaked(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            return baked.skyblockm$isBaked();
        }
        return false;
    }
}
