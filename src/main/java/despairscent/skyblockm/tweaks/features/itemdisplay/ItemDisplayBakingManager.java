package despairscent.skyblockm.tweaks.features.itemdisplay;
import despairscent.skyblockm.tweaks.ModUtils;

//? if >=1.21.11 {

import despairscent.skyblockm.tweaks.mixininner.IItemRenderStateAccessor;
import despairscent.skyblockm.tweaks.mixininner.ILayerRenderStateAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
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

                                    BlockRenderLayer defaultLayer = BlockRenderLayer.CUTOUT;
                                    if (stack.getItem() instanceof BlockItem blockItem && !stack.isOf(net.minecraft.item.Items.BARRIER)) {
                                        BlockRenderLayer blockLayer = BlockRenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                        if (blockLayer == BlockRenderLayer.TRANSLUCENT) {
                                            defaultLayer = BlockRenderLayer.TRANSLUCENT;
                                        }
                                    }
                                    int[] tints = layerAccessor.skyblockm$getTints();
                                    List<BakedQuad> layerQuads = layer.getQuads();
                                    if (layerQuads != null) {
                                        for (BakedQuad quad : layerQuads) {
                                            BlockRenderLayer quadRenderLayer = defaultLayer;
                                            if (quadRenderLayer != BlockRenderLayer.TRANSLUCENT && isSpriteTranslucent(quad.sprite())) {
                                                quadRenderLayer = BlockRenderLayer.TRANSLUCENT;
                                            }
                                            int tintColor = -1;
                                            if (quad.hasTint() && quad.tintIndex() >= 0 && tints != null && quad.tintIndex() < tints.length) {
                                                tintColor = tints[quad.tintIndex()];
                                            }
                                            bakedQuads.add(new BakedQuadInfo(quad, layerMatrix, quadRenderLayer, tintColor));
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

        public static boolean isSpriteTranslucent(net.minecraft.client.texture.Sprite sprite) {
            return SpriteTranslucencyCache.isTranslucent(sprite);
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
    private static final java.util.Set<Integer> PENDING_CULLED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Integer, BoxCacheEntry> BOX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> PENDING_SECTION_REBUILDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> SECTION_LAST_REBUILD = new ConcurrentHashMap<>();
    private static long LAST_PLAYER_CHUNK_REBUILD = 0L;

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
        if (display.getEntityWorld() == null) {
            return false;
        }
        BlockPos pos = display.getBlockPos();
        if (!display.getEntityWorld().getBlockState(pos).isOf(net.minecraft.block.Blocks.BARRIER)) {
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
        if (d.itemStack().getItem() instanceof BlockItem blockItem && !d.itemStack().isOf(net.minecraft.item.Items.BARRIER)) {
            BlockRenderLayer blockLayer = BlockRenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
            if (blockLayer == BlockRenderLayer.TRANSLUCENT) {
                return false;
            }
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
                List<BakedQuad> layerQuads = layers[l].getQuads();
                if (layerQuads != null) {
                    for (BakedQuad quad : layerQuads) {
                        if (SpriteTranslucencyCache.isTranslucent(quad.sprite())) {
                            return false;
                        }
                    }
                }
            }
        }

        Box box = getCachedBox(display);
        if (box == null) return false;
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return false;
        }
        if (sizeX > 1.05 || sizeY > 1.05 || sizeZ > 1.05) {
            if (itemRenderState instanceof IItemRenderStateAccessor itemAccessor) {
                int layerCount = itemAccessor.skyblockm$getLayerCount();
                ItemRenderState.LayerRenderState[] layers = itemAccessor.skyblockm$getLayers();
                for (int l = 0; l < layerCount && l < layers.length; l++) {
                    List<BakedQuad> layerQuads = layers[l].getQuads();
                    if (layerQuads != null) {
                        for (BakedQuad quad : layerQuads) {
                            if (quad.sprite() != null && quad.sprite().getContents() != null) {
                                String path = quad.sprite().getContents().getId().getPath().toLowerCase();
                                if (path.contains("completed") || path.contains("overlay")) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
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
                    if (PENDING_CULLED_ENTITIES.contains(display.getId())) {
                        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                            return;
                        }
                        PENDING_CULLED_ENTITIES.remove(display.getId());
                        markSectionDirty(pos);
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
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
            PENDING_CULLED_ENTITIES.add(display.getId());
            return;
        }
        PENDING_CULLED_ENTITIES.remove(display.getId());
        markSectionDirty(pos);
    }

    public static void markSectionDirty(BlockPos pos) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null) return;
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        
        long now = System.currentTimeMillis();
        long settleDelayMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkSettleTime * 1000.0));
        long cooldownMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkRebuildCooldown * 1000.0));
        
        long lastRebuild = SECTION_LAST_REBUILD.getOrDefault(sectionLong, 0L);
        long targetTime = Math.max(now + settleDelayMs, lastRebuild + cooldownMs);
        
        Long existingTarget = PENDING_SECTION_REBUILDS.get(sectionLong);
        if (existingTarget != null) {
            targetTime = Math.min(targetTime, existingTarget);
        }
        
        PENDING_SECTION_REBUILDS.put(sectionLong, targetTime);
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(false);
        }
        int id = display.getId();
        PENDING_CULLED_ENTITIES.remove(id);
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
        PENDING_CULLED_ENTITIES.remove(entityId);
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
                MinecraftClient client = MinecraftClient.getInstance();
                for (BakedEntityInfo info : list) {
                    ENTITY_ID_TO_POS.remove(info.entityId);
                    PENDING_CULLED_ENTITIES.remove(info.entityId);
                    if (client != null && client.world != null) {
                        net.minecraft.entity.Entity e = client.world.getEntityById(info.entityId);
                        if (e instanceof IBakedDisplay baked) {
                            baked.skyblockm$setBaked(false);
                        }
                    }
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void fullRebuildSection(int cx, int cy, int cz) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) return;

        STATIC_DISPLAYS.keySet().removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getY() >> 4) == cy && (pos.getZ() >> 4) == cz);

        Box sectionBox = new Box(cx << 4, cy << 4, cz << 4, (cx + 1) << 4, (cy + 1) << 4, (cz + 1) << 4);
        List<DisplayEntity.ItemDisplayEntity> entities = client.world.getEntitiesByClass(
                DisplayEntity.ItemDisplayEntity.class, sectionBox, e -> true
        );

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            PENDING_CULLED_ENTITIES.remove(display.getId());
            if (display instanceof IBakedDisplay baked) {
                baked.skyblockm$setBaked(false);
            }
        }

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            updateEntity(display);
        }

        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        int bx = (cx << 4) + 8;
        int by = (cy << 4) + 8;
        int bz = (cz << 4) + 8;
        client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
        SECTION_LAST_REBUILD.put(sectionLong, System.currentTimeMillis());
        PENDING_SECTION_REBUILDS.remove(sectionLong);
    }

    public static void tickRebuilds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;

        long now = System.currentTimeMillis();

        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled
                && CONFIG.itemDisplayBaking.playerChunkRebuildInterval > 0 && client.player != null && client.world != null) {
            long intervalMs = (long) (CONFIG.itemDisplayBaking.playerChunkRebuildInterval * 1000.0);
            if (now - LAST_PLAYER_CHUNK_REBUILD >= intervalMs) {
                LAST_PLAYER_CHUNK_REBUILD = now;
                int px = ((int) Math.floor(client.player.getX())) >> 4;
                int py = ((int) Math.floor(client.player.getY())) >> 4;
                int pz = ((int) Math.floor(client.player.getZ())) >> 4;
                int minY = client.world.getBottomY() >> 4;
                int maxY = (client.world.getBottomY() + client.world.getHeight()) >> 4;
                for (int dy = -1; dy <= 1; dy++) {
                    int cy = py + dy;
                    if (cy >= minY && cy <= maxY) {
                        fullRebuildSection(px, cy, pz);
                    }
                }
            }
        }

        if (!PENDING_CULLED_ENTITIES.isEmpty() && client.world != null) {
            var culledIterator = PENDING_CULLED_ENTITIES.iterator();
            while (culledIterator.hasNext()) {
                int id = culledIterator.next();
                net.minecraft.entity.Entity entity = client.world.getEntityById(id);
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    if (display.isRemoved()) {
                        culledIterator.remove();
                    } else if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.entityCullingIntegration
                            || !EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                        culledIterator.remove();
                        BlockPos pos = ENTITY_ID_TO_POS.get(id);
                        if (pos == null) pos = display.getBlockPos();
                        markSectionDirty(pos);
                    }
                } else {
                    culledIterator.remove();
                }
            }
        }

        if (PENDING_SECTION_REBUILDS.isEmpty()) return;

        java.util.List<Long> readySections = new java.util.ArrayList<>();
        for (var entry : PENDING_SECTION_REBUILDS.entrySet()) {
            if (now >= entry.getValue()) {
                readySections.add(entry.getKey());
            }
        }
        if (readySections.isEmpty()) return;

        if (client.player != null && readySections.size() > 1) {
            int px = ((int) Math.floor(client.player.getX())) >> 4;
            int py = ((int) Math.floor(client.player.getY())) >> 4;
            int pz = ((int) Math.floor(client.player.getZ())) >> 4;

            readySections.sort((s1, s2) -> {
                int x1 = ChunkSectionPos.unpackX(s1) - px;
                int y1 = ChunkSectionPos.unpackY(s1) - py;
                int z1 = ChunkSectionPos.unpackZ(s1) - pz;
                long dist1 = (long) x1 * x1 + (long) y1 * y1 + (long) z1 * z1;

                int x2 = ChunkSectionPos.unpackX(s2) - px;
                int y2 = ChunkSectionPos.unpackY(s2) - py;
                int z2 = ChunkSectionPos.unpackZ(s2) - pz;
                long dist2 = (long) x2 * x2 + (long) y2 * y2 + (long) z2 * z2;

                return Long.compare(dist1, dist2);
            });
        }

        int processed = 0;
        int maxPerFrame = (CONFIG != null && CONFIG.itemDisplayBaking != null) ? Math.max(1, CONFIG.itemDisplayBaking.maxRebuildsPerFrame) : 8;
        for (long sectionLong : readySections) {
            if (processed >= maxPerFrame) break;

            int cx = ChunkSectionPos.unpackX(sectionLong);
            int cy = ChunkSectionPos.unpackY(sectionLong);
            int cz = ChunkSectionPos.unpackZ(sectionLong);

            int bx = (cx << 4) + 8;
            int by = (cy << 4) + 8;
            int bz = (cz << 4) + 8;
            client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
            SECTION_LAST_REBUILD.put(sectionLong, now);
            PENDING_SECTION_REBUILDS.remove(sectionLong);

            if (!PENDING_CULLED_ENTITIES.isEmpty()) {
                PENDING_CULLED_ENTITIES.removeIf(id -> {
                    BlockPos p = ENTITY_ID_TO_POS.get(id);
                    if (p != null) {
                        return (p.getX() >> 4) == cx && (p.getY() >> 4) == cy && (p.getZ() >> 4) == cz;
                    }
                    return false;
                });
            }

            processed++;
        }
    }

    public static void clear() {
        STATIC_DISPLAYS.clear();
        ENTITY_ID_TO_POS.clear();
        PENDING_CULLED_ENTITIES.clear();
        BOX_CACHE.clear();
        PENDING_SECTION_REBUILDS.clear();
        SECTION_LAST_REBUILD.clear();
        LAST_PLAYER_CHUNK_REBUILD = 0L;
        ModelBoundsCache.clear();
        SpriteTranslucencyCache.clear();
    }

    public static void onChunkReload() {
        clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof IBakedDisplay baked) {
                    baked.skyblockm$setBaked(false);
                }
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    updateEntity(display);
                }
            }
        }
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
//?} elif =1.21.8 {
/*
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

                                    int[] tints = layerAccessor.skyblockm$getTints();
                                    List<BakedQuad> layerQuads = layer.getQuads();
                                    if (layerQuads != null) {
                                        for (BakedQuad quad : layerQuads) {
                                            BlockRenderLayer quadRenderLayer = BlockRenderLayer.CUTOUT_MIPPED;
                                            if (isSpriteTranslucent(quad.sprite())) {
                                                quadRenderLayer = BlockRenderLayer.TRANSLUCENT;
                                            } else if (stack.getItem() instanceof BlockItem blockItem && !stack.isOf(net.minecraft.item.Items.BARRIER)) {
                                                BlockRenderLayer blockLayer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
                                                if (blockLayer == BlockRenderLayer.TRANSLUCENT) {
                                                    quadRenderLayer = BlockRenderLayer.TRANSLUCENT;
                                                }
                                            }

                                            int tintColor = -1;
                                            if (quad.hasTint() && quad.tintIndex() >= 0 && tints != null && quad.tintIndex() < tints.length) {
                                                tintColor = tints[quad.tintIndex()];
                                            }
                                            bakedQuads.add(new BakedQuadInfo(quad, layerMatrix, quadRenderLayer, tintColor));
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

        public static boolean isSpriteTranslucent(net.minecraft.client.texture.Sprite sprite) {
            return SpriteTranslucencyCache.isTranslucent(sprite);
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
    private static final java.util.Set<Integer> PENDING_CULLED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Integer, BoxCacheEntry> BOX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> PENDING_SECTION_REBUILDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> SECTION_LAST_REBUILD = new ConcurrentHashMap<>();
    private static long LAST_PLAYER_CHUNK_REBUILD = 0L;

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
        if (d.itemStack().getItem() instanceof BlockItem blockItem && !d.itemStack().isOf(net.minecraft.item.Items.BARRIER)) {
            BlockRenderLayer blockLayer = RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState());
            if (blockLayer == BlockRenderLayer.TRANSLUCENT) {
                return false;
            }
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
                List<BakedQuad> layerQuads = layers[l].getQuads();
                if (layerQuads != null) {
                    for (BakedQuad quad : layerQuads) {
                        if (SpriteTranslucencyCache.isTranslucent(quad.sprite())) {
                            return false;
                        }
                    }
                }
            }
        }

        Box box = getCachedBox(display);
        if (box == null) return false;
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return false;
        }
        if (sizeX > 1.05 || sizeY > 1.05 || sizeZ > 1.05) {
            if (itemRenderState instanceof IItemRenderStateAccessor itemAccessor) {
                int layerCount = itemAccessor.skyblockm$getLayerCount();
                ItemRenderState.LayerRenderState[] layers = itemAccessor.skyblockm$getLayers();
                for (int l = 0; l < layerCount && l < layers.length; l++) {
                    List<BakedQuad> layerQuads = layers[l].getQuads();
                    if (layerQuads != null) {
                        for (BakedQuad quad : layerQuads) {
                            if (quad.sprite() != null && quad.sprite().getContents() != null) {
                                String path = quad.sprite().getContents().getId().getPath().toLowerCase();
                                if (path.contains("completed") || path.contains("overlay")) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
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
                    if (PENDING_CULLED_ENTITIES.contains(display.getId())) {
                        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                            return;
                        }
                        PENDING_CULLED_ENTITIES.remove(display.getId());
                        markSectionDirty(pos);
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
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
            PENDING_CULLED_ENTITIES.add(display.getId());
            return;
        }
        PENDING_CULLED_ENTITIES.remove(display.getId());
        markSectionDirty(pos);
    }

    public static void markSectionDirty(BlockPos pos) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null) return;
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        
        long now = System.currentTimeMillis();
        long settleDelayMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkSettleTime * 1000.0));
        long cooldownMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkRebuildCooldown * 1000.0));
        
        long lastRebuild = SECTION_LAST_REBUILD.getOrDefault(sectionLong, 0L);
        long targetTime = Math.max(now + settleDelayMs, lastRebuild + cooldownMs);
        
        Long existingTarget = PENDING_SECTION_REBUILDS.get(sectionLong);
        if (existingTarget != null) {
            targetTime = Math.min(targetTime, existingTarget);
        }
        
        PENDING_SECTION_REBUILDS.put(sectionLong, targetTime);
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(false);
        }
        int id = display.getId();
        PENDING_CULLED_ENTITIES.remove(id);
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
        PENDING_CULLED_ENTITIES.remove(entityId);
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
                MinecraftClient client = MinecraftClient.getInstance();
                for (BakedEntityInfo info : list) {
                    ENTITY_ID_TO_POS.remove(info.entityId);
                    PENDING_CULLED_ENTITIES.remove(info.entityId);
                    if (client != null && client.world != null) {
                        net.minecraft.entity.Entity e = client.world.getEntityById(info.entityId);
                        if (e instanceof IBakedDisplay baked) {
                            baked.skyblockm$setBaked(false);
                        }
                    }
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void fullRebuildSection(int cx, int cy, int cz) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) return;

        STATIC_DISPLAYS.keySet().removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getY() >> 4) == cy && (pos.getZ() >> 4) == cz);

        Box sectionBox = new Box(cx << 4, cy << 4, cz << 4, (cx + 1) << 4, (cy + 1) << 4, (cz + 1) << 4);
        List<DisplayEntity.ItemDisplayEntity> entities = client.world.getEntitiesByClass(
                DisplayEntity.ItemDisplayEntity.class, sectionBox, e -> true
        );

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            PENDING_CULLED_ENTITIES.remove(display.getId());
            if (display instanceof IBakedDisplay baked) {
                baked.skyblockm$setBaked(false);
            }
        }

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            updateEntity(display);
        }

        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        int bx = (cx << 4) + 8;
        int by = (cy << 4) + 8;
        int bz = (cz << 4) + 8;
        client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
        SECTION_LAST_REBUILD.put(sectionLong, System.currentTimeMillis());
        PENDING_SECTION_REBUILDS.remove(sectionLong);
    }

    public static void tickRebuilds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;

        long now = System.currentTimeMillis();

        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled
                && CONFIG.itemDisplayBaking.playerChunkRebuildInterval > 0 && client.player != null && client.world != null) {
            long intervalMs = (long) (CONFIG.itemDisplayBaking.playerChunkRebuildInterval * 1000.0);
            if (now - LAST_PLAYER_CHUNK_REBUILD >= intervalMs) {
                LAST_PLAYER_CHUNK_REBUILD = now;
                int px = ((int) Math.floor(client.player.getX())) >> 4;
                int py = ((int) Math.floor(client.player.getY())) >> 4;
                int pz = ((int) Math.floor(client.player.getZ())) >> 4;
                int minY = client.world.getBottomY() >> 4;
                int maxY = (client.world.getBottomY() + client.world.getHeight()) >> 4;
                for (int dy = -1; dy <= 1; dy++) {
                    int cy = py + dy;
                    if (cy >= minY && cy <= maxY) {
                        fullRebuildSection(px, cy, pz);
                    }
                }
            }
        }

        if (!PENDING_CULLED_ENTITIES.isEmpty() && client.world != null) {
            var culledIterator = PENDING_CULLED_ENTITIES.iterator();
            while (culledIterator.hasNext()) {
                int id = culledIterator.next();
                net.minecraft.entity.Entity entity = client.world.getEntityById(id);
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    if (display.isRemoved()) {
                        culledIterator.remove();
                    } else if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.entityCullingIntegration
                            || !EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                        culledIterator.remove();
                        BlockPos pos = ENTITY_ID_TO_POS.get(id);
                        if (pos == null) pos = display.getBlockPos();
                        markSectionDirty(pos);
                    }
                } else {
                    culledIterator.remove();
                }
            }
        }

        if (PENDING_SECTION_REBUILDS.isEmpty()) return;

        java.util.List<Long> readySections = new java.util.ArrayList<>();
        for (var entry : PENDING_SECTION_REBUILDS.entrySet()) {
            if (now >= entry.getValue()) {
                readySections.add(entry.getKey());
            }
        }
        if (readySections.isEmpty()) return;

        if (client.player != null && readySections.size() > 1) {
            int px = ((int) Math.floor(client.player.getX())) >> 4;
            int py = ((int) Math.floor(client.player.getY())) >> 4;
            int pz = ((int) Math.floor(client.player.getZ())) >> 4;

            readySections.sort((s1, s2) -> {
                int x1 = ChunkSectionPos.unpackX(s1) - px;
                int y1 = ChunkSectionPos.unpackY(s1) - py;
                int z1 = ChunkSectionPos.unpackZ(s1) - pz;
                long dist1 = (long) x1 * x1 + (long) y1 * y1 + (long) z1 * z1;

                int x2 = ChunkSectionPos.unpackX(s2) - px;
                int y2 = ChunkSectionPos.unpackY(s2) - py;
                int z2 = ChunkSectionPos.unpackZ(s2) - pz;
                long dist2 = (long) x2 * x2 + (long) y2 * y2 + (long) z2 * z2;

                return Long.compare(dist1, dist2);
            });
        }

        int processed = 0;
        int maxPerFrame = (CONFIG != null && CONFIG.itemDisplayBaking != null) ? Math.max(1, CONFIG.itemDisplayBaking.maxRebuildsPerFrame) : 8;
        for (long sectionLong : readySections) {
            if (processed >= maxPerFrame) break;

            int cx = ChunkSectionPos.unpackX(sectionLong);
            int cy = ChunkSectionPos.unpackY(sectionLong);
            int cz = ChunkSectionPos.unpackZ(sectionLong);

            int bx = (cx << 4) + 8;
            int by = (cy << 4) + 8;
            int bz = (cz << 4) + 8;
            client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
            SECTION_LAST_REBUILD.put(sectionLong, now);
            PENDING_SECTION_REBUILDS.remove(sectionLong);

            if (!PENDING_CULLED_ENTITIES.isEmpty()) {
                PENDING_CULLED_ENTITIES.removeIf(id -> {
                    BlockPos p = ENTITY_ID_TO_POS.get(id);
                    if (p != null) {
                        return (p.getX() >> 4) == cx && (p.getY() >> 4) == cy && (p.getZ() >> 4) == cz;
                    }
                    return false;
                });
            }

            processed++;
        }
    }

    public static void clear() {
        STATIC_DISPLAYS.clear();
        ENTITY_ID_TO_POS.clear();
        PENDING_CULLED_ENTITIES.clear();
        BOX_CACHE.clear();
        PENDING_SECTION_REBUILDS.clear();
        SECTION_LAST_REBUILD.clear();
        LAST_PLAYER_CHUNK_REBUILD = 0L;
        ModelBoundsCache.clear();
        SpriteTranslucencyCache.clear();
    }

    public static void onChunkReload() {
        clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof IBakedDisplay baked) {
                    baked.skyblockm$setBaked(false);
                }
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    updateEntity(display);
                }
            }
        }
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
*///?} elif >=1.21 {
/*import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ItemDisplayBakingManager {
    public static class BakedEntityInfo {
        public final int entityId;
        public final BlockPos pos;
        public final ItemStack itemStack;
        public final BakedModel itemModel;
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
            BakedModel model = null;
            Object transform = null;
            try {
                var d = display.getData();
                if (d != null && !d.itemStack().isEmpty()) {
                    stack = d.itemStack().copy();
                    transform = d.itemTransform();
                    model = MinecraftClient.getInstance().getItemRenderer().getModel(stack, null, null, 0);
                }
            } catch (Exception e) {
                // ignore
            }
            this.itemStack = stack;
            this.itemModel = model;
            this.itemTransform = transform;

            net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
            try {
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
                
                var d = display.getData();
                if (d != null && model != null) {
                    model.getTransformation().getTransformation(d.itemTransform()).apply(false, ms);
                }
                
                ms.translate(-0.5f, -0.5f, -0.5f);
            } catch (Exception e) {
                // ignore
            }
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
    private static final java.util.Set<Integer> PENDING_CULLED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Integer, BoxCacheEntry> BOX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> PENDING_SECTION_REBUILDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> SECTION_LAST_REBUILD = new ConcurrentHashMap<>();
    private static long LAST_PLAYER_CHUNK_REBUILD = 0L;

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
        if (d.itemStack().getItem() instanceof BlockItem blockItem && !d.itemStack().isOf(net.minecraft.item.Items.BARRIER)) {
            if (RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState()) == RenderLayer.getTranslucent()) {
                return false;
            }
        }
        BakedModel model = MinecraftClient.getInstance().getItemRenderer().getModel(d.itemStack(), null, null, 0);
        if (model == null || model.isBuiltin()) {
            return false;
        }

        try {
            net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(42L);
            Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : dirs) {
                List<BakedQuad> quads = model.getQuads(null, dir, random);
                if (quads != null) {
                    for (BakedQuad quad : quads) {
                        if (SpriteTranslucencyCache.isTranslucent(quad.getSprite())) {
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        Box box = getCachedBox(display);
        if (box == null) return false;
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return false;
        }
        if (sizeX > 1.05 || sizeY > 1.05 || sizeZ > 1.05) {
            try {
                net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(42L);
                Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction dir : dirs) {
                    List<BakedQuad> quads = model.getQuads(null, dir, random);
                    if (quads != null) {
                        for (BakedQuad quad : quads) {
                            if (quad.getSprite() != null && quad.getSprite().getContents() != null) {
                                String path = quad.getSprite().getContents().getId().getPath().toLowerCase();
                                if (path.contains("completed") || path.contains("overlay")) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return true;
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
                    // Already baked with matching model and transform!
                    if (display instanceof IBakedDisplay baked) {
                        baked.skyblockm$setBaked(true);
                    }
                    if (PENDING_CULLED_ENTITIES.contains(display.getId())) {
                        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                            return;
                        }
                        PENDING_CULLED_ENTITIES.remove(display.getId());
                        markSectionDirty(pos);
                    }
                    return;
                } else {
                    // Visual changed (e.g. wire connected): replace
                    list.remove(info);
                    break;
                }
            }
        }

        list.add(new BakedEntityInfo(display, pos));
        ENTITY_ID_TO_POS.put(display.getId(), pos);
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(true);
        }
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
            PENDING_CULLED_ENTITIES.add(display.getId());
            return;
        }
        PENDING_CULLED_ENTITIES.remove(display.getId());
        markSectionDirty(pos);
    }

    public static void onEntityDataChanged(DisplayEntity.ItemDisplayEntity display) {
        updateEntity(display);
    }

    public static void markSectionDirty(BlockPos pos) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null) return;
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        
        long now = System.currentTimeMillis();
        long settleDelayMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkSettleTime * 1000.0));
        long cooldownMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkRebuildCooldown * 1000.0));
        
        long lastRebuild = SECTION_LAST_REBUILD.getOrDefault(sectionLong, 0L);
        long targetTime = Math.max(now + settleDelayMs, lastRebuild + cooldownMs);
        
        Long existingTarget = PENDING_SECTION_REBUILDS.get(sectionLong);
        if (existingTarget != null) {
            targetTime = Math.min(targetTime, existingTarget);
        }
        
        PENDING_SECTION_REBUILDS.put(sectionLong, targetTime);
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(false);
        }
        int id = display.getId();
        PENDING_CULLED_ENTITIES.remove(id);
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
        PENDING_CULLED_ENTITIES.remove(entityId);
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

    public static void onBlockChanged(BlockPos pos, BlockState newState) {
        if (!newState.isOf(net.minecraft.block.Blocks.BARRIER)) {
            CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.remove(pos);
            if (list != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                for (BakedEntityInfo info : list) {
                    ENTITY_ID_TO_POS.remove(info.entityId);
                    PENDING_CULLED_ENTITIES.remove(info.entityId);
                    if (client != null && client.world != null) {
                        net.minecraft.entity.Entity e = client.world.getEntityById(info.entityId);
                        if (e instanceof IBakedDisplay baked) {
                            baked.skyblockm$setBaked(false);
                        }
                    }
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void fullRebuildSection(int cx, int cy, int cz) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) return;

        STATIC_DISPLAYS.keySet().removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getY() >> 4) == cy && (pos.getZ() >> 4) == cz);

        Box sectionBox = new Box(cx << 4, cy << 4, cz << 4, (cx + 1) << 4, (cy + 1) << 4, (cz + 1) << 4);
        List<DisplayEntity.ItemDisplayEntity> entities = client.world.getEntitiesByClass(
                DisplayEntity.ItemDisplayEntity.class, sectionBox, e -> true
        );

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            PENDING_CULLED_ENTITIES.remove(display.getId());
            if (display instanceof IBakedDisplay baked) {
                baked.skyblockm$setBaked(false);
            }
        }

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            updateEntity(display);
        }

        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        int bx = (cx << 4) + 8;
        int by = (cy << 4) + 8;
        int bz = (cz << 4) + 8;
        client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
        SECTION_LAST_REBUILD.put(sectionLong, System.currentTimeMillis());
        PENDING_SECTION_REBUILDS.remove(sectionLong);
    }

    public static void tickRebuilds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;

        long now = System.currentTimeMillis();

        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled
                && CONFIG.itemDisplayBaking.playerChunkRebuildInterval > 0 && client.player != null && client.world != null) {
            long intervalMs = (long) (CONFIG.itemDisplayBaking.playerChunkRebuildInterval * 1000.0);
            if (now - LAST_PLAYER_CHUNK_REBUILD >= intervalMs) {
                LAST_PLAYER_CHUNK_REBUILD = now;
                int px = ((int) Math.floor(client.player.getX())) >> 4;
                int py = ((int) Math.floor(client.player.getY())) >> 4;
                int pz = ((int) Math.floor(client.player.getZ())) >> 4;
                int minY = client.world.getBottomY() >> 4;
                int maxY = (client.world.getBottomY() + client.world.getHeight()) >> 4;
                for (int dy = -1; dy <= 1; dy++) {
                    int cy = py + dy;
                    if (cy >= minY && cy <= maxY) {
                        fullRebuildSection(px, cy, pz);
                    }
                }
            }
        }

        if (!PENDING_CULLED_ENTITIES.isEmpty() && client.world != null) {
            var culledIterator = PENDING_CULLED_ENTITIES.iterator();
            while (culledIterator.hasNext()) {
                int id = culledIterator.next();
                net.minecraft.entity.Entity entity = client.world.getEntityById(id);
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    if (display.isRemoved()) {
                        culledIterator.remove();
                    } else if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.entityCullingIntegration
                            || !EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                        culledIterator.remove();
                        BlockPos pos = ENTITY_ID_TO_POS.get(id);
                        if (pos == null) pos = display.getBlockPos();
                        markSectionDirty(pos);
                    }
                } else {
                    culledIterator.remove();
                }
            }
        }

        if (PENDING_SECTION_REBUILDS.isEmpty()) return;

        java.util.List<Long> readySections = new java.util.ArrayList<>();
        for (var entry : PENDING_SECTION_REBUILDS.entrySet()) {
            if (now >= entry.getValue()) {
                readySections.add(entry.getKey());
            }
        }
        if (readySections.isEmpty()) return;

        if (client.player != null && readySections.size() > 1) {
            int px = ((int) Math.floor(client.player.getX())) >> 4;
            int py = ((int) Math.floor(client.player.getY())) >> 4;
            int pz = ((int) Math.floor(client.player.getZ())) >> 4;

            readySections.sort((s1, s2) -> {
                int x1 = ChunkSectionPos.unpackX(s1) - px;
                int y1 = ChunkSectionPos.unpackY(s1) - py;
                int z1 = ChunkSectionPos.unpackZ(s1) - pz;
                long dist1 = (long) x1 * x1 + (long) y1 * y1 + (long) z1 * z1;

                int x2 = ChunkSectionPos.unpackX(s2) - px;
                int y2 = ChunkSectionPos.unpackY(s2) - py;
                int z2 = ChunkSectionPos.unpackZ(s2) - pz;
                long dist2 = (long) x2 * x2 + (long) y2 * y2 + (long) z2 * z2;

                return Long.compare(dist1, dist2);
            });
        }

        int processed = 0;
        int maxPerFrame = (CONFIG != null && CONFIG.itemDisplayBaking != null) ? Math.max(1, CONFIG.itemDisplayBaking.maxRebuildsPerFrame) : 8;
        for (long sectionLong : readySections) {
            if (processed >= maxPerFrame) break;

            int cx = ChunkSectionPos.unpackX(sectionLong);
            int cy = ChunkSectionPos.unpackY(sectionLong);
            int cz = ChunkSectionPos.unpackZ(sectionLong);

            int bx = (cx << 4) + 8;
            int by = (cy << 4) + 8;
            int bz = (cz << 4) + 8;
            client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
            SECTION_LAST_REBUILD.put(sectionLong, now);
            PENDING_SECTION_REBUILDS.remove(sectionLong);

            if (!PENDING_CULLED_ENTITIES.isEmpty()) {
                PENDING_CULLED_ENTITIES.removeIf(id -> {
                    BlockPos p = ENTITY_ID_TO_POS.get(id);
                    if (p != null) {
                        return (p.getX() >> 4) == cx && (p.getY() >> 4) == cy && (p.getZ() >> 4) == cz;
                    }
                    return false;
                });
            }

            processed++;
        }
    }

    public static void clear() {
        STATIC_DISPLAYS.clear();
        ENTITY_ID_TO_POS.clear();
        PENDING_CULLED_ENTITIES.clear();
        BOX_CACHE.clear();
        PENDING_SECTION_REBUILDS.clear();
        SECTION_LAST_REBUILD.clear();
        LAST_PLAYER_CHUNK_REBUILD = 0L;
        ModelBoundsCache.clear();
        SpriteTranslucencyCache.clear();
    }

    public static void onChunkReload() {
        clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof IBakedDisplay baked) {
                    baked.skyblockm$setBaked(false);
                }
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    updateEntity(display);
                }
            }
        }
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
*///?} else {

/*import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ItemDisplayBakingManager {
    public static class BakedEntityInfo {
        public final int entityId;
        public final BlockPos pos;
        public final ItemStack itemStack;
        public final BakedModel itemModel;
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
            BakedModel model = null;
            Object transform = null;
            try {
                var d = display.getData();
                if (d != null && !d.itemStack().isEmpty()) {
                    stack = d.itemStack().copy();
                    transform = d.itemTransform();
                    model = MinecraftClient.getInstance().getItemRenderer().getModel(stack, null, null, 0);
                }
            } catch (Exception e) {
                // ignore
            }
            this.itemStack = stack;
            this.itemModel = model;
            this.itemTransform = transform;

            net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
            try {
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
                
                var d = display.getData();
                if (d != null && model != null) {
                    model.getTransformation().getTransformation(d.itemTransform()).apply(false, ms);
                }
                
                ms.translate(-0.5f, -0.5f, -0.5f);
            } catch (Exception e) {
                // ignore
            }
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
    private static final java.util.Set<Integer> PENDING_CULLED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Integer, BoxCacheEntry> BOX_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> PENDING_SECTION_REBUILDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> SECTION_LAST_REBUILD = new ConcurrentHashMap<>();
    private static long LAST_PLAYER_CHUNK_REBUILD = 0L;

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
        if (d.itemStack().getItem() instanceof BlockItem blockItem && !d.itemStack().isOf(net.minecraft.item.Items.BARRIER)) {
            if (RenderLayers.getBlockLayer(blockItem.getBlock().getDefaultState()) == RenderLayer.getTranslucent()) {
                return false;
            }
        }
        BakedModel model = MinecraftClient.getInstance().getItemRenderer().getModel(d.itemStack(), null, null, 0);
        if (model == null || model.isBuiltin()) {
            return false;
        }

        try {
            net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(42L);
            Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : dirs) {
                List<BakedQuad> quads = model.getQuads(null, dir, random);
                if (quads != null) {
                    for (BakedQuad quad : quads) {
                        if (SpriteTranslucencyCache.isTranslucent(quad.getSprite())) {
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        Box box = getCachedBox(display);
        if (box == null) return false;
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return false;
        }
        if (sizeX > 1.05 || sizeY > 1.05 || sizeZ > 1.05) {
            try {
                net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(42L);
                Direction[] dirs = new Direction[]{null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction dir : dirs) {
                    List<BakedQuad> quads = model.getQuads(null, dir, random);
                    if (quads != null) {
                        for (BakedQuad quad : quads) {
                            if (quad.getSprite() != null && quad.getSprite().getContents() != null) {
                                String path = quad.getSprite().getContents().getId().getPath().toLowerCase();
                                if (path.contains("completed") || path.contains("overlay")) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return true;
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
                    // Already baked with matching model and transform!
                    if (display instanceof IBakedDisplay baked) {
                        baked.skyblockm$setBaked(true);
                    }
                    if (PENDING_CULLED_ENTITIES.contains(display.getId())) {
                        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                            return;
                        }
                        PENDING_CULLED_ENTITIES.remove(display.getId());
                        markSectionDirty(pos);
                    }
                    return;
                } else {
                    // Visual changed (e.g. wire connected): replace
                    list.remove(info);
                    break;
                }
            }
        }

        list.add(new BakedEntityInfo(display, pos));
        ENTITY_ID_TO_POS.put(display.getId(), pos);
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(true);
        }
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.entityCullingIntegration
                && EntityCullingCompat.isCulledOrOutOfCamera(display)) {
            PENDING_CULLED_ENTITIES.add(display.getId());
            return;
        }
        PENDING_CULLED_ENTITIES.remove(display.getId());
        markSectionDirty(pos);
    }

    public static void onEntityDataChanged(DisplayEntity.ItemDisplayEntity display) {
        updateEntity(display);
    }

    public static void markSectionDirty(BlockPos pos) {
        if (CONFIG == null || CONFIG.itemDisplayBaking == null) return;
        int cx = pos.getX() >> 4;
        int cy = pos.getY() >> 4;
        int cz = pos.getZ() >> 4;
        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        
        long now = System.currentTimeMillis();
        long settleDelayMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkSettleTime * 1000.0));
        long cooldownMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkRebuildCooldown * 1000.0));
        
        long lastRebuild = SECTION_LAST_REBUILD.getOrDefault(sectionLong, 0L);
        long targetTime = Math.max(now + settleDelayMs, lastRebuild + cooldownMs);
        
        Long existingTarget = PENDING_SECTION_REBUILDS.get(sectionLong);
        if (existingTarget != null) {
            targetTime = Math.min(targetTime, existingTarget);
        }
        
        PENDING_SECTION_REBUILDS.put(sectionLong, targetTime);
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        if (display instanceof IBakedDisplay baked) {
            baked.skyblockm$setBaked(false);
        }
        int id = display.getId();
        PENDING_CULLED_ENTITIES.remove(id);
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
        PENDING_CULLED_ENTITIES.remove(entityId);
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

    public static void onBlockChanged(BlockPos pos, BlockState newState) {
        if (!newState.isOf(net.minecraft.block.Blocks.BARRIER)) {
            CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.remove(pos);
            if (list != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                for (BakedEntityInfo info : list) {
                    ENTITY_ID_TO_POS.remove(info.entityId);
                    PENDING_CULLED_ENTITIES.remove(info.entityId);
                    if (client != null && client.world != null) {
                        net.minecraft.entity.Entity e = client.world.getEntityById(info.entityId);
                        if (e instanceof IBakedDisplay baked) {
                            baked.skyblockm$setBaked(false);
                        }
                    }
                }
                markSectionDirty(pos);
            }
        }
    }

    public static void fullRebuildSection(int cx, int cy, int cz) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.worldRenderer == null) return;

        STATIC_DISPLAYS.keySet().removeIf(pos -> (pos.getX() >> 4) == cx && (pos.getY() >> 4) == cy && (pos.getZ() >> 4) == cz);

        Box sectionBox = new Box(cx << 4, cy << 4, cz << 4, (cx + 1) << 4, (cy + 1) << 4, (cz + 1) << 4);
        List<DisplayEntity.ItemDisplayEntity> entities = client.world.getEntitiesByClass(
                DisplayEntity.ItemDisplayEntity.class, sectionBox, e -> true
        );

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            PENDING_CULLED_ENTITIES.remove(display.getId());
            if (display instanceof IBakedDisplay baked) {
                baked.skyblockm$setBaked(false);
            }
        }

        for (DisplayEntity.ItemDisplayEntity display : entities) {
            updateEntity(display);
        }

        long sectionLong = ChunkSectionPos.asLong(cx, cy, cz);
        int bx = (cx << 4) + 8;
        int by = (cy << 4) + 8;
        int bz = (cz << 4) + 8;
        client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
        SECTION_LAST_REBUILD.put(sectionLong, System.currentTimeMillis());
        PENDING_SECTION_REBUILDS.remove(sectionLong);
    }

    public static void tickRebuilds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.worldRenderer == null) return;

        long now = System.currentTimeMillis();

        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled
                && CONFIG.itemDisplayBaking.playerChunkRebuildInterval > 0 && client.player != null && client.world != null) {
            long intervalMs = (long) (CONFIG.itemDisplayBaking.playerChunkRebuildInterval * 1000.0);
            if (now - LAST_PLAYER_CHUNK_REBUILD >= intervalMs) {
                LAST_PLAYER_CHUNK_REBUILD = now;
                int px = ((int) Math.floor(client.player.getX())) >> 4;
                int py = ((int) Math.floor(client.player.getY())) >> 4;
                int pz = ((int) Math.floor(client.player.getZ())) >> 4;
                int minY = client.world.getBottomY() >> 4;
                int maxY = (client.world.getBottomY() + client.world.getHeight()) >> 4;
                for (int dy = -1; dy <= 1; dy++) {
                    int cy = py + dy;
                    if (cy >= minY && cy <= maxY) {
                        fullRebuildSection(px, cy, pz);
                    }
                }
            }
        }

        if (!PENDING_CULLED_ENTITIES.isEmpty() && client.world != null) {
            var culledIterator = PENDING_CULLED_ENTITIES.iterator();
            while (culledIterator.hasNext()) {
                int id = culledIterator.next();
                net.minecraft.entity.Entity entity = client.world.getEntityById(id);
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    if (display.isRemoved()) {
                        culledIterator.remove();
                    } else if (CONFIG == null || CONFIG.itemDisplayBaking == null || !CONFIG.itemDisplayBaking.entityCullingIntegration
                            || !EntityCullingCompat.isCulledOrOutOfCamera(display)) {
                        culledIterator.remove();
                        BlockPos pos = ENTITY_ID_TO_POS.get(id);
                        if (pos == null) pos = display.getBlockPos();
                        markSectionDirty(pos);
                    }
                } else {
                    culledIterator.remove();
                }
            }
        }

        if (PENDING_SECTION_REBUILDS.isEmpty()) return;

        java.util.List<Long> readySections = new java.util.ArrayList<>();
        for (var entry : PENDING_SECTION_REBUILDS.entrySet()) {
            if (now >= entry.getValue()) {
                readySections.add(entry.getKey());
            }
        }
        if (readySections.isEmpty()) return;

        if (client.player != null && readySections.size() > 1) {
            int px = ((int) Math.floor(client.player.getX())) >> 4;
            int py = ((int) Math.floor(client.player.getY())) >> 4;
            int pz = ((int) Math.floor(client.player.getZ())) >> 4;

            readySections.sort((s1, s2) -> {
                int x1 = ChunkSectionPos.unpackX(s1) - px;
                int y1 = ChunkSectionPos.unpackY(s1) - py;
                int z1 = ChunkSectionPos.unpackZ(s1) - pz;
                long dist1 = (long) x1 * x1 + (long) y1 * y1 + (long) z1 * z1;

                int x2 = ChunkSectionPos.unpackX(s2) - px;
                int y2 = ChunkSectionPos.unpackY(s2) - py;
                int z2 = ChunkSectionPos.unpackZ(s2) - pz;
                long dist2 = (long) x2 * x2 + (long) y2 * y2 + (long) z2 * z2;

                return Long.compare(dist1, dist2);
            });
        }

        int processed = 0;
        int maxPerFrame = (CONFIG != null && CONFIG.itemDisplayBaking != null) ? Math.max(1, CONFIG.itemDisplayBaking.maxRebuildsPerFrame) : 8;
        for (long sectionLong : readySections) {
            if (processed >= maxPerFrame) break;

            int cx = ChunkSectionPos.unpackX(sectionLong);
            int cy = ChunkSectionPos.unpackY(sectionLong);
            int cz = ChunkSectionPos.unpackZ(sectionLong);

            int bx = (cx << 4) + 8;
            int by = (cy << 4) + 8;
            int bz = (cz << 4) + 8;
            client.worldRenderer.scheduleBlockRenders(bx, by, bz, bx, by, bz);
            SECTION_LAST_REBUILD.put(sectionLong, now);
            PENDING_SECTION_REBUILDS.remove(sectionLong);

            if (!PENDING_CULLED_ENTITIES.isEmpty()) {
                PENDING_CULLED_ENTITIES.removeIf(id -> {
                    BlockPos p = ENTITY_ID_TO_POS.get(id);
                    if (p != null) {
                        return (p.getX() >> 4) == cx && (p.getY() >> 4) == cy && (p.getZ() >> 4) == cz;
                    }
                    return false;
                });
            }

            processed++;
        }
    }

    public static void clear() {
        STATIC_DISPLAYS.clear();
        ENTITY_ID_TO_POS.clear();
        PENDING_CULLED_ENTITIES.clear();
        BOX_CACHE.clear();
        PENDING_SECTION_REBUILDS.clear();
        SECTION_LAST_REBUILD.clear();
        LAST_PLAYER_CHUNK_REBUILD = 0L;
        ModelBoundsCache.clear();
        SpriteTranslucencyCache.clear();
    }

    public static void onChunkReload() {
        clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity instanceof IBakedDisplay baked) {
                    baked.skyblockm$setBaked(false);
                }
                if (entity instanceof DisplayEntity.ItemDisplayEntity display) {
                    updateEntity(display);
                }
            }
        }
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
*///?}
