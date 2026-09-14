package despairscent.skyblockm.tweaks;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class HitboxUtils {
    public static Box calculateDisplayBox(DisplayEntity.ItemDisplayEntity display) {
        try {
            var renderState = display.getRenderState();
            if (renderState == null) return null;
            
            var data = display.getData();
            if (data == null || data.itemStack().isEmpty()) return null;
            
            net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
            
            org.joml.Quaternionf billboardRot = new org.joml.Quaternionf().rotationYXZ(
                (float) Math.toRadians(-display.getYaw()),
                (float) Math.toRadians(display.getPitch()),
                0.0f
            );
            ms.multiply(billboardRot);
            
            ms.multiplyPositionMatrix(renderState.transformation().interpolate(1.0f).getMatrix());
            
            ms.multiply(RotationAxis.POSITIVE_Y.rotation((float)Math.PI));
            
            Box rawBounds = new Box(0, 0, 0, 1, 1, 1);
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                rawBounds = ModelBoundsCache.getBounds(data.itemStack(), data.itemTransform());
            }
            
            Matrix4f matrix = ms.peek().getPositionMatrix();
            
            Vector3f[] corners = new Vector3f[] {
                new Vector3f((float) rawBounds.minX, (float) rawBounds.minY, (float) rawBounds.minZ),
                new Vector3f((float) rawBounds.minX, (float) rawBounds.minY, (float) rawBounds.maxZ),
                new Vector3f((float) rawBounds.minX, (float) rawBounds.maxY, (float) rawBounds.minZ),
                new Vector3f((float) rawBounds.minX, (float) rawBounds.maxY, (float) rawBounds.maxZ),
                new Vector3f((float) rawBounds.maxX, (float) rawBounds.minY, (float) rawBounds.minZ),
                new Vector3f((float) rawBounds.maxX, (float) rawBounds.minY, (float) rawBounds.maxZ),
                new Vector3f((float) rawBounds.maxX, (float) rawBounds.maxY, (float) rawBounds.minZ),
                new Vector3f((float) rawBounds.maxX, (float) rawBounds.maxY, (float) rawBounds.maxZ)
            };
            
            double dx = display.getX();
            double dy = display.getY();
            double dz = display.getZ();
            
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            
            for (Vector3f corner : corners) {
                corner.mulPosition(matrix);
                double cx = corner.x + dx;
                double cy = corner.y + dy;
                double cz = corner.z + dz;
                if (cx < minX) minX = cx;
                if (cy < minY) minY = cy;
                if (cz < minZ) minZ = cz;
                if (cx > maxX) maxX = cx;
                if (cy > maxY) maxY = cy;
                if (cz > maxZ) maxZ = cz;
            }

            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
