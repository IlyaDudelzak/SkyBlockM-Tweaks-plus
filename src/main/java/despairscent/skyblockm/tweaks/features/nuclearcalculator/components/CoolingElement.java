package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class CoolingElement extends ReactorComponent {
    public CoolingElement(ReactorComponentType type) {
        super(type);
    }

    public CoolingElement(ReactorComponentType type, int heat) {
        super(type, heat);
    }

    public static CoolingElement element10k() { return new CoolingElement(ReactorComponentType.COOLING_ELEMENT_10K); }
    public static CoolingElement element30k() { return new CoolingElement(ReactorComponentType.COOLING_ELEMENT_30K); }
    public static CoolingElement element60k() { return new CoolingElement(ReactorComponentType.COOLING_ELEMENT_60K); }
}