package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.*;

public class ReactorSimulator {
    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int SIZE = COLS * ROWS;

    private final ReactorComponent[] grid;
    private int hullHeat = 0;
    private final int maxHullHeat = 10000; // Порог корпуса реактора
    private long outputWatts = 0;

    private boolean neutronReflector = false;

    public ReactorSimulator(ReactorComponent[] sourceGrid) {
        this.grid = new ReactorComponent[SIZE];
        // Копируем состояние, чтобы виртуальная симуляция не ломала исходные данные
        System.arraycopy(sourceGrid, 0, this.grid, 0, SIZE);
    }

    public void recalculateOutput() {
        long totalWatts = 0;
        for (int i = 0; i < SIZE; i++) {
            if (grid[i] instanceof FuelRod rod && !rod.isBroken()) {
                int adjRods = countAdjacentRods(i);
                totalWatts += rod.calculateEnergy(adjRods, this.hullHeat, this.maxHullHeat);
            }
        }
        if (neutronReflector) {
            totalWatts *= 4;
        }
        this.outputWatts = totalWatts;
    }

    /** Выполняет 1 секунду работы реактора */
    public void tick() {
        long totalWatts = 0;

        // 1. Фаза выработки тепла и энергии стержнями
        for (int i = 0; i < SIZE; i++) {
            if (grid[i] instanceof FuelRod rod && !rod.isBroken()) {
                int adjRods = countAdjacentRods(i);
                totalWatts += rod.calculateEnergy(adjRods, this.hullHeat, this.maxHullHeat);
                int generatedHeat = rod.calculateHeat(adjRods);

                distributeHeatToNeighbors(i, generatedHeat);
            }
        }
        if (neutronReflector) {
            totalWatts *= 4;
        }
        this.outputWatts = totalWatts;

        // 2. Фаза теплоотводов (охлаждение себя, соседей и корпуса)
        for (int i = 0; i < SIZE; i++) {
            if (grid[i] instanceof HeatVent vent && !vent.isBroken()) {
                // Забор из корпуса (реакторный / разогнанный)
                if (vent.getReactorDrain() > 0 && this.hullHeat > 0) {
                    int drain = Math.min(this.hullHeat, vent.getReactorDrain());
                    int overflow = vent.addHeat(drain);
                    this.hullHeat -= (drain - overflow);
                }

                // Охлаждение соседей (компонентный)
                if (vent.getSideCooling() > 0) {
                    coolNeighbors(i, vent.getSideCooling());
                }

                // Рассеивание тепла от себя в атмосферу
                vent.dissipate(vent.getSelfCooling());
            }
        }
    }

    private void distributeHeatToNeighbors(int index, int heat) {
        int[] neighbors = getAdjacentIndices(index);
        int validTargets = 0;

        for (int n : neighbors) {
            if (grid[n] != null && grid[n].canAcceptHeat() && !grid[n].isBroken()) {
                validTargets++;
            }
        }

        if (validTargets == 0) {
            // Если соседей нет или они не принимают тепло — всё тепло идет прямо в корпус
            this.hullHeat += heat;
            return;
        }

        int perTarget = heat / validTargets;
        int rest = heat % validTargets;

        for (int n : neighbors) {
            if (grid[n] != null && grid[n].canAcceptHeat() && !grid[n].isBroken()) {
                int excess = grid[n].addHeat(perTarget);
                this.hullHeat += excess;
            }
        }
        this.hullHeat += rest;
    }

    private void coolNeighbors(int index, int amount) {
        for (int n : getAdjacentIndices(index)) {
            if (grid[n] != null && grid[n].canAcceptHeat()) {
                grid[n].dissipate(amount);
            }
        }
    }

    private int countAdjacentRods(int index) {
        int count = 0;
        for (int n : getAdjacentIndices(index)) {
            if (grid[n] instanceof FuelRod rod && !rod.isBroken()) count++;
        }
        return count;
    }

    private int[] getAdjacentIndices(int i) {
        int r = i / COLS, c = i % COLS;
        java.util.List<Integer> list = new java.util.ArrayList<>(4);
        if (r > 0) list.add(i - COLS);
        if (r < ROWS - 1) list.add(i + COLS);
        if (c > 0) list.add(i - 1);
        if (c < COLS - 1) list.add(i + 1);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean hasNeutronReflector() { return neutronReflector; }
    public void setNeutronReflector(boolean reflector) { this.neutronReflector = reflector; }
    public int getHullHeat() { return hullHeat; }
    public void setHullHeat(int heat) { this.hullHeat = Math.max(0, heat); }
    public double getHullHeatPercent() { return (hullHeat / (double) maxHullHeat) * 100.0; }
    public long getOutputWatts() { return outputWatts; }
    public ReactorComponent[] getGrid() { return grid; }
}