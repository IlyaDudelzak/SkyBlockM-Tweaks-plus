package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class NuclearCalculatorScreen extends Screen {

    //? if <=1.20.4 {
    /*private static final Identifier TEXTURE_GENERIC_54 = new Identifier("textures/gui/container/generic_54.png");
    private static final Identifier TEXTURE_TOP = new Identifier("electricity", "textures/gui/nuclear_reactor.png");
    private static final Identifier TEXTURE_BOTTOM = new Identifier("electricity", "textures/gui/nuclear_reactor_bottom.png");
    *///?} else {
    private static final Identifier TEXTURE_GENERIC_54 = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    private static final Identifier TEXTURE_TOP = Identifier.of("electricity", "textures/gui/nuclear_reactor.png");
    private static final Identifier TEXTURE_BOTTOM = Identifier.of("electricity", "textures/gui/nuclear_reactor_bottom.png");
    //?}

    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 222;
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int SLOT_SIZE = 18;

    // Панелька слева от реактора
    private static final int PANEL_WIDTH = 72;
    private static final int PANEL_HEIGHT = 146;
    private static final int PALETTE_COLS = 3;
    private static final int PALETTE_ROWS = 6;
    private static final int PALETTE_SLOT_SIZE = 19;

    public final Screen parent;
    private final ReactorComponent[] grid = new ReactorComponent[54];
    private final ItemStack[] displayStacks = new ItemStack[54];
    private ReactorSimulator simulator;

    // Инвентарь игрока в нижней половине generic_54 (36 слотов: 27 инвентарь + 9 хотбар)
    private final ItemStack[] playerInventoryStacks = new ItemStack[36];

    // Палитра компонентов (панелька слева: 18 компонентов)
    private final PaletteEntry[] palette = new PaletteEntry[18];

    // Курсор ("призрак предмета" в руке)
    private ItemStack cursorStack = ItemStack.EMPTY;
    private Supplier<ReactorComponent> cursorSupplier = null;
    private boolean cursorIsEraser = false;

    private int guiLeft;
    private int guiTop;
    private int panelX;
    private int panelY;
    private int lastInteractedSlot = -1;

    private boolean isAutoTicking = false;
    private int tickTimer = 0;
    private boolean neutronReflector = false;

    private ButtonWidget btnStartStop;
    private ButtonWidget btnReflector;
    private ButtonWidget btnHeatPreset;

    public static class PaletteEntry {
        public final ItemStack stack;
        public final Supplier<ReactorComponent> supplier;
        public final String name;
        public final String[] description;
        public final boolean isEraser;

        public PaletteEntry(ItemStack stack, Supplier<ReactorComponent> supplier, String name, String[] description, boolean isEraser) {
            this.stack = stack;
            this.supplier = supplier;
            this.name = name;
            this.description = description;
            this.isEraser = isEraser;
        }
    }

    public NuclearCalculatorScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
        this.simulator = new ReactorSimulator(this.grid);
        Arrays.fill(this.displayStacks, ItemStack.EMPTY);
        Arrays.fill(this.playerInventoryStacks, ItemStack.EMPTY);
        initPalette();
    }

    private void initPalette() {
        // Ряд 0: Урановые стержни (1x, 2x, 4x)
        palette[0] = new PaletteEntry(
                ModelDataHelper.createReactorStack(240, "Урановый стержень"),
                FuelRod::singleUranium,
                "§aУрановый стержень (1x)",
                new String[]{"§7Энергия: §f320 Вт (+320 за соседа)", "§7Тепло: §f4 HU/s (+6 за соседа)"},
                false
        );
        palette[1] = new PaletteEntry(
                ModelDataHelper.createReactorStack(241, "Сдвоенный урановый стержень"),
                FuelRod::dualUranium,
                "§aСдвоенный урановый стержень (2x)",
                new String[]{"§7Энергия: §f1280 Вт (+640 за соседа)", "§7Тепло: §f24 HU/s (+36 за соседа)"},
                false
        );
        palette[2] = new PaletteEntry(
                ModelDataHelper.createReactorStack(242, "Счетверенный урановый стержень"),
                FuelRod::quadUranium,
                "§aСчетверенный урановый стержень (4x)",
                new String[]{"§7Энергия: §f2560 Вт (+1280 за соседа)", "§7Тепло: §f96 HU/s (+144 за соседа)"},
                false
        );

        // Ряд 1: MOX стержни (1x, 2x, 4x)
        palette[3] = new PaletteEntry(
                ModelDataHelper.createReactorStack(243, "MOX стержень"),
                FuelRod::singleMox,
                "§6MOX стержень (1x)",
                new String[]{"§7Энергия: §f320 Вт (растет до 5x от тепла)", "§7Тепло: §f4 HU/s (+6 за соседа)"},
                false
        );
        palette[4] = new PaletteEntry(
                ModelDataHelper.createReactorStack(244, "Сдвоенный MOX стержень"),
                FuelRod::dualMox,
                "§6Сдвоенный MOX стержень (2x)",
                new String[]{"§7Энергия: §f1280 Вт (растет до 5x от тепла)", "§7Тепло: §f24 HU/s (+36 за соседа)"},
                false
        );
        palette[5] = new PaletteEntry(
                ModelDataHelper.createReactorStack(245, "Счетверенный MOX стержень"),
                FuelRod::quadMox,
                "§6Счетверенный MOX стержень (4x)",
                new String[]{"§7Энергия: §f2560 Вт (растет до 5x от тепла)", "§7Тепло: §f96 HU/s (+144 за соседа)"},
                false
        );

        // Ряд 2: Охлаждающие стержни (10k, 30k, 60k)
        palette[6] = new PaletteEntry(
                ModelDataHelper.createReactorStack(210, "Охлаждающий стержень 10k"),
                CoolingElement::element10k,
                "§bОхлаждающий стержень 10k",
                new String[]{"§7Емкость: §f10 000 тепла", "§7Не рассеивает тепло сам"},
                false
        );
        palette[7] = new PaletteEntry(
                ModelDataHelper.createReactorStack(211, "Охлаждающий стержень 30k"),
                CoolingElement::element30k,
                "§bОхлаждающий стержень 30k",
                new String[]{"§7Емкость: §f30 000 тепла", "§7Не рассеивает тепло сам"},
                false
        );
        palette[8] = new PaletteEntry(
                ModelDataHelper.createReactorStack(212, "Охлаждающий стержень 60k"),
                CoolingElement::element60k,
                "§bОхлаждающий стержень 60k",
                new String[]{"§7Емкость: §f60 000 тепла", "§7Не рассеивает тепло сам"},
                false
        );

        // Ряд 3: Теплоотводы (базовый, улучшенный, реакторный)
        palette[9] = new PaletteEntry(
                ModelDataHelper.createReactorStack(230, "Базовый теплоотвод"),
                HeatVent::basic,
                "§fБазовый теплоотвод",
                new String[]{"§7Емкость: §f1000 тепла", "§7Отвод от себя: §a12 HU/s"},
                false
        );
        palette[10] = new PaletteEntry(
                ModelDataHelper.createReactorStack(231, "Улучшенный теплоотвод"),
                HeatVent::advanced,
                "§fУлучшенный теплоотвод",
                new String[]{"§7Емкость: §f1000 тепла", "§7Отвод от себя: §a24 HU/s"},
                false
        );
        palette[11] = new PaletteEntry(
                ModelDataHelper.createReactorStack(233, "Реакторный теплоотвод"),
                HeatVent::reactor,
                "§fРеакторный теплоотвод",
                new String[]{"§7Емкость: §f1000 тепла", "§7Отвод от себя: §a12 HU/s", "§7Забор из корпуса: §a12 HU/s"},
                false
        );

        // Ряд 4: Теплоотводы и теплообменники (компонентный, разогнанный, базовый теплообменник)
        palette[12] = new PaletteEntry(
                ModelDataHelper.createReactorStack(232, "Компонентный теплоотвод"),
                HeatVent::component,
                "§fКомпонентный теплоотвод",
                new String[]{"§7Емкость: §fнет (не греется)", "§7Охлаждение 4 соседей: §a10 HU/s"},
                false
        );
        palette[13] = new PaletteEntry(
                ModelDataHelper.createReactorStack(234, "Разогнанный теплоотвод"),
                HeatVent::overclocked,
                "§fРазогнанный теплоотвод",
                new String[]{"§7Емкость: §f1000 тепла", "§7Отвод от себя: §a40 HU/s", "§7Забор из корпуса: §a60 HU/s"},
                false
        );
        palette[14] = new PaletteEntry(
                ModelDataHelper.createReactorStack(220, "Базовый теплообменник"),
                HeatExchanger::basic,
                "§fБазовый теплообменник",
                new String[]{"§7Емкость: §f2500 тепла", "§7Обмен с соседями: §a12 HU/s", "§7Обмен с корпусом: §a4 HU/s"},
                false
        );

        // Ряд 5: Теплообменники и ластик
        palette[15] = new PaletteEntry(
                ModelDataHelper.createReactorStack(221, "Улучшенный теплообменник"),
                HeatExchanger::advanced,
                "§fУлучшенный теплообменник",
                new String[]{"§7Емкость: §f10000 тепла", "§7Обмен с соседями: §a24 HU/s", "§7Обмен с корпусом: §a8 HU/s"},
                false
        );
        palette[16] = new PaletteEntry(
                ModelDataHelper.createReactorStack(222, "Компонентный теплообменник"),
                HeatExchanger::component,
                "§fКомпонентный теплообменник",
                new String[]{"§7Емкость: §f5000 тепла", "§7Обмен с соседями: §a24 HU/s"},
                false
        );
        palette[17] = new PaletteEntry(
                new ItemStack(Items.BARRIER),
                () -> null,
                "§cОчистить слот (Ластик)",
                new String[]{"§7Нажмите или проведите по слоту,", "§7чтобы убрать компонент"},
                true
        );
    }

    public void setSlot(int index, ReactorComponent component, ItemStack originalStack) {
        if (index >= 0 && index < 54) {
            this.grid[index] = component;
            this.displayStacks[index] = (component != null && originalStack != null && !originalStack.isEmpty())
                    ? originalStack.copy()
                    : ItemStack.EMPTY;
            int prevHull = (this.simulator != null) ? this.simulator.getHullHeat() : 0;
            this.simulator = new ReactorSimulator(this.grid);
            this.simulator.setNeutronReflector(this.neutronReflector);
            this.simulator.setHullHeat(prevHull);
            this.simulator.recalculateOutput();
        }
    }

    public void setPlayerInventorySlot(int index, ItemStack stack) {
        if (index >= 0 && index < 36 && stack != null) {
            this.playerInventoryStacks[index] = stack.copy();
        }
    }

    public void setInitialHeatPercent(double percent) {
        int heat = (int) Math.round((percent / 100.0) * 10000);
        if (this.simulator != null) {
            this.simulator.setHullHeat(heat);
            this.simulator.recalculateOutput();
            updateHeatButtonMessage();
        }
    }

    private void resetHeat() {
        for (ReactorComponent comp : this.grid) {
            if (comp != null) {
                comp.setHeat(0);
            }
        }
        this.simulator = new ReactorSimulator(this.grid);
        this.simulator.setNeutronReflector(this.neutronReflector);
        this.simulator.setHullHeat(0);
        this.simulator.recalculateOutput();
        updateHeatButtonMessage();
    }

    private void clearAll() {
        Arrays.fill(this.grid, null);
        Arrays.fill(this.displayStacks, ItemStack.EMPTY);
        this.simulator = new ReactorSimulator(this.grid);
        this.simulator.setNeutronReflector(this.neutronReflector);
        this.simulator.setHullHeat(0);
        this.simulator.recalculateOutput();
        updateHeatButtonMessage();
    }

    private void toggleReflector() {
        this.neutronReflector = !this.neutronReflector;
        this.simulator.setNeutronReflector(this.neutronReflector);
        this.simulator.recalculateOutput();
        if (btnReflector != null) {
            btnReflector.setMessage(Text.literal(this.neutronReflector ? "§aОтраж: x4" : "§7Отраж: x1"));
        }
    }

    private void cycleHeatPreset() {
        int current = simulator.getHullHeat();
        int next;
        if (current < 2500) {
            next = 5000; // 50%
        } else if (current < 7000) {
            next = 8500; // 85%
        } else if (current < 9500) {
            next = 9900; // 99%
        } else {
            next = 0; // 0%
        }
        simulator.setHullHeat(next);
        simulator.recalculateOutput();
        updateHeatButtonMessage();
    }

    private void adjustHeat(int delta) {
        int current = simulator.getHullHeat();
        int newHeat = Math.max(0, Math.min(9999, current + delta));
        simulator.setHullHeat(newHeat);
        simulator.recalculateOutput();
        updateHeatButtonMessage();
    }

    private void updateHeatButtonMessage() {
        if (btnHeatPreset != null) {
            btnHeatPreset.setMessage(Text.literal(String.format("Ядро: %.0f%%", simulator.getHullHeatPercent())));
        }
    }

    @Override
    protected void init() {
        super.init();

        // Позиция основного окна реактора (двойной сундук 176x222)
        this.guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        this.guiTop = (this.height - BACKGROUND_HEIGHT) / 2;

        // Позиция панельки слева (палитра компонентов)
        this.panelX = this.guiLeft - PANEL_WIDTH - 4;
        this.panelY = this.guiTop;

        int btnX = this.guiLeft + 180;
        int btnWidth = 76;
        int btnHeight = 20;

        // Кнопки управления справа от окна реактора
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Шаг +1с"), btn -> {
            simulator.tick();
            updateHeatButtonMessage();
        }).dimensions(btnX, this.guiTop + 18, btnWidth, btnHeight).build());

        this.btnStartStop = this.addDrawableChild(ButtonWidget.builder(Text.literal(isAutoTicking ? "Стоп" : "Пуск"), btn -> {
            this.isAutoTicking = !this.isAutoTicking;
            btn.setMessage(Text.literal(isAutoTicking ? "Стоп" : "Пуск"));
        }).dimensions(btnX, this.guiTop + 40, btnWidth, btnHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сброс"), btn -> resetHeat())
                .dimensions(btnX, this.guiTop + 62, btnWidth, btnHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Очистить"), btn -> clearAll())
                .dimensions(btnX, this.guiTop + 84, btnWidth, btnHeight).build());

        // Переключатель отражателя нейтронов (увеличивает выработку энергии в 4 раза)
        this.btnReflector = this.addDrawableChild(ButtonWidget.builder(
                Text.literal(this.neutronReflector ? "§aОтраж: x4" : "§7Отраж: x1"),
                btn -> toggleReflector()
        ).dimensions(btnX, this.guiTop + 108, btnWidth, btnHeight).build());

        // Настройка тепла ядра
        this.btnHeatPreset = this.addDrawableChild(ButtonWidget.builder(
                Text.literal(String.format("Ядро: %.0f%%", simulator.getHullHeatPercent())),
                btn -> cycleHeatPreset()
        ).dimensions(btnX, this.guiTop + 132, btnWidth, btnHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("-10%"), btn -> adjustHeat(-1000))
                .dimensions(btnX, this.guiTop + 154, 36, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("+10%"), btn -> adjustHeat(1000))
                .dimensions(btnX + 40, this.guiTop + 154, 36, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), btn -> this.close())
                .dimensions(btnX, this.guiTop + 198, btnWidth, btnHeight).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (isAutoTicking) {
            tickTimer++;
            if (tickTimer >= 20) { // 20 тиков = 1 секунда
                tickTimer = 0;
                simulator.tick();
                updateHeatButtonMessage();
                if (simulator.getHullHeatPercent() >= 100.0) {
                    isAutoTicking = false;
                    if (btnStartStop != null) {
                        btnStartStop.setMessage(Text.literal("Пуск"));
                    }
                }
            }
        }
    }

    private void drawTextureRegion(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height) {
        //? if <=1.21.1 {
        /*context.drawTexture(texture, x, y, u, v, width, height, 256, 256);
        *///?} elif <=1.21.3 {
        /*context.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                texture,
                x, y,
                u, v,
                width, height,
                256, 256
        );
        *///?} else {
        context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                texture,
                x, y,
                u, v,
                width, height,
                256, 256
        );
        //?}
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Затемнение фона
        context.fill(0, 0, this.width, this.height, 0x80000000);

        // 2. Отрисовка базовой текстуры двойного сундука (176x222)
        drawTextureRegion(context, TEXTURE_GENERIC_54, this.guiLeft, this.guiTop, 0.0F, 0.0F, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

        // 3. Оверлей верхней рамки реактора с табличкой «ЯДЕРНЫЙ РЕАКТОР» (176x131)
        drawTextureRegion(context, TEXTURE_TOP, this.guiLeft, this.guiTop - 6, 0.0F, 0.0F, 176, 131);

        // 4. Отрисовка нижней части реактора (nuclear_reactor_bottom.png - плашка статуса)
        context.fill(this.guiLeft + 7, this.guiTop + 125, this.guiLeft + 169, this.guiTop + 137, 0xFF212121);
        drawTextureRegion(context, TEXTURE_BOTTOM, this.guiLeft, this.guiTop + 125, 0.0F, 131.0F, 176, 12);

        // 5. Вывод статуса реактора (цифры тепла и мощности белым текстом в нижней планке)
        String statusText;
        if (simulator.getHullHeatPercent() >= 100.0) {
            statusText = String.format("Выход: %d Вт | Нагрев: %.0f%% §4(ВЗРЫВ!)",
                    simulator.getOutputWatts(),
                    simulator.getHullHeatPercent());
        } else {
            statusText = String.format("Выход: %d Вт | Нагрев ядра: %.0f%%",
                    simulator.getOutputWatts(),
                    simulator.getHullHeatPercent());
        }
        int textWidth = this.textRenderer.getWidth(statusText);
        int textX = this.guiLeft + Math.max(7, (176 - textWidth) / 2);
        context.drawText(this.textRenderer, Text.literal(statusText), textX, this.guiTop + 127, 0xFFFFFFFF, false);

        // 7. Отрисовка сетки реактора (54 слота)
        int gridStartX = this.guiLeft + 8;
        int gridStartY = this.guiTop + 18;
        ReactorComponent[] simGrid = simulator.getGrid();
        int hoveredReactorSlot = -1;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int slotX = gridStartX + c * SLOT_SIZE;
                int slotY = gridStartY + r * SLOT_SIZE;
                int idx = r * COLS + c;

                ItemStack stack = displayStacks[idx];
                ReactorComponent comp = simGrid[idx];

                if (!stack.isEmpty()) {
                    context.drawItem(stack, slotX, slotY);

                    // Полоска нагрева поверх предмета
                    if (comp != null && comp.getMaxHeat() > 0 && comp.getCurrentHeat() > 0) {
                        float heatRatio = Math.min(1.0f, (float) comp.getCurrentHeat() / comp.getMaxHeat());
                        int barWidth = Math.round(heatRatio * 13);
                        int barY = slotY + 13;
                        int color = comp.isBroken() ? 0xFFFF0000 : (heatRatio > 0.7f ? 0xFFFF5555 : (heatRatio > 0.3f ? 0xFFFFAA00 : 0xFF55FF55));

                        context.fill(slotX + 2, barY, slotX + 15, barY + 2, 0xFF000000);
                        context.fill(slotX + 2, barY, slotX + 2 + barWidth, barY + 2, color);
                    }

                    // Индикатор расплавления / разрушения
                    if (comp != null && comp.isBroken()) {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FF0000);
                    }
                }

                // Подсветка при наведении курсора на слот реактора
                if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                    hoveredReactorSlot = idx;
                    if (!this.cursorStack.isEmpty()) {
                        if (this.cursorIsEraser) {
                            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FF3333);
                        } else {
                            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x6033FF33);
                            if (stack.isEmpty()) {
                                context.drawItem(this.cursorStack, slotX, slotY);
                            }
                        }
                    } else {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x50FFFFFF);
                    }
                }
            }
        }

        // 8. Отрисовка слотов инвентаря игрока в нижней части generic_54 (36 слотов)
        int invStartX = this.guiLeft + 8;
        int hoveredInvSlot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = playerInventoryStacks[i];
            int c = i % 9;
            int r = i / 9;
            int slotX = invStartX + c * SLOT_SIZE;
            int slotY = (r < 3) ? (this.guiTop + 140 + r * SLOT_SIZE) : (this.guiTop + 198);

            if (stack != null && !stack.isEmpty()) {
                context.drawItem(stack, slotX, slotY);
            }

            if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                hoveredInvSlot = i;
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x50FFFFFF);
            }
        }

        // 9. Отрисовка панельки палитры компонентов СЛЕВА от реактора
        renderPalettePanel(context, mouseX, mouseY);
        int hoveredPaletteSlot = getHoveredPaletteSlot(mouseX, mouseY);

        // 10. Отрисовка кнопок
        super.render(context, mouseX, mouseY, delta);

        // 11. Всплывающие подсказки (показываются, только если в руке нет призрака предмета)
        if (this.cursorStack.isEmpty()) {
            if (hoveredReactorSlot != -1) {
                ReactorComponent comp = simGrid[hoveredReactorSlot];
                ItemStack stack = displayStacks[hoveredReactorSlot];
                if (comp != null || !stack.isEmpty()) {
                    renderReactorSlotTooltip(context, comp, stack, mouseX, mouseY);
                }
            } else if (hoveredPaletteSlot != -1) {
                renderPaletteTooltip(context, palette[hoveredPaletteSlot], mouseX, mouseY);
            } else if (hoveredInvSlot != -1) {
                ItemStack stack = playerInventoryStacks[hoveredInvSlot];
                if (stack != null && !stack.isEmpty()) {
                    ReactorComponent comp = ReactorItemMapper.fromItemStack(stack);
                    renderReactorSlotTooltip(context, comp, stack, mouseX, mouseY);
                }
            }
        }

        // 12. Отрисовка удерживаемого "призрака предмета" на курсоре (поверх всего)
        if (!this.cursorStack.isEmpty()) {
            context.drawItem(this.cursorStack, mouseX - 8, mouseY - 8);
        }
    }

    private void renderPalettePanel(DrawContext context, int mouseX, int mouseY) {
        // Фон панели
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xD0181818);

        // Обводка панели
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF373737);
        context.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF373737);
        context.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFF373737);
        context.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF373737);

        // Заголовок панели
        context.drawText(this.textRenderer, Text.literal("§6Стержни"), panelX + 7, panelY + 6, 0xFFFFFFFF, false);

        // Сетка 3x6 слотов палитры
        for (int r = 0; r < PALETTE_ROWS; r++) {
            for (int c = 0; c < PALETTE_COLS; c++) {
                int idx = r * PALETTE_COLS + c;
                if (idx >= palette.length) continue;
                PaletteEntry entry = palette[idx];

                int sx = panelX + 7 + c * PALETTE_SLOT_SIZE;
                int sy = panelY + 18 + r * PALETTE_SLOT_SIZE;

                // Рамка слота
                context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                context.fill(sx, sy, sx + 16, sy + 16, 0xFF2A2A2A);

                if (entry != null && entry.stack != null && !entry.stack.isEmpty()) {
                    context.drawItem(entry.stack, sx, sy);
                }

                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    context.fill(sx, sy, sx + 16, sy + 16, 0x50FFFFFF);
                }
            }
        }
    }

    private int getHoveredPaletteSlot(int mouseX, int mouseY) {
        for (int r = 0; r < PALETTE_ROWS; r++) {
            for (int c = 0; c < PALETTE_COLS; c++) {
                int idx = r * PALETTE_COLS + c;
                if (idx >= palette.length) continue;

                int sx = panelX + 7 + c * PALETTE_SLOT_SIZE;
                int sy = panelY + 18 + r * PALETTE_SLOT_SIZE;

                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    return idx;
                }
            }
        }
        return -1;
    }

    private void renderReactorSlotTooltip(DrawContext context, ReactorComponent comp, ItemStack stack, int mouseX, int mouseY) {
        List<Text> lines = new ArrayList<>();
        String name = (stack != null && !stack.isEmpty()) ? stack.getName().getString() : (comp != null ? comp.getClass().getSimpleName() : "Компонент");
        lines.add(Text.literal(name));

        if (comp != null) {
            if (comp.getMaxHeat() > 0) {
                lines.add(Text.literal(String.format("§7Тепло: §f%d §7/ §f%d", comp.getCurrentHeat(), comp.getMaxHeat())));
            } else if (comp instanceof HeatVent vent && vent.getSideCooling() > 0) {
                lines.add(Text.literal("§7Емкость: §fнет (не накапливает тепло)"));
                lines.add(Text.literal("§7Охлаждение 4 соседей: §a10 HU/s"));
            }
            if (comp.isBroken()) {
                lines.add(Text.literal("§c(ВЗОРВАН / РАСПЛАВЛЕН)"));
            }
        }

        lines.add(Text.literal("§8[ЛКМ] Взять  [ПКМ] Очистить"));
        context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
    }

    private void renderPaletteTooltip(DrawContext context, PaletteEntry entry, int mouseX, int mouseY) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(entry.name));
        for (String desc : entry.description) {
            lines.add(Text.literal(desc));
        }
        lines.add(Text.literal(entry.isEraser ? "§8[ЛКМ] Взять ластик" : "§8[ЛКМ] Взять компонент"));
        context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
    }

    private boolean handleSlotInteraction(double mouseX, double mouseY, int button, boolean isDrag) {
        // 1. Взаимодействие с сеткой реактора (54 слота)
        int gridStartX = this.guiLeft + 8;
        int gridStartY = this.guiTop + 18;

        if (mouseX >= gridStartX && mouseX < gridStartX + COLS * SLOT_SIZE &&
                mouseY >= gridStartY && mouseY < gridStartY + ROWS * SLOT_SIZE) {

            int col = (int) (mouseX - gridStartX) / SLOT_SIZE;
            int row = (int) (mouseY - gridStartY) / SLOT_SIZE;
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int idx = row * COLS + col;

                if (isDrag && idx == lastInteractedSlot) {
                    return true;
                }
                lastInteractedSlot = idx;

                if (button == 0) { // ЛКМ
                    if (cursorIsEraser) {
                        setSlot(idx, null, ItemStack.EMPTY);
                        return true;
                    }
                    if (!cursorStack.isEmpty()) {
                        ReactorComponent comp = (cursorSupplier != null)
                                ? cursorSupplier.get()
                                : ReactorItemMapper.fromItemStack(cursorStack);
                        setSlot(idx, comp, cursorStack.copy());
                        return true;
                    }
                    // Если в руке пусто и это обычный клик — подбираем компонент из слота
                    if (!isDrag && grid[idx] != null && !displayStacks[idx].isEmpty()) {
                        ItemStack clicked = displayStacks[idx];
                        this.cursorStack = clicked.copy();
                        this.cursorSupplier = () -> ReactorItemMapper.fromItemStack(clicked);
                        this.cursorIsEraser = false;
                        return true;
                    }
                } else if (button == 1) { // ПКМ — очистка слота
                    setSlot(idx, null, ItemStack.EMPTY);
                    return true;
                } else if (button == 2 && !isDrag) { // СКМ — пипетка (Pick block)
                    if (grid[idx] != null && !displayStacks[idx].isEmpty()) {
                        ItemStack clicked = displayStacks[idx];
                        this.cursorStack = clicked.copy();
                        this.cursorSupplier = () -> ReactorItemMapper.fromItemStack(clicked);
                        this.cursorIsEraser = false;
                        return true;
                    }
                }
            }
            return true;
        }

        if (!isDrag) {
            lastInteractedSlot = -1;

            // 2. Взаимодействие с палитрой компонентов (панелька слева)
            int palIdx = getHoveredPaletteSlot((int) mouseX, (int) mouseY);
            if (palIdx >= 0 && palIdx < palette.length && palette[palIdx] != null) {
                PaletteEntry entry = palette[palIdx];
                this.cursorStack = entry.stack.copy();
                this.cursorSupplier = entry.supplier;
                this.cursorIsEraser = entry.isEraser;
                return true;
            }

            // 3. Взаимодействие со слотами инвентаря игрока внизу
            int invStartX = this.guiLeft + 8;
            if (mouseX >= invStartX && mouseX < invStartX + 9 * SLOT_SIZE) {
                int col = (int) (mouseX - invStartX) / SLOT_SIZE;
                if (col >= 0 && col < 9) {
                    int invIdx = -1;
                    for (int r = 0; r < 3; r++) {
                        int sy = this.guiTop + 140 + r * SLOT_SIZE;
                        if (mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                            invIdx = r * 9 + col;
                            break;
                        }
                    }
                    if (invIdx == -1) {
                        int sy = this.guiTop + 198;
                        if (mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                            invIdx = 27 + col;
                        }
                    }

                    if (invIdx >= 0 && invIdx < 36) {
                        // Если в руке удерживается призрак предмета — клик по инвентарю сбрасывает его (в инвентарь класть нельзя)
                        if (!this.cursorStack.isEmpty()) {
                            this.cursorStack = ItemStack.EMPTY;
                            this.cursorSupplier = null;
                            this.cursorIsEraser = false;
                            return true;
                        }

                        // Если в руке пусто — можно взять копию компонента из своего инвентаря (если это компонент реактора)
                        if (button == 0 || button == 2) {
                            if (!playerInventoryStacks[invIdx].isEmpty()) {
                                ItemStack clicked = playerInventoryStacks[invIdx];
                                ReactorComponent comp = ReactorItemMapper.fromItemStack(clicked);
                                if (comp != null) {
                                    this.cursorStack = clicked.copy();
                                    this.cursorSupplier = () -> ReactorItemMapper.fromItemStack(clicked);
                                    this.cursorIsEraser = false;
                                    return true;
                                }
                            }
                        }
                        return true;
                    }
                }
            }

            // 4. Клик по полосе статуса реактора (быстрое переключение пресетов тепла)
            if (mouseX >= this.guiLeft + 7 && mouseX <= this.guiLeft + 169
                    && mouseY >= this.guiTop + 125 && mouseY <= this.guiTop + 138) {
                cycleHeatPreset();
                return true;
            }

            // 5. ПКМ в пустом месте экрана — сбросить призрак предмета
            if (button == 1 && !this.cursorStack.isEmpty()) {
                this.cursorStack = ItemStack.EMPTY;
                this.cursorSupplier = null;
                this.cursorIsEraser = false;
                return true;
            }
        }

        return false;
    }

    //? if <=1.20.1 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        if ((mouseX >= this.guiLeft && mouseX <= this.guiLeft + BACKGROUND_WIDTH && mouseY >= this.guiTop + 124 && mouseY <= this.guiTop + 139)
                || (mouseX >= this.guiLeft + 180 && mouseX <= this.guiLeft + 260 && mouseY >= this.guiTop + 130 && mouseY <= this.guiTop + 175)) {
            if (amount > 0) {
                adjustHeat(500);
            } else if (amount < 0) {
                adjustHeat(-500);
            }
            return true;
        }
        return false;
    }
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        // Колесико мыши над статус-баром или над кнопками тепла регулирует тепло корпуса (+-500 = +-5%)
        if ((mouseX >= this.guiLeft && mouseX <= this.guiLeft + BACKGROUND_WIDTH && mouseY >= this.guiTop + 124 && mouseY <= this.guiTop + 139)
                || (mouseX >= this.guiLeft + 180 && mouseX <= this.guiLeft + 260 && mouseY >= this.guiTop + 130 && mouseY <= this.guiTop + 175)) {
            if (verticalAmount > 0) {
                adjustHeat(500);
            } else if (verticalAmount < 0) {
                adjustHeat(-500);
            }
            return true;
        }
        return false;
    }
    //?}

    //? if <=1.21.8 {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return handleSlotInteraction(mouseX, mouseY, button, false);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return handleSlotInteraction(mouseX, mouseY, button, true);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        lastInteractedSlot = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // Escape
            if (!this.cursorStack.isEmpty()) {
                this.cursorStack = ItemStack.EMPTY;
                this.cursorSupplier = null;
                this.cursorIsEraser = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        if (super.mouseClicked(click, bl)) {
            return true;
        }
        return handleSlotInteraction(click.x(), click.y(), click.button(), false);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        if (super.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        return handleSlotInteraction(click.x(), click.y(), click.button(), true);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        lastInteractedSlot = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        if (keyInput.key() == 256) { // Escape
            if (!this.cursorStack.isEmpty()) {
                this.cursorStack = ItemStack.EMPTY;
                this.cursorSupplier = null;
                this.cursorIsEraser = false;
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }
    //?}

    @Override
    public void close() {
        this.isAutoTicking = false;
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}