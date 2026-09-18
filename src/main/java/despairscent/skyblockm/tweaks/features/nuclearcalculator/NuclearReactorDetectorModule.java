package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponent;
import despairscent.skyblockm.tweaks.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

public class NuclearReactorDetectorModule {

    private static final Pattern REACTOR_PATTERN = Pattern.compile(
            "Выход:\\s*[0-9.,]+\\s*Вт\\s*\\|\\s*Нагрев ядра:\\s*[0-9.,]+%",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    public static boolean isReactorScreen(Screen screen) {
        if (!(screen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }

        // 1. Быстрая проверка по чистому тексту заголовка
        String title = handledScreen.getTitle().getString();
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "").trim();

        if (REACTOR_PATTERN.matcher(cleanTitle).find()) {
            return true;
        }

        // 2. Запасная проверка: если реактор пустой/остановлен, но шрифт electricity:nuclear_reactor
        return handledScreen.getTitle().getStyle() != null
                && handledScreen.getTitle().getStyle().getFont() != null
                && handledScreen.getTitle().getStyle().getFont().toString().contains("nuclear_reactor");
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (isReactorScreen(screen) && screen instanceof HandledScreen<?> handledScreen) {
                HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                int guiX = accessor.getX();
                int guiY = accessor.getY();

                // Создаем кнопку вызова калькулятора
                ButtonWidget calcButton = ButtonWidget.builder(Text.literal("🖩"), button -> {
                            NuclearCalculatorScreen calcScreen = new NuclearCalculatorScreen(
                                    Text.literal("Калькулятор реактора"),
                                    handledScreen
                            );

                            var handler = handledScreen.getScreenHandler();
                            String screenTitle = handledScreen.getTitle().getString();
                            String cleanTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "").trim();

                            // 1. Копируем схему реактора (первые 54 слота)
                            int slotsToCopy = Math.min(54, handler.slots.size());
                            for (int i = 0; i < slotsToCopy; i++) {
                                ItemStack stack = handler.getSlot(i).getStack();
                                if (!stack.isEmpty()) {
                                    ReactorComponent comp = ReactorItemMapper.fromItemStack(stack);
                                    if (comp != null) {
                                        calcScreen.setSlot(i, comp, stack);
                                    }
                                }
                            }

                            // 2. Копируем инвентарь игрока (слоты 54..89 в двойном сундуке)
                            int totalSlots = handler.slots.size();
                            for (int i = 54; i < Math.min(90, totalSlots); i++) {
                                ItemStack stack = handler.getSlot(i).getStack();
                                if (!stack.isEmpty()) {
                                    calcScreen.setPlayerInventorySlot(i - 54, stack);
                                }
                            }

                            // 3. Парсим текущий нагрев из заголовка
                            var matcher = REACTOR_PATTERN.matcher(cleanTitle);
                            if (matcher.find()) {
                                try {
                                    String heatStr = matcher.group(2).replace(',', '.');
                                    double heatPercent = Double.parseDouble(heatStr);
                                    calcScreen.setInitialHeatPercent(heatPercent);
                                } catch (Exception ignored) {}
                            }

                            client.setScreen(calcScreen);
                        })
                        .dimensions(guiX + 178, guiY + 4, 20, 20)
                        .build();

                Screens.getButtons(screen).add(calcButton);
            }
        });
    }
}