package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponent;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponentType;
import net.minecraft.item.ItemStack;

public class ReactorHeatingOptimizer {

    public static class HeatingPlan {
        public final boolean success;
        public final String message;
        public final ReactorComponent[] grid; // 54 slots
        public final int heatPerSecond;
        public final int seconds;
        public final int startHeat;
        public final int endHeat;
        public final double endPercent;
        public final int pairs;
        public final int quads;
        public final int duals;
        public final int singles;
        public final boolean isMox;

        public HeatingPlan(boolean success, String message, ReactorComponent[] grid,
                           int heatPerSecond, int seconds, int startHeat, int endHeat,
                           int pairs, int quads, int duals, int singles, boolean isMox) {
            this.success = success;
            this.message = message;
            this.grid = grid;
            this.heatPerSecond = heatPerSecond;
            this.seconds = seconds;
            this.startHeat = startHeat;
            this.endHeat = endHeat;
            this.endPercent = (endHeat / 10000.0) * 100.0;
            this.pairs = pairs;
            this.quads = quads;
            this.duals = duals;
            this.singles = singles;
            this.isMox = isMox;
        }

        public static HeatingPlan failure(String message) {
            return new HeatingPlan(false, message, null, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }
    }

    public static HeatingPlan findBestHeatingPlan(int currentHeat, ItemStack[] inventory, ItemStack[] currentGrid, boolean fromReactor) {
        int targetHeat = 9900; // 99%
        if (currentHeat >= targetHeat) {
            return HeatingPlan.failure("Реактор уже разогрет до 99%!");
        }

        int neededHeat = targetHeat - currentHeat;

        // 1. Подсчет доступных стержней
        int quads = 0;
        int duals = 0;
        int singles = 0;
        boolean hasUranium = false;
        boolean hasMox = false;

        if (fromReactor) {
            if (inventory != null) {
                for (ItemStack stack : inventory) {
                    if (stack != null && !stack.isEmpty()) {
                        ReactorComponent comp = ReactorItemMapper.fromItemStack(stack);
                        if (comp != null && comp.isFuelRod()) {
                            if (comp.getType() == ReactorComponentType.QUAD_URANIUM) { quads++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.DUAL_URANIUM) { duals++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.SINGLE_URANIUM) { singles++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.QUAD_MOX) { quads++; hasMox = true; }
                            else if (comp.getType() == ReactorComponentType.DUAL_MOX) { duals++; hasMox = true; }
                            else if (comp.getType() == ReactorComponentType.SINGLE_MOX) { singles++; hasMox = true; }
                        }
                    }
                }
            }
            if (currentGrid != null) {
                for (ItemStack stack : currentGrid) {
                    if (stack != null && !stack.isEmpty()) {
                        ReactorComponent comp = ReactorItemMapper.fromItemStack(stack);
                        if (comp != null && comp.isFuelRod()) {
                            if (comp.getType() == ReactorComponentType.QUAD_URANIUM) { quads++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.DUAL_URANIUM) { duals++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.SINGLE_URANIUM) { singles++; hasUranium = true; }
                            else if (comp.getType() == ReactorComponentType.QUAD_MOX) { quads++; hasMox = true; }
                            else if (comp.getType() == ReactorComponentType.DUAL_MOX) { duals++; hasMox = true; }
                            else if (comp.getType() == ReactorComponentType.SINGLE_MOX) { singles++; hasMox = true; }
                        }
                    }
                }
            }
        }

        boolean unlimited = (quads + duals + singles == 0);
        if (unlimited) {
            quads = 10;
            duals = 10;
            singles = 10;
            hasUranium = true;
        }

        boolean useMox = !hasUranium && hasMox;

        // 2. Поиск оптимальной комбинации
        // Тепло: пара quads = 480, quad = 96, dual = 24, single = 4
        int bestScore = Integer.MAX_VALUE;
        int bestT = 0;
        int bestQ = 0;
        int bestPairs = 0;
        int bestQuads = 0;
        int bestDuals = 0;
        int bestSingles = 0;

        int minT = (neededHeat > 4000) ? 15 : (neededHeat > 1000 ? 8 : 1);
        int maxT = (neededHeat > 4000) ? 45 : (neededHeat > 1000 ? 30 : 15);
        int idealT = (neededHeat > 4000) ? 25 : (neededHeat > 1000 ? 15 : 5);

        int maxPairs = quads / 2;

        for (int T = minT; T <= maxT; T++) {
            for (int p = 0; p <= Math.min(2, maxPairs); p++) {
                int remQuads = quads - p * 2;
                for (int q = 0; q <= Math.min(5, remQuads); q++) {
                    for (int d = 0; d <= Math.min(4, duals); d++) {
                        for (int s = 0; s <= Math.min(6, singles); s++) {
                            int Q = p * 480 + q * 96 + d * 24 + s * 4;
                            if (Q <= 0) continue;
                            int totalAdded = T * Q;
                            if (totalAdded <= neededHeat) {
                                int rem = neededHeat - totalAdded;
                                // Штрафы: недобранное тепло, отклонение по времени, количество стержней
                                int score = rem * 100 + Math.abs(T - idealT) * 10 + (p * 2 + q + d + s) * 2;
                                if (score < bestScore) {
                                    bestScore = score;
                                    bestT = T;
                                    bestQ = Q;
                                    bestPairs = p;
                                    bestQuads = q;
                                    bestDuals = d;
                                    bestSingles = s;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestQ <= 0) {
            return HeatingPlan.failure("Не удалось подобрать схему (недостаточно стержней)");
        }

        // 3. Формирование сетки 9x6
        ReactorComponent[] grid = new ReactorComponent[54];
        placeComponents(grid, bestPairs, bestQuads, bestDuals, bestSingles, useMox);

        int finalHeat = currentHeat + bestT * bestQ;
        String msg = String.format("Разогрев: +%d/с за %d сек (до %.1f%%)",
                bestQ, bestT, (finalHeat / 10000.0) * 100.0);

        return new HeatingPlan(true, msg, grid, bestQ, bestT, currentHeat, finalHeat,
                bestPairs, bestQuads, bestDuals, bestSingles, useMox);
    }

    private static void placeComponents(ReactorComponent[] grid, int pairs, int quads, int duals, int singles, boolean useMox) {
        ReactorComponentType quadType = useMox ? ReactorComponentType.QUAD_MOX : ReactorComponentType.QUAD_URANIUM;
        ReactorComponentType dualType = useMox ? ReactorComponentType.DUAL_MOX : ReactorComponentType.DUAL_URANIUM;
        ReactorComponentType singleType = useMox ? ReactorComponentType.SINGLE_MOX : ReactorComponentType.SINGLE_URANIUM;

        // Размещаем пары в четных рядах (row 0, 2, 4) с зазором
        int curRow = 0;
        int curCol = 0;

        for (int p = 0; p < pairs; p++) {
            if (curCol + 1 >= 9) {
                curRow += 2;
                curCol = 0;
            }
            if (curRow >= 6) break;
            grid[curRow * 9 + curCol] = quadType.create();
            grid[curRow * 9 + curCol + 1] = quadType.create();
            curCol += 3; // 2 слота под пару + 1 пустой слот зазора
        }

        // Переходим к следующему четному ряду для изолированных стержней
        if (pairs > 0 && curCol > 0) {
            curRow += 2;
            curCol = 0;
        }

        // Размещаем изолированные квады
        for (int q = 0; q < quads; q++) {
            if (curCol >= 9) {
                curRow += 2;
                curCol = 0;
            }
            if (curRow >= 6) break;
            grid[curRow * 9 + curCol] = quadType.create();
            curCol += 2; // 1 слот стержень + 1 слот зазора
        }

        // Размещаем изолированные дуалы
        for (int d = 0; d < duals; d++) {
            if (curCol >= 9) {
                curRow += 2;
                curCol = 0;
            }
            if (curRow >= 6) break;
            grid[curRow * 9 + curCol] = dualType.create();
            curCol += 2;
        }

        // Размещаем изолированные синглы
        for (int s = 0; s < singles; s++) {
            if (curCol >= 9) {
                curRow += 2;
                curCol = 0;
            }
            if (curRow >= 6) break;
            grid[curRow * 9 + curCol] = singleType.create();
            curCol += 2;
        }
    }
}
