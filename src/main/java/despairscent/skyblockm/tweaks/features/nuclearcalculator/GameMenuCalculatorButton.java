package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GameMenuCalculatorButton {

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof GameMenuScreen) {
                // Добавляем аккуратную кнопку в левый верхний угол меню паузы
                ButtonWidget button = ButtonWidget.builder(Text.literal("⚛ Калькулятор"), btn -> {
                            client.setScreen(new NuclearCalculatorScreen(
                                    Text.literal("Калькулятор реактора"),
                                    screen // При нажатии "Назад" вернет обратно в меню паузы
                            ));
                        })
                        .dimensions(10, 10, 105, 20)
                        .build();

                Screens.getButtons(screen).add(button);
            }
        });
    }
}