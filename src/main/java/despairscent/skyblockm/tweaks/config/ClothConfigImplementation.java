package despairscent.skyblockm.tweaks.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import despairscent.skyblockm.tweaks.config.ItemDisplayHitboxConfig;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;
import static despairscent.skyblockm.tweaks.ModUtils.i18n;

public class ClothConfigImplementation {

    static Screen generate(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(i18n("config.base.title"));
        ConfigCategory base = builder.getOrCreateCategory(Text.empty());
        
        Text moduleSetupText = i18n("config.base.moduleSetup");

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.storageTargetingFix"), CONFIG.storageTargetingFix.enabled)
                .setTooltip(i18n("config.storageTargetingFix.tooltip"))
                .setDefaultValue(Config.DEFAULT.storageTargetingFix.enabled)
                .setSaveConsumer(value -> CONFIG.storageTargetingFix.enabled = value)
                .build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.moreTooltipInfo"), CONFIG.moreTooltipInfo.enabled)
                .setTooltip(i18n("config.moreTooltipInfo.tooltip"))
                .setDefaultValue(Config.DEFAULT.moreTooltipInfo.enabled)
                .setSaveConsumer(value -> CONFIG.moreTooltipInfo.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startBooleanToggle(i18n("config.moreTooltipInfo.storage"), CONFIG.moreTooltipInfo.storage)
                        .setTooltip(i18n("config.moreTooltipInfo.storage.tooltip"))
                        .setDefaultValue(Config.DEFAULT.moreTooltipInfo.storage)
                        .setSaveConsumer(value -> CONFIG.moreTooltipInfo.storage = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.moreTooltipInfo.fluidStorage"), CONFIG.moreTooltipInfo.fluidStorage)
                        .setTooltip(i18n("config.moreTooltipInfo.fluidStorage.tooltip"))
                        .setDefaultValue(Config.DEFAULT.moreTooltipInfo.fluidStorage)
                        .setSaveConsumer(value -> CONFIG.moreTooltipInfo.fluidStorage = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.moreTooltipInfo.crystalMemory"), CONFIG.moreTooltipInfo.crystalMemory)
                        .setTooltip(i18n("config.moreTooltipInfo.crystalMemory.tooltip"))
                        .setDefaultValue(Config.DEFAULT.moreTooltipInfo.crystalMemory)
                        .setSaveConsumer(value -> CONFIG.moreTooltipInfo.crystalMemory = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.renderItemInside"), CONFIG.renderItemInside.enabled)
                .setTooltip(i18n("config.renderItemInside.tooltip"))
                .setDefaultValue(Config.DEFAULT.renderItemInside.enabled)
                .setSaveConsumer(value -> CONFIG.renderItemInside.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startSubCategory(i18n("config.renderItemInside.item.esPattern"),
                        prepareRenderItemInsideSetup(builder, config -> config.renderItemInside.esPattern,
                                builder.entryBuilder().startBooleanToggle(i18n("config.renderItemInside.itemSetupEsPattern.forceRenderInsideInterface"), CONFIG.renderItemInside.esPattern.forceRenderInsideInterface)
                                        .setDefaultValue(Config.DEFAULT.renderItemInside.esPattern.forceRenderInsideInterface)
                                        .setSaveConsumer(value -> CONFIG.renderItemInside.esPattern.forceRenderInsideInterface = value)
                                        .build()
                        )).build(),
                builder.entryBuilder().startSubCategory(i18n("config.renderItemInside.item.storage"),
                        prepareRenderItemInsideSetup(builder, config -> config.renderItemInside.storage)).build(),
                builder.entryBuilder().startSubCategory(i18n("config.renderItemInside.item.crystalMemory"),
                        prepareRenderItemInsideSetup(builder, config -> config.renderItemInside.crystalMemory)).build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.textInputLagFix"), CONFIG.textInputLagFix.enabled)
                .setDefaultValue(Config.DEFAULT.textInputLagFix.enabled)
                .setSaveConsumer(value -> CONFIG.textInputLagFix.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startBooleanToggle(i18n("config.textInputLagFix.recipesSearch"), CONFIG.textInputLagFix.recipesSearch)
                        .setDefaultValue(Config.DEFAULT.textInputLagFix.recipesSearch)
                        .setSaveConsumer(value -> CONFIG.textInputLagFix.recipesSearch = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.textInputLagFix.esTerminalSearch"), CONFIG.textInputLagFix.esTerminalSearch)
                        .setDefaultValue(Config.DEFAULT.textInputLagFix.esTerminalSearch)
                        .setSaveConsumer(value -> CONFIG.textInputLagFix.esTerminalSearch = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.inventoryDesyncFix"), CONFIG.inventoryDesyncFix.enabled)
                .setTooltip(i18n("config.inventoryDesyncFix.tooltip"))
                .setDefaultValue(Config.DEFAULT.inventoryDesyncFix.enabled)
                .setSaveConsumer(value -> CONFIG.inventoryDesyncFix.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startBooleanToggle(i18n("config.inventoryDesyncFix.inventoryUpdates"), CONFIG.inventoryDesyncFix.inventoryUpdates)
                        .setDefaultValue(Config.DEFAULT.inventoryDesyncFix.inventoryUpdates)
                        .setSaveConsumer(value -> CONFIG.inventoryDesyncFix.inventoryUpdates = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.inventoryDesyncFix.selectedSlot"), CONFIG.inventoryDesyncFix.selectedSlot)
                        .setDefaultValue(Config.DEFAULT.inventoryDesyncFix.selectedSlot)
                        .setSaveConsumer(value -> CONFIG.inventoryDesyncFix.selectedSlot = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.esTerminalScroll"), CONFIG.esTerminalScroll.enabled)
                .setDefaultValue(Config.DEFAULT.esTerminalScroll.enabled)
                .setSaveConsumer(value -> CONFIG.esTerminalScroll.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startBooleanToggle(i18n("config.esTerminalScroll.wheel"), CONFIG.esTerminalScroll.wheel)
                        .setDefaultValue(Config.DEFAULT.esTerminalScroll.wheel)
                        .setSaveConsumer(value -> CONFIG.esTerminalScroll.wheel = value)
                        .build(),
                builder.entryBuilder().startKeyCodeField(i18n("config.esTerminalScroll.wheelModifier"), InputUtil.Type.KEYSYM.createFromCode(CONFIG.esTerminalScroll.wheelModifier))
                        .setAllowModifiers(false)
                        .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(Config.DEFAULT.esTerminalScroll.wheelModifier))
                        .setKeySaveConsumer(value -> CONFIG.esTerminalScroll.wheelModifier = value.getCode())
                        .build(),
                builder.entryBuilder().startKeyCodeField(i18n("config.esTerminalScroll.keyUp"), InputUtil.Type.KEYSYM.createFromCode(CONFIG.esTerminalScroll.keyUp))
                        .setAllowModifiers(false)
                        .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(Config.DEFAULT.esTerminalScroll.keyUp))
                        .setKeySaveConsumer(value -> CONFIG.esTerminalScroll.keyUp = value.getCode())
                        .build(),
                builder.entryBuilder().startKeyCodeField(i18n("config.esTerminalScroll.keyDown"), InputUtil.Type.KEYSYM.createFromCode(CONFIG.esTerminalScroll.keyDown))
                        .setAllowModifiers(false)
                        .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(Config.DEFAULT.esTerminalScroll.keyDown))
                        .setKeySaveConsumer(value -> CONFIG.esTerminalScroll.keyDown = value.getCode())
                        .build(),
                builder.entryBuilder().startIntField(i18n("config.esTerminalScroll.actionLimitWheel"), CONFIG.esTerminalScroll.actionLimitWheel)
                        .setTooltip(i18n("config.esTerminalScroll.actionLimitWheel.tooltip"))
                        .setDefaultValue(Config.DEFAULT.esTerminalScroll.actionLimitWheel)
                        .setSaveConsumer(value -> CONFIG.esTerminalScroll.actionLimitWheel = value)
                        .build(),
                builder.entryBuilder().startIntField(i18n("config.esTerminalScroll.actionLimitKey"), CONFIG.esTerminalScroll.actionLimitKey)
                        .setTooltip(i18n("config.esTerminalScroll.actionLimitKey.tooltip"))
                        .setDefaultValue(Config.DEFAULT.esTerminalScroll.actionLimitKey)
                        .setSaveConsumer(value -> CONFIG.esTerminalScroll.actionLimitKey = value)
                        .build(),
                builder.entryBuilder().startKeyCodeField(i18n("config.esTerminalScroll.boostKey"), InputUtil.Type.KEYSYM.createFromCode(CONFIG.esTerminalScroll.boostKey))
                        .setTooltip(i18n("config.esTerminalScroll.boostKey.tooltip"))
                        .setAllowModifiers(false)
                        .setDefaultValue(InputUtil.Type.KEYSYM.createFromCode(Config.DEFAULT.esTerminalScroll.boostKey))
                        .setKeySaveConsumer(value -> CONFIG.esTerminalScroll.boostKey = value.getCode())
                        .build(),
                builder.entryBuilder().startEnumSelector(i18n("config.esTerminalScroll.boostKeyType"), Config.EsTerminalScrollBoostKeyType.class, CONFIG.esTerminalScroll.boostKeyType)
                        .setTooltip(i18n("config.esTerminalScroll.boostKeyType.tooltip"))
                        .setEnumNameProvider(v -> ((Config.EsTerminalScrollBoostKeyType) v).optionName)
                        .setDefaultValue(Config.DEFAULT.esTerminalScroll.boostKeyType)
                        .setSaveConsumer(value -> CONFIG.esTerminalScroll.boostKeyType = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.compactGenome"), CONFIG.compactGenome.enabled)
                .setTooltip(i18n("config.compactGenome.tooltip"))
                .setDefaultValue(Config.DEFAULT.compactGenome.enabled)
                .setSaveConsumer(value -> CONFIG.compactGenome.enabled = value)
                .build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.hideHiddenArmorStands"), CONFIG.hideHiddenArmorStands.enabled)
                .setTooltip(i18n("config.hideHiddenArmorStands.tooltip"))
                .setDefaultValue(Config.DEFAULT.hideHiddenArmorStands.enabled)
                .setSaveConsumer(value -> CONFIG.hideHiddenArmorStands.enabled = value)
                .build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.itemDisplayBaking"), CONFIG.itemDisplayBaking.enabled)
                .setTooltip(i18n("config.itemDisplayBaking.tooltip"))
                .setDefaultValue(Config.DEFAULT.itemDisplayBaking.enabled)
                .setSaveConsumer(value -> CONFIG.itemDisplayBaking.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startDoubleField(i18n("config.itemDisplayBaking.maxBakeSize"), CONFIG.itemDisplayBaking.maxBakeSize)
                        .setTooltip(i18n("config.itemDisplayBaking.maxBakeSize.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayBaking.maxBakeSize)
                        .setSaveConsumer(value -> CONFIG.itemDisplayBaking.maxBakeSize = value)
                        .build(),
                builder.entryBuilder().startIntField(i18n("config.itemDisplayBaking.bakeDelayTicks"), CONFIG.itemDisplayBaking.bakeDelayTicks)
                        .setTooltip(i18n("config.itemDisplayBaking.bakeDelayTicks.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayBaking.bakeDelayTicks)
                        .setSaveConsumer(value -> CONFIG.itemDisplayBaking.bakeDelayTicks = value)
                        .build(),
                builder.entryBuilder().startDoubleField(i18n("config.itemDisplayBaking.chunkSettleTime"), CONFIG.itemDisplayBaking.chunkSettleTime)
                        .setTooltip(i18n("config.itemDisplayBaking.chunkSettleTime.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayBaking.chunkSettleTime)
                        .setSaveConsumer(value -> CONFIG.itemDisplayBaking.chunkSettleTime = value)
                        .build(),
                builder.entryBuilder().startDoubleField(i18n("config.itemDisplayBaking.chunkRebuildCooldown"), CONFIG.itemDisplayBaking.chunkRebuildCooldown)
                        .setTooltip(i18n("config.itemDisplayBaking.chunkRebuildCooldown.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayBaking.chunkRebuildCooldown)
                        .setSaveConsumer(value -> CONFIG.itemDisplayBaking.chunkRebuildCooldown = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.itemDisplayBaking.hideBakeableEntities"), CONFIG.itemDisplayBaking.hideBakeableEntities)
                        .setTooltip(i18n("config.itemDisplayBaking.hideBakeableEntities.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayBaking.hideBakeableEntities)
                        .setSaveConsumer(value -> CONFIG.itemDisplayBaking.hideBakeableEntities = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.itemDisplayHitbox"), CONFIG.itemDisplayHitbox.enabled)
                .setTooltip(i18n("config.itemDisplayHitbox.tooltip"))
                .setDefaultValue(Config.DEFAULT.itemDisplayHitbox.enabled)
                .setSaveConsumer(value -> CONFIG.itemDisplayHitbox.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startSubCategory(moduleSetupText, Arrays.asList(
                builder.entryBuilder().startEnumSelector(i18n("config.itemDisplayHitbox.hitboxType"), ItemDisplayHitboxConfig.HitboxType.class, CONFIG.itemDisplayHitbox.hitboxType)
                        .setDefaultValue(Config.DEFAULT.itemDisplayHitbox.hitboxType)
                        .setSaveConsumer(value -> CONFIG.itemDisplayHitbox.hitboxType = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.itemDisplayHitbox.antiRubberband"), CONFIG.itemDisplayHitbox.antiRubberband)
                        .setTooltip(i18n("config.itemDisplayHitbox.antiRubberband.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayHitbox.antiRubberband)
                        .setSaveConsumer(value -> CONFIG.itemDisplayHitbox.antiRubberband = value)
                        .build(),
                builder.entryBuilder().startDoubleField(i18n("config.itemDisplayHitbox.antiRubberbandDistance"), CONFIG.itemDisplayHitbox.antiRubberbandDistance)
                        .setTooltip(i18n("config.itemDisplayHitbox.antiRubberbandDistance.tooltip"))
                        .setDefaultValue(Config.DEFAULT.itemDisplayHitbox.antiRubberbandDistance)
                        .setSaveConsumer(value -> CONFIG.itemDisplayHitbox.antiRubberbandDistance = value)
                        .build()
        )).build());

        base.addEntry(builder.entryBuilder().startEnumSelector(i18n("config.skyblockPackOptimization.mode"), Config.AutoLoadMode.class, CONFIG.skyblockPackOptimization.mode)
                .setTooltip(i18n("config.skyblockPackOptimization.mode.tooltip"))
                .setEnumNameProvider(v -> ((Config.AutoLoadMode) v).optionName)
                .setDefaultValue(Config.DEFAULT.skyblockPackOptimization.mode)
                .setSaveConsumer(value -> {
                    CONFIG.skyblockPackOptimization.mode = value;
                    CONFIG.skyblockPackOptimization.enabled = value != Config.AutoLoadMode.NONE;
                })
                .build());

        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.serverPackUnlocker"), CONFIG.serverPackUnlocker.enabled)
                .setTooltip(i18n("config.serverPackUnlocker.tooltip"))
                .setDefaultValue(Config.DEFAULT.serverPackUnlocker.enabled)
                .setSaveConsumer(value -> CONFIG.serverPackUnlocker.enabled = value)
                .build());
        base.addEntry(builder.entryBuilder().startBooleanToggle(i18n("config.adBlocker"), CONFIG.adBlocker.enabled)
                .setTooltip(i18n("config.adBlocker.tooltip"))
                .setDefaultValue(Config.DEFAULT.adBlocker.enabled)
                .setSaveConsumer(value -> CONFIG.adBlocker.enabled = value)
                .build());

        builder.setSavingRunnable(() -> CONFIG.save());

        return builder.build();
    }

    private static List<AbstractConfigListEntry> prepareRenderItemInsideSetup(ConfigBuilder builder, Function<Config, Config.RenderItemInsideItemSetup> subGetter, AbstractConfigListEntry... additional) {
        List<AbstractConfigListEntry> list = Arrays.asList(
                builder.entryBuilder().startBooleanToggle(i18n("config.renderItemInside.itemSetup.enabled"), subGetter.apply(CONFIG).enabled)
                        .setDefaultValue(subGetter.apply(Config.DEFAULT).enabled)
                        .setSaveConsumer(value -> subGetter.apply(CONFIG).enabled = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.renderItemInside.itemSetup.renderAlways"), subGetter.apply(CONFIG).renderAlways)
                        .setDefaultValue(subGetter.apply(Config.DEFAULT).renderAlways)
                        .setSaveConsumer(value -> subGetter.apply(CONFIG).renderAlways = value)
                        .build(),
                builder.entryBuilder().startBooleanToggle(i18n("config.renderItemInside.itemSetup.drawOriginal"), subGetter.apply(CONFIG).drawOriginal)
                        .setDefaultValue(subGetter.apply(Config.DEFAULT).drawOriginal)
                        .setSaveConsumer(value -> subGetter.apply(CONFIG).drawOriginal = value)
                        .build(),
                builder.entryBuilder().startAlphaColorField(i18n("config.renderItemInside.itemSetup.bgColor"), subGetter.apply(CONFIG).bgColor)
                        .setDefaultValue(subGetter.apply(Config.DEFAULT).bgColor)
                        .setSaveConsumer(value -> subGetter.apply(CONFIG).bgColor = value)
                        .build()
        );
        if (additional.length != 0) {
            list = new ArrayList<>(list);
            list.addAll(Arrays.asList(additional));
        }
        return list;
    }

}
