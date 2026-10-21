package despairscent.skyblockm.tweaks.features.renderiteminside;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
//? if <=1.21.3 {
/*import net.minecraft.nbt.NbtElement;
*///?}
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

//? if >=1.20.5 {
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
//?}

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemInsideResolver {

    private static final Map<String, Integer> FLUID_CMD_MAP;

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("acetic_acid", 2104314);
        map.put("acetone", 2104414);
        map.put("aluminium_solution", 2106114);
        map.put("arrow", 2100514);
        map.put("background", 2106414);
        map.put("benzol", 2102114);
        map.put("biodiesel", 2103814);
        map.put("biogas", 2104914);
        map.put("biomass", 2102214);
        map.put("bisphenol_a", 2104514);
        map.put("chlorine", 2103114);
        map.put("chromium_solution", 2105814);
        map.put("coke_oven_gas", 2102064);
        map.put("compressed_air", 2100914);
        map.put("coolant", 2101414);
        map.put("creosote", 2101314);
        map.put("distilled_water", 2101214);
        map.put("epoxy_resin", 2104614);
        map.put("ethanol", 2102314);
        map.put("ethylene", 2102414);
        map.put("eu", 2100414);
        map.put("fermented_biomass", 2103314);
        map.put("fluorine", 2107314);
        map.put("fluorine_waste", 2107514);
        map.put("glowstone", 2104714);
        map.put("glycerol", 2103914);
        map.put("gold_iii_chloride", 2106614);
        map.put("gustavson_complex", 2107014);
        map.put("helium", 2102714);
        map.put("honey", 2100614);
        map.put("hydrochloric_acid", 2104114);
        map.put("hydrogen", 2102914);
        map.put("hydrogen_fluorine", 2107214);
        map.put("iron_ii_chloride", 2107814);
        map.put("iron_iii_chloride", 2104014);
        map.put("lava", 2106514);
        map.put("life_time", 2100314);
        map.put("liquid_helium", 2102814);
        map.put("liquid_hydrogen", 2105514);
        map.put("liquid_methane", 2105414);
        map.put("liquid_oxygen", 2105314);
        map.put("matter", 2101114);
        map.put("methane", 2103514);
        map.put("methanol", 2103614);
        map.put("nether_concentrate", 2106814);
        map.put("nether_oil", 2106714);
        map.put("nether_waste", 2106914);
        map.put("netherite_suspension", 2107114);
        map.put("netherrack_solution", 2106214);
        map.put("nickel_solution", 2106014);
        map.put("oxygen", 2103014);
        map.put("phenol", 2103214);
        map.put("phosphoric_acid", 2103414);
        map.put("polyethylene", 2101814);
        map.put("polyvinyl_chloride", 2101914);
        map.put("prismarine_solution", 2106314);
        map.put("pure_titanium_tetrachloride", 2107714);
        map.put("quantum_progress", 2101614);
        map.put("resin", 2105614);
        map.put("rocket_fuel_mk_1", 2105014);
        map.put("rocket_fuel_mk_2", 2105114);
        map.put("rocket_fuel_mk_3", 2105214);
        map.put("saline_solution", 2100114);
        map.put("scanner", 2101014);
        map.put("seed_oil", 2103714);
        map.put("sodium_persulfate", 2104214);
        map.put("solder", 2102614);
        map.put("steam", 2100814);
        map.put("sulphuric_acid", 2100214);
        map.put("titanium_tetrachloride", 2107614);
        map.put("turpentine", 2105714);
        map.put("uranium_hexafluoride", 2107414);
        map.put("vinyl_chloride", 2102514);
        map.put("water", 2100014);
        map.put("working_quantum_progress", 2101714);
        map.put("zinc_solution", 2105914);
        FLUID_CMD_MAP = Collections.unmodifiableMap(map);
    }

    public static Integer getFluidCmd(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return null;
        String key = fluidId;
        int colon = key.indexOf(':');
        if (colon != -1) {
            key = key.substring(colon + 1);
        }
        return FLUID_CMD_MAP.get(key.toLowerCase(java.util.Locale.ROOT));
    }

    public static ItemStack createStack(Item item, int cmd) {
        ItemStack stack = new ItemStack(item);
        if (cmd > 0) {
            //? if <=1.20.4 {
            /*stack.getOrCreateNbt().putInt("CustomModelData", cmd);
            *///?} elif <=1.21.3 {
            /*stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(cmd));
            *///?} else {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                    List.of((float) cmd),
                    List.of(),
                    List.of(),
                    List.of()
            ));
            //?}
        }
        return stack;
    }

    private static int getCmdFromCompound(NbtCompound nbt) {
        if (nbt == null) return -1;
        //? if <=1.21.3 {
        /*if (nbt.contains("CustomModelData", NbtElement.NUMBER_TYPE)) {
            return nbt.getInt("CustomModelData");
        }
        if (nbt.contains("custom_model_data", NbtElement.NUMBER_TYPE)) {
            return nbt.getInt("custom_model_data");
        }
        return -1;
        *///?} else {
        if (nbt.contains("CustomModelData")) {
            return nbt.getInt("CustomModelData").orElse(-1);
        }
        if (nbt.contains("custom_model_data")) {
            return nbt.getInt("custom_model_data").orElse(-1);
        }
        return -1;
        //?}
    }

    private static int extractCmd(NbtCompound stackNbt) {
        if (stackNbt == null) return -1;
        //? if <=1.21.3 {
        /*if (stackNbt.contains("tag", NbtElement.COMPOUND_TYPE)) {
            int cmd = getCmdFromCompound(stackNbt.getCompound("tag"));
            if (cmd != -1) return cmd;
        }
        if (stackNbt.contains("custom_data", NbtElement.COMPOUND_TYPE)) {
            int cmd = getCmdFromCompound(stackNbt.getCompound("custom_data"));
            if (cmd != -1) return cmd;
        }
        *///?} else {
        if (stackNbt.contains("tag")) {
            int cmd = getCmdFromCompound(stackNbt.getCompoundOrEmpty("tag"));
            if (cmd != -1) return cmd;
        }
        if (stackNbt.contains("minecraft:custom_data")) {
            int cmd = getCmdFromCompound(stackNbt.getCompoundOrEmpty("minecraft:custom_data"));
            if (cmd != -1) return cmd;
        }
        if (stackNbt.contains("custom_data")) {
            int cmd = getCmdFromCompound(stackNbt.getCompoundOrEmpty("custom_data"));
            if (cmd != -1) return cmd;
        }
        //?}
        int cmd = getCmdFromCompound(stackNbt);
        if (cmd != -1) return cmd;
        return -1;
    }

    public static ItemStack resolveItemInside(NbtCompound stackNbt) {
        if (stackNbt == null || stackNbt.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 1. Проверяем жидкость
        String fluid = null;
        //? if >=1.21.11 {
        if (stackNbt.contains("ES.Fluid")) {
            fluid = stackNbt.getString("ES.Fluid", "");
        } else if (stackNbt.contains("Fluid")) {
            fluid = stackNbt.getString("Fluid", "");
        }
        //?} elif >1.21.3 {
        /*if (stackNbt.contains("ES.Fluid")) {
            fluid = stackNbt.getString("ES.Fluid", "");
        } else if (stackNbt.contains("Fluid")) {
            fluid = stackNbt.getString("Fluid", "");
        }
        *///?} else {
        /*if (stackNbt.contains("ES.Fluid")) {
            fluid = stackNbt.getString("ES.Fluid");
        } else if (stackNbt.contains("Fluid")) {
            fluid = stackNbt.getString("Fluid");
        }
        *///?}

        if (fluid != null && !fluid.isEmpty()) {
            Integer cmd = getFluidCmd(fluid);
            if (cmd != null) {
                return createStack(Items.SUGAR_CANE, cmd);
            }
        }

        // 2. Получаем ID предмета
        String id = "";
        //? if >=1.21.11 {
        id = stackNbt.getString("id", "");
        if (id.isEmpty()) {
            id = stackNbt.getString("minecraft:item_model", "");
        }
        //?} elif >1.21.3 {
        /*id = stackNbt.getString("id", "");
        if (id.isEmpty()) {
            id = stackNbt.getString("minecraft:item_model", "");
        }
        *///?} else {
        /*if (stackNbt.contains("id")) {
            id = stackNbt.getString("id");
        } else if (stackNbt.contains("minecraft:item_model")) {
            id = stackNbt.getString("minecraft:item_model");
        }
        *///?}

        if (id.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int cmd = extractCmd(stackNbt);

        Item item;
        if ("mechsandtechs:block".equals(id) || id.startsWith("mechsandtechs:")) {
            item = Items.BARRIER;
        } else {
            Identifier identifier = Identifier.tryParse(id);
            item = identifier != null ? Registries.ITEM.get(identifier) : Items.AIR;
            if (item == Items.AIR) {
                if (cmd > 0 || stackNbt.contains("MechID")) {
                    item = Items.BARRIER;
                }
            }
        }

        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return createStack(item, cmd);
    }
}
