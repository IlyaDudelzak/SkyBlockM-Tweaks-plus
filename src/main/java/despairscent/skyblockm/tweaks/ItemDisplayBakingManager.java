package despairscent.skyblockm.tweaks;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
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

        public BakedEntityInfo(DisplayEntity.ItemDisplayEntity display, BlockPos pos) {
            this.entityId = display.getId();
            this.pos = pos;
            this.renderState = display.getRenderState();
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

            Matrix4f m = new Matrix4f();
            try {
                float rx = (float) (display.getX() - pos.getX());
                float ry = (float) (display.getY() - pos.getY());
                float rz = (float) (display.getZ() - pos.getZ());
                m.translate(rx, ry, rz);
                
                m.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-display.getYaw()));
                m.rotate(RotationAxis.POSITIVE_X.rotationDegrees(display.getPitch()));
                
                var rs = display.getRenderState();
                if (rs != null) {
                    m.mul(rs.transformation().interpolate(1.0f).getMatrix());
                }
                
                m.rotate(RotationAxis.POSITIVE_Y.rotation((float)Math.PI));
                
                var d = display.getData();
                if (d != null && model != null) {
                    net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
                    model.getTransformation().getTransformation(d.itemTransform()).apply(false, ms);
                    m.mul(ms.peek().getPositionMatrix());
                }
                
                m.translate(-0.5f, -0.5f, -0.5f);
            } catch (Exception e) {
                // ignore
            }
            this.matrix = m;
        }

        public boolean matches(DisplayEntity.ItemDisplayEntity display) {
            return this.entityId == display.getId();
        }

        public boolean hasSameVisual(DisplayEntity.ItemDisplayEntity display) {
            var d = display.getData();
            if (d == null) return false;
            ItemStack otherStack = d.itemStack();
            if (otherStack.isEmpty()) return false;
            return ItemStack.areEqual(this.itemStack, otherStack) && java.util.Objects.equals(this.itemTransform, d.itemTransform());
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
        if (!CONFIG.itemDisplayBaking.enabled) {
            return false;
        }
        BlockPos pos = display.getBlockPos();
        if (display.getWorld() == null || !display.getWorld().getBlockState(pos).isOf(net.minecraft.block.Blocks.BARRIER)) {
            return false;
        }
        Box box = getCachedBox(display);
        if (box == null) return false;
        double maxSize = CONFIG.itemDisplayBaking.maxBakeSize;
        return (box.maxX - box.minX) <= maxSize && 
               (box.maxY - box.minY) <= maxSize && 
               (box.maxZ - box.minZ) <= maxSize;
    }

    public static boolean shouldHideEntity(DisplayEntity.ItemDisplayEntity display) {
        if (!CONFIG.itemDisplayBaking.enabled) {
            return false;
        }
        if (display instanceof IBakedDisplay baked && baked.skyblockm$isBaked()) {
            return true;
        }
        if (CONFIG.itemDisplayBaking.hideBakeableEntities) {
            return isBakeable(display);
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
        markSectionDirty(pos);
    }

    public static void onEntityDataChanged(DisplayEntity.ItemDisplayEntity display) {
        invalidateCache(display);
        if (!isBakeable(display)) {
            removeEntity(display);
            return;
        }
        BlockPos pos = display.getBlockPos();
        CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.computeIfAbsent(pos, k -> new CopyOnWriteArrayList<>());
        BakedEntityInfo existing = null;
        for (BakedEntityInfo info : list) {
            if (info.matches(display)) {
                existing = info;
                break;
            }
        }

        if (existing != null && existing.hasSameVisual(display)) {
            if (display instanceof IBakedDisplay baked) {
                baked.skyblockm$setBaked(true);
            }
            return;
        }

        if (existing != null) {
            list.remove(existing);
        }

        list.add(new BakedEntityInfo(display, pos));
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
        long cooldownMs = Math.max(0, (long) (CONFIG.itemDisplayBaking.chunkRebuildCooldown * 1000.0));
        
        long lastRebuild = SECTION_LAST_REBUILD.getOrDefault(sectionLong, 0L);
        long targetTime = Math.max(now + settleDelayMs, lastRebuild + cooldownMs);
        
        PENDING_SECTION_REBUILDS.put(sectionLong, targetTime);
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
        // Process at most 2 sections per frame to eliminate any frame drops/freezes
        while (iterator.hasNext() && processed < 2) {
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
