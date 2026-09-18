package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ReactorItemMapper {

    public static ReactorComponent fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        if (!stack.isOf(Items.WOODEN_HOE)) {
            return null;
        }

        int cmd = ModelDataHelper.getCustomModelData(stack);
        if (cmd == -1) return null;

        ReactorComponent comp = switch (cmd) {
            // 1. Охлаждающие элементы
            case 210 -> CoolingElement.element10k();
            case 211 -> CoolingElement.element30k();
            case 212 -> CoolingElement.element60k();

            // 2. Теплообменники
            case 220 -> HeatExchanger.basic();
            case 221 -> HeatExchanger.advanced();
            case 222 -> HeatExchanger.component();

            // 3. Теплоотводы
            case 230 -> HeatVent.basic();
            case 231 -> HeatVent.advanced();
            case 232 -> HeatVent.component();
            case 233 -> HeatVent.reactor();
            case 234 -> HeatVent.overclocked();

            // Топливные стержни (Уран)
            case 240 -> FuelRod.singleUranium();
            case 241 -> FuelRod.dualUranium();
            case 242 -> FuelRod.quadUranium();

            // Топливные стержни (MOX)
            case 243 -> FuelRod.singleMox();
            case 244 -> FuelRod.dualMox();
            case 245 -> FuelRod.quadMox();

            // Все остальное (включая сингулярные 260-281) симулятору не принадлежит
            default -> null;
        };

        if (comp != null && stack.isDamageable() && comp.getMaxHeat() > 0) {
            int damage = stack.getDamage();
            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                int currentHeat = (int) Math.round(((double) damage / maxDamage) * comp.getMaxHeat());
                comp.setHeat(currentHeat);
            }
        }

        return comp;
    }
}