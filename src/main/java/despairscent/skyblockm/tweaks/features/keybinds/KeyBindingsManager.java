package despairscent.skyblockm.tweaks.features.keybinds;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.NuclearCalculatorScreen;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.NuclearReactorDetectorModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeyBindingsManager {

    //? if >=1.21.11 {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(net.minecraft.util.Identifier.of("skyblockm-tweaks", "keybinds"));
    //?} else {
    /*private static final String CATEGORY = "category.skyblockm-tweaks";
    *///?}

    public static KeyBinding homeKey;
    public static KeyBinding loadCell1Key;
    public static KeyBinding loadCell2Key;
    public static KeyBinding loadCell3Key;

    private static int pendingHomeIslandTpTicks = -1;

    public static void init() {
        homeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockm-tweaks.home",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_HOME,
                CATEGORY
        ));

        loadCell1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockm-tweaks.load_reactor_cell_1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                CATEGORY
        ));

        loadCell2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockm-tweaks.load_reactor_cell_2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                CATEGORY
        ));

        loadCell3Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockm-tweaks.load_reactor_cell_3",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindingsManager::onClientTick);

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen<?> || screen instanceof NuclearCalculatorScreen) {
                //? if >=1.21.11 {
                ScreenKeyboardEvents.afterKeyPress(screen).register((activeScreen, keyInput) -> {
                    if (activeScreen.getFocused() instanceof TextFieldWidget) return;
                    if (matches(loadCell1Key, keyInput.key(), keyInput.scancode())) {
                        handleReactorLoad(client, 0);
                    } else if (matches(loadCell2Key, keyInput.key(), keyInput.scancode())) {
                        handleReactorLoad(client, 1);
                    } else if (matches(loadCell3Key, keyInput.key(), keyInput.scancode())) {
                        handleReactorLoad(client, 2);
                    } else if (homeKey != null && matches(homeKey, keyInput.key(), keyInput.scancode())) {
                        if (client.player != null && isPlayerFalling(client.player)) {
                            client.setScreen(null);
                            handleHomeAction(client);
                        }
                    }
                });
                //?} else {
                /*ScreenKeyboardEvents.afterKeyPress(screen).register((activeScreen, key, scancode, modifiers) -> {
                    if (activeScreen.getFocused() instanceof TextFieldWidget) return;
                    if (matches(loadCell1Key, key, scancode)) {
                        handleReactorLoad(client, 0);
                    } else if (matches(loadCell2Key, key, scancode)) {
                        handleReactorLoad(client, 1);
                    } else if (matches(loadCell3Key, key, scancode)) {
                        handleReactorLoad(client, 2);
                    } else if (homeKey != null && matches(homeKey, key, scancode)) {
                        if (client.player != null && isPlayerFalling(client.player)) {
                            client.setScreen(null);
                            handleHomeAction(client);
                        }
                    }
                });
                *///?}
            }
        });
    }

    public static void onClientTick(MinecraftClient client) {
        // Обработка отложенной команды /i tp после /spawn
        if (pendingHomeIslandTpTicks > 0) {
            pendingHomeIslandTpTicks--;
            if (pendingHomeIslandTpTicks == 0) {
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.player.sendMessage(Text.literal("§7[§6SkyBlockM§7] §aТелепортация на остров (/i tp)"), true);
                    try {
                        client.getNetworkHandler().sendChatCommand("i tp");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Обработка клавиш в игре (когда экран не открыт)
        if (client.currentScreen == null) {
            while (homeKey.wasPressed()) {
                handleHomeAction(client);
            }

            while (loadCell1Key.wasPressed()) {
                handleReactorLoad(client, 0);
            }

            while (loadCell2Key.wasPressed()) {
                handleReactorLoad(client, 1);
            }

            while (loadCell3Key.wasPressed()) {
                handleReactorLoad(client, 2);
            }
        }
    }

    public static boolean isPlayerFalling(ClientPlayerEntity player) {
        if (player == null) return false;
        if (player.getY() < 0) return true;
        if (player.isOnGround()) return false;
        //? if >=1.21.3 {
        if (player.getAbilities().flying || player.isGliding()) return false;
        //?} else {
        /*if (player.getAbilities().flying || player.isFallFlying()) return false;
        *///?}
        if (player.isTouchingWater() || player.isInLava() || player.isClimbing()) return false;

        return player.getVelocity().y < -0.1 || player.fallDistance > 0.5f;
    }

    public static void handleHomeAction(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return;

        if (isPlayerFalling(client.player)) {
            client.player.sendMessage(Text.literal("§7[§6SkyBlockM§7] §eПадение! Спасение на /spawn..."), true);
            try {
                client.getNetworkHandler().sendChatCommand("spawn");
            } catch (Exception e) {
                e.printStackTrace();
            }
            pendingHomeIslandTpTicks = 20; // 1 секунда (20 тиков) до /i tp
        } else {
            client.player.sendMessage(Text.literal("§7[§6SkyBlockM§7] §aТелепортация на остров (/i tp)"), true);
            try {
                client.getNetworkHandler().sendChatCommand("i tp");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void handleReactorLoad(MinecraftClient client, int slotIndex) {
        if (client.player == null) return;

        Screen screen = client.currentScreen;
        if (screen instanceof HandledScreen<?> handledScreen && NuclearReactorDetectorModule.isReactorScreen(handledScreen)) {
            NuclearReactorDetectorModule.loadPresetIntoReactor(client, handledScreen, slotIndex);
        } else if (screen instanceof NuclearCalculatorScreen calcScreen) {
            calcScreen.loadPresetAndInsert(slotIndex);
        } else {
            client.player.sendMessage(Text.literal("§cОткройте интерфейс реактора для загрузки ячейки " + (slotIndex + 1)), true);
        }
    }

    public static boolean matches(KeyBinding keyBinding, int key, int scancode) {
        if (keyBinding == null) return false;
        //? if >=1.21.11 {
        return keyBinding.matchesKey(new net.minecraft.client.input.KeyInput(key, scancode, 0));
        //?} else {
        /*return keyBinding.matchesKey(key, scancode);
        *///?}
    }
}
