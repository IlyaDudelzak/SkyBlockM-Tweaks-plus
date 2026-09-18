package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public class CoolingElement extends ReactorComponent {

    public CoolingElement(int maxHeat) {
        super(maxHeat);
    }

    public static CoolingElement element10k() { return new CoolingElement(10_000); }
    public static CoolingElement element30k() { return new CoolingElement(30_000); }
    public static CoolingElement element60k() { return new CoolingElement(60_000); }

    @Override
    public void dissipate(int amount) {
        // Охлаждающие элементы пассивно не остывают сами по себе
    }
}