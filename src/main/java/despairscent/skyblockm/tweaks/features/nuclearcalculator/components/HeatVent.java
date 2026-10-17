package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class HeatVent extends ReactorComponent {
    public HeatVent(ReactorComponentType type) {
        super(type);
    }

    public HeatVent(ReactorComponentType type, int heat) {
        super(type, heat);
    }

    public static HeatVent basic() { return new HeatVent(ReactorComponentType.BASIC_HEAT_VENT); }
    public static HeatVent advanced() { return new HeatVent(ReactorComponentType.ADVANCED_HEAT_VENT); }
    public static HeatVent component() { return new HeatVent(ReactorComponentType.COMPONENT_HEAT_VENT); }
    public static HeatVent reactor() { return new HeatVent(ReactorComponentType.REACTOR_HEAT_VENT); }
    public static HeatVent overclocked() { return new HeatVent(ReactorComponentType.OVERCLOCKED_HEAT_VENT); }
}
