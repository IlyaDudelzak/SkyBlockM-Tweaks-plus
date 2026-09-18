package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class HeatExchanger extends ReactorComponent {
    private final int sideExchange;
    private final int reactorExchange;

    public HeatExchanger(int maxHeat, int sideExchange, int reactorExchange) {
        super(maxHeat);
        this.sideExchange = sideExchange;
        this.reactorExchange = reactorExchange;
    }

    public static HeatExchanger basic() { return new HeatExchanger(2500, 12, 4); }
    public static HeatExchanger advanced() { return new HeatExchanger(10000, 24, 8); }
    public static HeatExchanger component() { return new HeatExchanger(5000, 24, 0); }

    public int getSideExchange() { return sideExchange; }
    public int getReactorExchange() { return reactorExchange; }
}