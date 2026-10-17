package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class FuelRod extends ReactorComponent {
    public FuelRod(ReactorComponentType type) {
        super(type);
    }

    public FuelRod(ReactorComponentType type, int heat) {
        super(type, heat);
    }

    public static FuelRod singleUranium() { return new FuelRod(ReactorComponentType.SINGLE_URANIUM); }
    public static FuelRod dualUranium() { return new FuelRod(ReactorComponentType.DUAL_URANIUM); }
    public static FuelRod quadUranium() { return new FuelRod(ReactorComponentType.QUAD_URANIUM); }

    public static FuelRod singleMox() { return new FuelRod(ReactorComponentType.SINGLE_MOX); }
    public static FuelRod dualMox() { return new FuelRod(ReactorComponentType.DUAL_MOX); }
    public static FuelRod quadMox() { return new FuelRod(ReactorComponentType.QUAD_MOX); }
}