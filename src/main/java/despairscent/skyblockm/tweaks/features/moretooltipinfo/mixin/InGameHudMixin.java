package despairscent.skyblockm.tweaks.features.moretooltipinfo.mixin;

import despairscent.skyblockm.tweaks.ModUtils;
import net.minecraft.client.gui.hud.InGameHud;
//? if >=1.20.5 {
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
//?} else {
/*import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
*///?}
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Redirect(method = "renderHeldItemTooltip",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"))
    private Text itemNameModifier1(ItemStack itemStack) {
        return getExtendedName(itemStack);
    }

    @Redirect(method = "tick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getName()Lnet/minecraft/text/Text;"))
    private Text itemNameModifier2(ItemStack itemStack) {
        return getExtendedName(itemStack);
    }

    @Unique
    private static Text getExtendedName(ItemStack itemStack) {
        if (!CONFIG.moreTooltipInfo.enabled) {
            return itemStack.getName();
        }

        int modelId = ModUtils.getCustomModelId(itemStack);

        if (itemStack.getItem() == Items.BARRIER && modelId >= 1010 && modelId <= 1013) { // storage
            if (!CONFIG.moreTooltipInfo.storage) {
                return itemStack.getName();
            }

            return getExtendedNameForStorage(itemStack);
        } else if (itemStack.getItem() == Items.BARRIER && modelId >= 1020 && modelId <= 1023) { // fluid storage
            if (!CONFIG.moreTooltipInfo.fluidStorage) {
                return itemStack.getName();
            }

            return getExtendedNameForStorage(itemStack);
        } else if (itemStack.getItem() == Items.IRON_HORSE_ARMOR && modelId == 2001) { // memory crystal
            if (!CONFIG.moreTooltipInfo.crystalMemory) {
                return itemStack.getName();
            }

            //? if >=1.20.5 {
            LoreComponent lore = itemStack.get(DataComponentTypes.LORE);
            if (lore == null || lore.styledLines().size() != 4) {
                return itemStack.getName();
            }

            try {
                return Text.empty().append(itemStack.getName())
                        .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                        .append(lore.styledLines().get(0).getSiblings().get(1))
                        .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
            } catch (Exception e) {
            }
            //?} elif =1.20.4 {
            /*NbtCompound nbtDisplay = itemStack.getSubNbt(ItemStack.DISPLAY_KEY);
            if (nbtDisplay == null) {
                return itemStack.getName();
            }
            NbtList lore = nbtDisplay.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
            if (lore.size() != 3) {
                return itemStack.getName();
            }

            try {
                Text itemStr = Text.Serialization.fromJson(lore.getString(0));
                return Text.empty().append(itemStack.getName())
                        .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                        .append(itemStr.getSiblings().get(1))
                        .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
            } catch (Exception e) {
            }
            *///?} else {
            /*NbtCompound nbtDisplay = itemStack.getSubNbt(ItemStack.DISPLAY_KEY);
            if (nbtDisplay == null) {
                return itemStack.getName();
            }
            NbtList lore = nbtDisplay.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
            if (lore.size() != 3) {
                return itemStack.getName();
            }

            try {
                Text itemStr = Text.Serializer.fromJson(lore.getString(0));
                return Text.empty().append(itemStack.getName())
                        .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                        .append(itemStr.getSiblings().get(1))
                        .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
            } catch (Exception e) {
            }
            *///?}
        }

        return itemStack.getName();
    }

    @Unique
    private static Text getExtendedNameForStorage(ItemStack itemStack) {
        //? if >=1.20.5 {
        LoreComponent lore = itemStack.get(DataComponentTypes.LORE);
        if (lore == null || lore.styledLines().size() != 4) {
            return itemStack.getName();
        }

        try {
            return Text.empty().append(itemStack.getName())
                    .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                    .append(lore.styledLines().get(3))
                    .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
        } catch (Exception e) {
        }
        //?} elif =1.20.4 {
        /*NbtCompound nbtDisplay = itemStack.getSubNbt(ItemStack.DISPLAY_KEY);
        if (nbtDisplay == null) {
            return itemStack.getName();
        }
        NbtList lore = nbtDisplay.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
        if (lore.size() != 4) {
            return itemStack.getName();
        }

        try {
            Text itemStr = Text.Serialization.fromJson(lore.getString(3));
            return Text.empty().append(itemStack.getName())
                    .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                    .append(itemStr)
                    .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
        } catch (Exception e) {
        }
        *///?} else {
        /*NbtCompound nbtDisplay = itemStack.getSubNbt(ItemStack.DISPLAY_KEY);
        if (nbtDisplay == null) {
            return itemStack.getName();
        }
        NbtList lore = nbtDisplay.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
        if (lore.size() != 4) {
            return itemStack.getName();
        }

        try {
            Text itemStr = Text.Serializer.fromJson(lore.getString(3));
            return Text.empty().append(itemStack.getName())
                    .append(Text.literal(" <").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)))
                    .append(itemStr)
                    .append(Text.literal(">").styled(style -> style.withColor(Formatting.WHITE).withItalic(false)));
        } catch (Exception e) {
        }
        *///?}

        return itemStack.getName();
    }

}
