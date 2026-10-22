package despairscent.skyblockm.tweaks.features.itemdisplay;

import net.minecraft.entity.Entity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class EntityCullingCompat {

    private static final boolean AVAILABLE;
    private static MethodHandle IS_CULLED_HANDLE;
    private static MethodHandle IS_OUT_OF_CAMERA_HANDLE;

    static {
        boolean avail = false;
        try {
            Class<?> cullableClass = Class.forName("dev.tr7zw.entityculling.versionless.access.Cullable");
            Method isCulledMethod = cullableClass.getMethod("isCulled");
            Method isOutOfCameraMethod = cullableClass.getMethod("isOutOfCamera");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            IS_CULLED_HANDLE = lookup.unreflect(isCulledMethod);
            IS_OUT_OF_CAMERA_HANDLE = lookup.unreflect(isOutOfCameraMethod);
            avail = true;
        } catch (Throwable ignored) {
        }
        AVAILABLE = avail;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean isCulled(Entity entity) {
        if (!AVAILABLE || entity == null) return false;
        try {
            return (boolean) IS_CULLED_HANDLE.invoke(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isOutOfCamera(Entity entity) {
        if (!AVAILABLE || entity == null) return false;
        try {
            return (boolean) IS_OUT_OF_CAMERA_HANDLE.invoke(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isCulledOrOutOfCamera(Entity entity) {
        return isCulled(entity) || isOutOfCamera(entity);
    }
}
