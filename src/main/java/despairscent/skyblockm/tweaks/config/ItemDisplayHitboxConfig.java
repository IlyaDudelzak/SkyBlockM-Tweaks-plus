package despairscent.skyblockm.tweaks.config;

public class ItemDisplayHitboxConfig {
    public boolean enabled = true;
    public boolean antiRubberband = true;
    public double antiRubberbandDistance = 3.0;
    public boolean optimizeAsBlocks = true;
    public HitboxType hitboxType = HitboxType.ENTITY_AABB;

    public enum HitboxType {
        ENTITY_AABB,
        VOXEL_SHAPE
    }
}
