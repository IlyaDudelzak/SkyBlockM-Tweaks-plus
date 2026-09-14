package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.ModUtils;
import despairscent.skyblockm.tweaks.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static despairscent.skyblockm.tweaks.ModUtils.CLIENT;
import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private Matrix3x2fStack matrices;

    @Shadow protected abstract void drawItem(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed);

    @Unique
    private boolean isRenderingItemInside = false;

    @Unique
    private boolean didPushOriginalMatrix = false;

    @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD"), cancellable = true)
    private void drawItemInjectHead(LivingEntity entity, World world, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        if (isRenderingItemInside) {
            return;
        }
        if (!CONFIG.renderItemInside.enabled ||
                !itemStack.contains(DataComponentTypes.CUSTOM_MODEL_DATA) ||
                !itemStack.contains(DataComponentTypes.CUSTOM_DATA)) {
            return;
        }

        CustomModelDataComponent cmd = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int modelId = (cmd != null && cmd.getFloat(0) != null) ? Math.round(cmd.getFloat(0)) : 0;
        NbtCompound customData = itemStack.get(DataComponentTypes.CUSTOM_DATA).copyNbt();

        ItemStack itemInside;
        boolean drawOriginal;
        int bgColor;

        if (itemStack.getItem() == Items.PAPER && modelId == 7301) {
            if (!customData.contains("ElectricStorage.RecipeResults") ||
                    !testRender(CONFIG.renderItemInside.esPattern)) {
                return;
            }
            drawOriginal = CONFIG.renderItemInside.esPattern.drawOriginal;
            bgColor = CONFIG.renderItemInside.esPattern.bgColor;
            itemInside = itemStackFromNbtPre1_20_5(
                    customData.getListOrEmpty("ElectricStorage.RecipeResults")
                            .getCompoundOrEmpty(0));
        } else if (itemStack.getItem() == Items.BARRIER && modelId >= 1010 && modelId <= 1013) {
            if (!customData.contains("ItemStack") ||
                    !testRender(CONFIG.renderItemInside.storage)) {
                return;
            }
            drawOriginal = CONFIG.renderItemInside.storage.drawOriginal;
            bgColor = CONFIG.renderItemInside.storage.bgColor;
            itemInside = itemStackFromNbtPre1_20_5(customData.getCompoundOrEmpty("ItemStack"));
        } else if (itemStack.getItem() == Items.IRON_HORSE_ARMOR && modelId == 2001) {
            if (!testRender(CONFIG.renderItemInside.crystalMemory)) {
                return;
            }
            drawOriginal = CONFIG.renderItemInside.crystalMemory.drawOriginal;
            bgColor = CONFIG.renderItemInside.crystalMemory.bgColor;
            itemInside = itemStackFromCrystalMemory(customData);
            if (itemInside == null) {
                return;
            }
        } else {
            return;
        }

        if (bgColor >>> 24 != 0) {
            ((DrawContext) (Object) this).fill(x, y, x + 16, y + 16, bgColor);
        }

        if (itemInside != null && !itemInside.isEmpty()) {
            isRenderingItemInside = true;
            try {
                this.drawItem(entity, world, itemInside, x, y, seed);
            } finally {
                isRenderingItemInside = false;
            }

            if (drawOriginal) {
                this.matrices.pushMatrix();
                this.matrices.translate(x + 8.0f, y);
                this.matrices.scale(0.5f, 0.5f);
                this.matrices.translate(-x, -y);
                didPushOriginalMatrix = true;
            } else {
                ci.cancel();
            }
        }
    }

    @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("RETURN"))
    private void drawItemInjectReturn(LivingEntity entity, World world, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        if (didPushOriginalMatrix) {
            this.matrices.popMatrix();
            didPushOriginalMatrix = false;
        }
    }

    @Unique
    private static boolean testRender(Config.RenderItemInsideItemSetup itemSetup) {
        return itemSetup.enabled && (itemSetup.renderAlways ||
                (CLIENT.currentScreen != null && ModUtils.hasShiftDown()) ||
                (itemSetup instanceof Config.RenderItemInsideItemSetupEsPattern itemSetupEsPattern &&
                        itemSetupEsPattern.forceRenderInsideInterface && ModUtils.testCustomScreen(CLIENT.currentScreen, "electric_storage:interfaces", "\u0003")));
    }

    @Unique
    private static ItemStack itemStackFromCrystalMemory(NbtCompound nbt) {
        Item item;
        int modelId;
        item_definition:
        {
            if (nbt.getCompoundOrEmpty("StoredItem_Display") instanceof NbtCompound nbtStoredItem &&
                    nbtStoredItem.contains("id") &&
                    nbtStoredItem.contains("CustomModelData")) {
                item = Registries.ITEM.get(Identifier.tryParse(nbtStoredItem.getString("id", "")));
                if (item != Items.AIR) {
                    modelId = nbtStoredItem.getInt("CustomModelData", 0);
                    break item_definition;
                }
            }

            if (nbt.contains("StoredItem")) {
                String identifierStr = nbt.getString("StoredItem", "");

                item = Registries.ITEM.get(Identifier.tryParse(identifierStr));
                if (item != Items.AIR) {
                    modelId = 0;
                    break item_definition;
                }

                // TODO: удалить [по ненадобности] после обновы сервера
                switch (identifierStr) {
                    case "general:tin_ingot":
                        item = Items.PAPER;
                        modelId = 204;
                        break item_definition;
                    case "general:bronze_ingot":
                        item = Items.PAPER;
                        modelId = 304;
                        break item_definition;
                    case "general:steel_ingot":
                        item = Items.PAPER;
                        modelId = 1104;
                        break item_definition;
                    case "general:lead_ingot":
                        item = Items.PAPER;
                        modelId = 1004;
                        break item_definition;
                    case "electricity:iridium_shard":
                        item = Items.PAPER;
                        modelId = 4015;
                        break item_definition;
                    case "electricity:sunnarium_shard":
                        item = Items.PAPER;
                        modelId = 4019;
                        break item_definition;
                    case "electricity:silica":
                        item = Items.PAPER;
                        modelId = 4010;
                        break item_definition;
                    case "quantum:enderium":
                        item = Items.PAPER;
                        modelId = 8002;
                        break item_definition;
                }
            }

            return null;
        }

        ItemStack stack = new ItemStack(item);
        if (modelId != 0) {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) modelId), List.of(), List.of(), List.of()));
        }
        return stack;
    }

    @Unique
    private static ItemStack itemStackFromNbtPre1_20_5(NbtCompound stackNbt) {
        ItemStack stack = ItemStack.EMPTY;
        try {
            Item item = Registries.ITEM.get(Identifier.tryParse(stackNbt.getString("id", "")));
            stack = new ItemStack(item);
            if (stackNbt.contains("tag")) {
                NbtCompound nbt = stackNbt.getCompoundOrEmpty("tag");
                if (nbt.contains("CustomModelData")) {
                    stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) nbt.getInt("CustomModelData", 0)), List.of(), List.of(), List.of()));
                }
            }
        } catch (Exception e) {
        }
        return stack;
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String suppressVanillaStackCountText(String stackCountText, net.minecraft.client.font.TextRenderer textRenderer, ItemStack stack, int x, int y) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && !stack.isEmpty()) {
            if (despairscent.skyblockm.tweaks.StoredCountUtils.getStoredCountFormatted(stack) != null || despairscent.skyblockm.tweaks.StoredCountUtils.hasAutocraft(stack)) {
                return "";
            }
        }
        return stackCountText;
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL")
    )
    private void drawStoredCountOverlay(net.minecraft.client.font.TextRenderer textRenderer, ItemStack stack, int x, int y, String stackCountText, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && !stack.isEmpty()) {
            String count = despairscent.skyblockm.tweaks.StoredCountUtils.getStoredCountFormatted(stack);
            if (count != null) {
                boolean isPlus = "+".equals(count);
                float scale = (float) (isPlus ? CONFIG.terminalStackCount.scalePlus : CONFIG.terminalStackCount.scaleDigits);
                if (scale <= 0.05f) {
                    scale = isPlus ? 1.0f : 0.58f;
                }
                this.matrices.pushMatrix();
                this.matrices.translate(x, y);
                this.matrices.scale(scale, scale);
                int textX = isPlus ? (int) ((17.0f / scale) - textRenderer.getWidth(count)) : (int) ((16.0f / scale) - textRenderer.getWidth(count) - 0.5f);
                int textY = isPlus ? (int) ((17.0f / scale) - textRenderer.fontHeight + 1.0f) : (int) ((16.0f / scale) - textRenderer.fontHeight + 0.5f);
                ((DrawContext) (Object) this).drawText(textRenderer, count, textX, textY, 0xFFFFFFFF, true);
                this.matrices.popMatrix();
            }

            if (despairscent.skyblockm.tweaks.StoredCountUtils.hasAutocraft(stack)) {
                float scale = (float) CONFIG.terminalStackCount.scalePlus;
                if (scale <= 0.05f) {
                    scale = 1.0f;
                }
                this.matrices.pushMatrix();
                this.matrices.translate(x, y);
                this.matrices.scale(scale, scale);
                int textX = (int) ((17.0f / scale) - textRenderer.getWidth("+"));
                int textY = 0;
                ((DrawContext) (Object) this).drawText(textRenderer, "+", textX, textY, 0xFFFFFFFF, true);
                this.matrices.popMatrix();
            }
        }
    }

}
