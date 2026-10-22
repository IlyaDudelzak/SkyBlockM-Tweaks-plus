package despairscent.skyblockm.tweaks.config;

public class ItemDisplayBakingConfig {
    public enum BakingMode {
        CLASSIC("classic"),
        BATCHED("batched");

        public final String optionName;

        BakingMode(String optionName) {
            this.optionName = optionName;
        }
    }

    public boolean enabled = true;
    public BakingMode mode = BakingMode.CLASSIC;
    public double maxBakeSize = 10.0;
    public int bakeDelayTicks = 0;
    public double chunkSettleTime = 0.0;
    public double chunkRebuildCooldown = 1.0;
    public int maxRebuildsPerFrame = 8;
    public boolean hideBakeableEntities = true;
    public boolean entityCullingIntegration = true;
    public double playerChunkRebuildInterval = 0.0;
}
