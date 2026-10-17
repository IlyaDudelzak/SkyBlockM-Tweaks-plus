package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponent;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponentType;
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

        ReactorComponentType type = ReactorComponentType.byId(cmd);
        if (type == null) return null;

        ReactorComponent comp = type.create();

        if (stack.isDamageable() && comp.getMaxHeat() > 0) {
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