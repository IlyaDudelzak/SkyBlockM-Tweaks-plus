package despairscent.skyblockm.tweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ItemDisplayBakingManager {
    public static class BakedEntityInfo {
        public final DisplayEntity.ItemDisplayEntity entity;
        public final Object renderState;
        public final Object data;
        public final BakedModel itemModel;

        public BakedEntityInfo(DisplayEntity.ItemDisplayEntity entity) {
            this.entity = entity;
            this.renderState = entity.getRenderState();
            this.data = entity.getData();
            
            BakedModel model = null;
            try {
                var d = entity.getData();
                if (d != null && !d.itemStack().isEmpty()) {
                    model = MinecraftClient.getInstance().getItemRenderer().getModel(d.itemStack(), null, null, 0);
                }
            } catch (Exception e) {
                // ignore
            }
            this.itemModel = model;
        }

        public boolean isUpToDate() {
            // We NO LONGER check this during rendering, we trust the tracker's isBaked!
            return true; 
        }
    }

    private static class StateTracker {
        Object renderState;
        Object data;
        int lastChangeAge;
        boolean isBaked;
        
        boolean isPendingUnbake;
        int pendingUnbakeStartAge;
        
        Box cachedBox;
        double x, y, z;
        float yaw, pitch;
    }

    private static final ConcurrentHashMap<BlockPos, CopyOnWriteArrayList<BakedEntityInfo>> STATIC_DISPLAYS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<DisplayEntity.ItemDisplayEntity, StateTracker> TRACKERS = new ConcurrentHashMap<>();

    public static Box getCachedBox(DisplayEntity.ItemDisplayEntity display) {
        StateTracker tracker = TRACKERS.computeIfAbsent(display, k -> new StateTracker());
        
        boolean posChanged = tracker.x != display.getX() || tracker.y != display.getY() || tracker.z != display.getZ() ||
                             tracker.yaw != display.getYaw() || tracker.pitch != display.getPitch();
        
        boolean stateChanged = tracker.renderState != display.getRenderState() || tracker.data != display.getData();
        
        if (stateChanged || posChanged || tracker.cachedBox == null) {
            tracker.cachedBox = HitboxUtils.calculateDisplayBox(display);
            tracker.x = display.getX();
            tracker.y = display.getY();
            tracker.z = display.getZ();
            tracker.yaw = display.getYaw();
            tracker.pitch = display.getPitch();
        }
        
        return tracker.cachedBox;
    }

    public static void updateEntity(DisplayEntity.ItemDisplayEntity display) {
        StateTracker tracker = TRACKERS.computeIfAbsent(display, k -> new StateTracker());
        
        boolean posChanged = tracker.x != display.getX() || tracker.y != display.getY() || tracker.z != display.getZ() ||
                             tracker.yaw != display.getYaw() || tracker.pitch != display.getPitch();
                             
        boolean stateChanged = tracker.renderState != display.getRenderState() || tracker.data != display.getData();
        
        if (posChanged) {
            // If it physically moved, unbake immediately! No debounce!
            tracker.renderState = display.getRenderState();
            tracker.data = display.getData();
            tracker.lastChangeAge = display.age;
            tracker.isPendingUnbake = false;
            removeEntity(display);
        } else if (stateChanged) {
            if (tracker.isBaked) {
                if (!tracker.isPendingUnbake) {
                    tracker.isPendingUnbake = true;
                    tracker.pendingUnbakeStartAge = display.age;
                }
                
                // If it has been changed for > 100 ticks (5 seconds), unbake it!
                if (display.age - tracker.pendingUnbakeStartAge > 100) {
                    tracker.renderState = display.getRenderState();
                    tracker.data = display.getData();
                    tracker.lastChangeAge = display.age;
                    tracker.isPendingUnbake = false;
                    removeEntity(display);
                }
            } else {
                tracker.renderState = display.getRenderState();
                tracker.data = display.getData();
                tracker.lastChangeAge = display.age;
                tracker.isPendingUnbake = false;
            }
        } else {
            tracker.isPendingUnbake = false;
        }
        
        BlockPos pos = display.getBlockPos();
        
        Box box = getCachedBox(display);
        boolean isSmall = false;
        if (box != null) {
            isSmall = (box.maxX - box.minX) <= 1.6 && 
                      (box.maxY - box.minY) <= 1.6 && 
                      (box.maxZ - box.minZ) <= 1.6;
        }
        
        if (!isSmall || display.getWorld() == null || !display.getWorld().getBlockState(pos).isOf(net.minecraft.block.Blocks.BARRIER)) {
            tracker.lastChangeAge = display.age;
            removeEntity(display);
            return;
        }
        
        if (display.getVelocity().lengthSquared() < 0.0001 && display.getLerpProgress(1.0f) >= 1.0f) {
            if (display.age - tracker.lastChangeAge > 40) {
                if (!tracker.isBaked) {
                    CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.computeIfAbsent(pos, k -> new CopyOnWriteArrayList<>());
                    list.add(new BakedEntityInfo(display));
                    tracker.isBaked = true;
                    triggerRebuild(display, pos);
                }
            }
        } else {
            tracker.lastChangeAge = display.age;
            removeEntity(display);
        }
    }

    public static void removeEntity(DisplayEntity.ItemDisplayEntity display) {
        StateTracker tracker = TRACKERS.get(display);
        boolean wasBaked = false;
        if (tracker != null) {
            wasBaked = tracker.isBaked;
            tracker.isBaked = false;
        }
        
        BlockPos pos = display.getBlockPos();
        CopyOnWriteArrayList<BakedEntityInfo> list = STATIC_DISPLAYS.get(pos);
        if (list != null) {
            boolean removed = list.removeIf(info -> info.entity == display);
            if (removed || wasBaked) {
                if (list.isEmpty()) {
                    STATIC_DISPLAYS.remove(pos);
                }
                triggerRebuild(display, pos);
            }
        }
    }

    public static void onEntityRemoved(DisplayEntity.ItemDisplayEntity display) {
        removeEntity(display);
        TRACKERS.remove(display);
    }

    private static void triggerRebuild(DisplayEntity.ItemDisplayEntity display, BlockPos pos) {
        if (display.getWorld() != null && display.getWorld().isClient) {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().worldRenderer != null) {
                    MinecraftClient.getInstance().worldRenderer.scheduleBlockRenders(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
                }
            });
        }
    }

    public static List<BakedEntityInfo> getStaticDisplaysAt(BlockPos pos) {
        return STATIC_DISPLAYS.get(pos);
    }
    
    public static boolean isBaked(DisplayEntity.ItemDisplayEntity display) {
        StateTracker tracker = TRACKERS.get(display);
        return tracker != null && tracker.isBaked;
    }
}
