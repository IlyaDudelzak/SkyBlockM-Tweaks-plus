package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class HeatExchanger extends ReactorComponent {
    public HeatExchanger(ReactorComponentType type) {
        super(type);
    }

    public HeatExchanger(ReactorComponentType type, int heat) {
        super(type, heat);
    }

    public static HeatExchanger basic() { return new HeatExchanger(ReactorComponentType.BASIC_HEAT_EXCHANGER); }
    public static HeatExchanger advanced() { return new HeatExchanger(ReactorComponentType.ADVANCED_HEAT_EXCHANGER); }
    public static HeatExchanger component() { return new HeatExchanger(ReactorComponentType.COMPONENT_HEAT_EXCHANGER); }
}