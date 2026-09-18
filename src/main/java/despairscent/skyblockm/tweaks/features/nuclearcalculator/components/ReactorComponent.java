package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

public abstract class ReactorComponent {
    protected int currentHeat = 0;
    protected final int maxHeat;

    public ReactorComponent(int maxHeat) {
        this.maxHeat = maxHeat;
    }

    public int getCurrentHeat() { return currentHeat; }
    public int getMaxHeat() { return maxHeat; }
    public boolean canAcceptHeat() { return maxHeat > 0; }
    public boolean isBroken() { return maxHeat > 0 && currentHeat >= maxHeat; }

    public int addHeat(int amount) {
        if (!canAcceptHeat() || isBroken()) return amount;
        int space = maxHeat - currentHeat;
        if (amount <= space) {
            currentHeat += amount;
            return 0;
        } else {
            currentHeat = maxHeat;
            return amount - space; // Излишек уходит в корпус или взрывает компонент
        }
    }

    public void dissipate(int amount) {
        this.currentHeat = Math.max(0, this.currentHeat - amount);
    }

    public void setHeat(int heat) {
        if (this.maxHeat > 0) {
            this.currentHeat = Math.max(0, Math.min(this.maxHeat, heat));
        } else {
            this.currentHeat = 0;
        }
    }
}