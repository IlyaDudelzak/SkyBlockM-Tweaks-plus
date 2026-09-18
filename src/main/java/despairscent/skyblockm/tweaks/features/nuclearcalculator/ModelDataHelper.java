package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import net.minecraft.item.ItemStack;

public class ModelDataHelper {

    public static int getCustomModelData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;

        //? if <=1.20.4 {
        /*if (stack.hasNbt() && stack.getNbt().contains("CustomModelData")) {
            return stack.getNbt().getInt("CustomModelData");
        }
        return -1;
        *///?} else {
        // 1. Проверяем штатный дата-компонент CustomModelData
        net.minecraft.component.type.CustomModelDataComponent comp =
                stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA);
        if (comp != null) {
            //? if <=1.21.3 {
            /*return comp.value();
            *///?} else {
            return comp.floats().isEmpty() ? -1 : (int) (float) comp.floats().getFirst();
             //?}
        }

        // 2. Запасной вариант: кастомный NBT (custom_data)
        net.minecraft.component.type.NbtComponent customData =
                stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);

        if (customData != null) {
            net.minecraft.nbt.NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("CustomModelData")) {
                //? if <=1.21.3 {
                /*return nbt.getInt("CustomModelData");
                *///?} else {
                return nbt.getInt("CustomModelData").orElse(-1);
                 //?}
            }
        }

        return -1;
        //?}
    }

    public static ItemStack createReactorStack(int cmd, String name) {
        ItemStack stack = new ItemStack(net.minecraft.item.Items.WOODEN_HOE);
        //? if <=1.20.4 {
        /*net.minecraft.nbt.NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt("CustomModelData", cmd);
        stack.setCustomName(net.minecraft.text.Text.literal(name));
        *///?} elif <=1.21.3 {
        /*stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                new net.minecraft.component.type.CustomModelDataComponent(cmd));
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal(name));
        *///?} else {
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
                new net.minecraft.component.type.CustomModelDataComponent(
                        java.util.List.of((float) cmd),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ));
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal(name));
        //?}
        return stack;
    }
}