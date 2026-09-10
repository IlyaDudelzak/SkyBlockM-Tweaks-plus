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
            
            Matrix4f matrix = new Matrix4f();
            matrix.translate((float)display.getX(), (float)display.getY(), (float)display.getZ());
            
            matrix.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-display.getYaw()));
            matrix.rotate(RotationAxis.POSITIVE_X.rotationDegrees(display.getPitch()));
            
            matrix.mul(renderState.transformation().interpolate(1.0f).getMatrix());
            
            matrix.rotate(RotationAxis.POSITIVE_Y.rotation((float)Math.PI));
            
            Box rawBounds = new Box(0, 0, 0, 1, 1, 1);
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                rawBounds = ModelBoundsCache.getBounds(data.itemStack());
                
                net.minecraft.client.render.model.BakedModel itemModel = net.minecraft.client.MinecraftClient.getInstance().getItemRenderer().getModel(data.itemStack(), null, null, 0);
                if (itemModel != null) {
                    net.minecraft.client.util.math.MatrixStack ms = new net.minecraft.client.util.math.MatrixStack();
                    itemModel.getTransformation().getTransformation(data.itemTransform()).apply(false, ms);
                    matrix.mul(ms.peek().getPositionMatrix());
                }
            }
            
            matrix.translate(-0.5f, -0.5f, -0.5f);
            
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
            
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            
            for (Vector3f corner : corners) {
                corner.mulPosition(matrix);
                if (corner.x < minX) minX = corner.x;
                if (corner.y < minY) minY = corner.y;
                if (corner.z < minZ) minZ = corner.z;
                if (corner.x > maxX) maxX = corner.x;
                if (corner.y > maxY) maxY = corner.y;
                if (corner.z > maxZ) maxZ = corner.z;
            }

            return new Box(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
