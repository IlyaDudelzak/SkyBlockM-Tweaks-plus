package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class HeatVent extends ReactorComponent {
    private final int selfCooling;      // Отвод от себя
    private final int sideCooling;      // Отвод от каждого соседа (для компонентного)
    private final int reactorDrain;     // Забор тепла из корпуса (для реакторного и разогнанного)

    public HeatVent(int maxHeat, int selfCooling, int sideCooling, int reactorDrain) {
        super(maxHeat);
        this.selfCooling = selfCooling;
        this.sideCooling = sideCooling;
        this.reactorDrain = reactorDrain;
    }

    public static HeatVent basic() { return new HeatVent(1000, 12, 0, 0); }
    public static HeatVent advanced() { return new HeatVent(1000, 24, 0, 0); }
    public static HeatVent component() { return new HeatVent(0, 0, 10, 0); }
    public static HeatVent reactor() { return new HeatVent(1000, 12, 0, 12); }
    public static HeatVent overclocked() { return new HeatVent(1000, 40, 0, 60); }

    public int getSelfCooling() { return selfCooling; }
    public int getSideCooling() { return sideCooling; }
    public int getReactorDrain() { return reactorDrain; }
}
