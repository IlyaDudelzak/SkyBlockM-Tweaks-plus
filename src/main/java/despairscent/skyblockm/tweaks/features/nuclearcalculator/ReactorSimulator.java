package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReactorSimulator implements Serializable {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(ReactorComponent.class, new ReactorComponent.Serializer())
            .create();

    public static final int COLS = 9;
    public static final int ROWS = 6;
    public static final int SIZE = COLS * ROWS;

    private final ReactorComponent[] grid;
    private int hullHeat = 0;
    private final transient int maxHullHeat = 10000; // Порог корпуса реактора
    private long outputWatts = 0;

    private boolean neutronReflector = false;

    public ReactorSimulator(ReactorComponent[] sourceGrid) {
        this.grid = new ReactorComponent[SIZE];
        if (sourceGrid != null) {
            for (int i = 0; i < SIZE && i < sourceGrid.length; i++) {
                this.grid[i] = sourceGrid[i] != null ? sourceGrid[i].copy() : null;
            }
        }
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
        // ВСЕГДА выполняется ДО всего остального!
        List<Integer> rodIndices = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            if (grid[i] instanceof FuelRod rod && !rod.isBroken()) {
                rodIndices.add(i);
            }
        }
        Collections.shuffle(rodIndices, ThreadLocalRandom.current());

        for (int i : rodIndices) {
            FuelRod rod = (FuelRod) grid[i];
            int adjRods = countAdjacentRods(i);
            totalWatts += rod.calculateEnergy(adjRods, this.hullHeat, this.maxHullHeat);
            int generatedHeat = rod.calculateHeat(adjRods);
            if (neutronReflector) {
                generatedHeat *= 4;
            }

            distributeHeatToNeighbors(i, generatedHeat);
        }
        if (neutronReflector) {
            totalWatts *= 4;
        }
        this.outputWatts = totalWatts;

        // 2. Фаза остальных элементов (теплоотводы, теплообменники и др.)
        // Обрабатываются в СЛУЧАЙНОМ порядке каждый тик
        List<Integer> otherIndices = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            if (grid[i] != null && !(grid[i] instanceof FuelRod) && !grid[i].isBroken()) {
                otherIndices.add(i);
            }
        }
        Collections.shuffle(otherIndices, ThreadLocalRandom.current());

        for (int i : otherIndices) {
            ReactorComponent comp = grid[i];
            if (comp == null || comp.isBroken()) continue;

            if (comp instanceof HeatVent vent) {
                processHeatVent(i, vent);
            } else if (comp instanceof HeatExchanger exchanger) {
                processHeatExchanger(i, exchanger);
            }
        }
    }

    private void processHeatVent(int index, HeatVent vent) {
        int mult = neutronReflector ? 4 : 1;

        // Отвод тепла от себя в атмосферу
        vent.dissipate(vent.getSelfCooling() * mult);

        // Забор тепла из корпуса (реакторный / разогнанный)
        if (vent.getReactorDrain() > 0 && this.hullHeat > 0) {
            int drain = Math.min(this.hullHeat, vent.getReactorDrain() * mult);
            int overflow = vent.addHeat(drain);
            this.hullHeat -= (drain - overflow);
        }

        // Охлаждение 4-х соседей (компонентный теплоотвод)
        if (vent.getSideCooling() > 0) {
            coolNeighbors(index, vent.getSideCooling() * mult);
        }
    }

    private void processHeatExchanger(int index, HeatExchanger exchanger) {
        if (exchanger.getMaxHeat() <= 0) return;

        int mult = neutronReflector ? 4 : 1;

        // 1. Обмен с корпусом реактора
        int rEx = exchanger.getType().getReactorExchange() * mult;
        if (rEx > 0 && maxHullHeat > 0) {
            double hullP = this.hullHeat / (double) this.maxHullHeat;
            double myP = exchanger.getCurrentHeat() / (double) exchanger.getMaxHeat();
            if (hullP > myP && this.hullHeat > 0) {
                int transfer = Math.min(rEx, (int) Math.round((hullP - myP) * exchanger.getMaxHeat()));
                transfer = Math.min(transfer, this.hullHeat);
                int overflow = exchanger.addHeat(transfer);
                this.hullHeat -= (transfer - overflow);
            } else if (myP > hullP && exchanger.getCurrentHeat() > 0) {
                int transfer = Math.min(rEx, (int) Math.round((myP - hullP) * exchanger.getMaxHeat()));
                transfer = Math.min(transfer, exchanger.getCurrentHeat());
                exchanger.dissipate(transfer);
                this.hullHeat += transfer;
            }
        }

        // 2. Обмен с соседними компонентами
        int sEx = exchanger.getType().getSideExchange() * mult;
        if (sEx > 0) {
            int[] neighbors = getAdjacentIndices(index);
            for (int n : neighbors) {
                ReactorComponent neighbor = grid[n];
                if (neighbor != null && neighbor.canAcceptHeat() && !neighbor.isBroken() && neighbor.getMaxHeat() > 0) {
                    double myP = exchanger.getCurrentHeat() / (double) exchanger.getMaxHeat();
                    double nP = neighbor.getCurrentHeat() / (double) neighbor.getMaxHeat();
                    if (myP > nP && exchanger.getCurrentHeat() > 0) {
                        int transfer = Math.min(sEx, (int) Math.round((myP - nP) * exchanger.getMaxHeat()));
                        transfer = Math.min(transfer, exchanger.getCurrentHeat());
                        int excess = neighbor.addHeat(transfer);
                        exchanger.dissipate(transfer - excess);
                    } else if (nP > myP && neighbor.getCurrentHeat() > 0) {
                        int transfer = Math.min(sEx, (int) Math.round((nP - myP) * neighbor.getMaxHeat()));
                        transfer = Math.min(transfer, neighbor.getCurrentHeat());
                        int overflow = exchanger.addHeat(transfer);
                        neighbor.dissipate(transfer - overflow);
                    }
                }
            }
        }
    }

    private void distributeHeatToNeighbors(int index, int heat) {
        int[] neighbors = getAdjacentIndices(index);
        List<Integer> validTargets = new ArrayList<>();

        for (int n : neighbors) {
            if (grid[n] != null && grid[n].canAcceptHeat() && !grid[n].isBroken()) {
                validTargets.add(n);
            }
        }

        if (validTargets.isEmpty()) {
            // Если соседей нет или они не принимают тепло — всё тепло идет прямо в корпус
            this.hullHeat += heat;
            return;
        }

        // Перемешиваем цели, чтобы остаток тепла распределялся равномерно
        Collections.shuffle(validTargets, ThreadLocalRandom.current());

        int count = validTargets.size();
        int perTarget = heat / count;
        int rest = heat % count;

        for (int i = 0; i < count; i++) {
            int n = validTargets.get(i);
            int amount = perTarget + (i < rest ? 1 : 0);
            int excess = grid[n].addHeat(amount);
            this.hullHeat += excess;
        }
    }

    private void coolNeighbors(int index, int amount) {
        for (int n : getAdjacentIndices(index)) {
            if (grid[n] != null && grid[n].canAcceptHeat() && !grid[n].isBroken()) {
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

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ReactorSimulator fromJson(String json) {
        return GSON.fromJson(json, ReactorSimulator.class);
    }
}