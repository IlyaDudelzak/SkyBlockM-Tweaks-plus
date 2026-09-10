package despairscent.skyblockm.tweaks.config;

public class ItemDisplayHitboxConfig {
    public boolean enabled = false;
    public boolean antiRubberband = true;
    public double antiRubberbandDistance = 3.0;
    public HitboxType hitboxType = HitboxType.ENTITY_AABB;

    public enum HitboxType {
        ENTITY_AABB,
        VOXEL_SHAPE
    }
}
