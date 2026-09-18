package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class FuelRod extends ReactorComponent {
    private final int baseEnergy;
    private final int bonusEnergy;
    private final int baseHeat;
    private final int bonusHeat;
    private final boolean isMox;

    public FuelRod(int baseEnergy, int bonusEnergy, int baseHeat, int bonusHeat, boolean isMox) {
        super(0);
        this.baseEnergy = baseEnergy;
        this.bonusEnergy = bonusEnergy;
        this.baseHeat = baseHeat;
        this.bonusHeat = bonusHeat;
        this.isMox = isMox;
    }

    public static FuelRod singleUranium() { return new FuelRod(320, 320, 4, 6, false); }
    public static FuelRod dualUranium() { return new FuelRod(1280, 640, 24, 36, false); }
    public static FuelRod quadUranium() { return new FuelRod(2560, 1280, 96, 144, false); }

    public static FuelRod singleMox() { return new FuelRod(320, 320, 4, 6, true); }
    public static FuelRod dualMox() { return new FuelRod(1280, 640, 24, 36, true); }
    public static FuelRod quadMox() { return new FuelRod(2560, 1280, 96, 144, true); }

    public boolean isMox() { return isMox; }

    public int calculateEnergy(int adjacentRods) {
        return calculateEnergy(adjacentRods, 0, 10000);
    }

    public int calculateEnergy(int adjacentRods, int hullHeat, int maxHullHeat) {
        int base = baseEnergy + (bonusEnergy * adjacentRods);
        if (isMox && maxHullHeat > 0) {
            double heatRatio = Math.min(1.0, (double) hullHeat / maxHullHeat);
            return (int) Math.round(base * (1.0 + 4.0 * heatRatio));
        }
        return base;
    }

    public int calculateHeat(int adjacentRods) {
        return baseHeat + (bonusHeat * adjacentRods);
    }
}