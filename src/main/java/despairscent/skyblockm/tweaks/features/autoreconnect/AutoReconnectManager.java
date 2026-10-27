package despairscent.skyblockm.tweaks.features.autoreconnect;
import despairscent.skyblockm.tweaks.ModUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
//? if >=1.20.2 {
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
//?} else {
/*import net.minecraft.client.gui.screen.ConnectScreen;
*///?}
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class AutoReconnectManager {

    private static ServerInfo lastConnectedServer = null;

    private static DisconnectedScreen activeDisconnectedScreen = null;
    private static Screen activeParentScreen = null;
    private static TextWidget countdownWidget = null;
    private static long targetReconnectTime = 0L;
    private static int lastDisplayedSeconds = -1;
    private static boolean isAutoReconnecting = false;

    private static boolean hubDetected = false;
    private static boolean manualServerSwitch = false;
    private static long scheduledServerJoinTime = 0L;
    private static boolean hasLoggedIn = false;
    private static int pendingLoginTicks = -1;

    private static float initialYaw = 0.0f;
    private static float initialPitch = 0.0f;
    private static boolean trackingRotation = false;

    public static void setLastConnectedServer(ServerInfo serverInfo) {
        if (serverInfo != null) {
            lastConnectedServer = serverInfo;
        }
    }

    public static ServerInfo getLastConnectedServer() {
        if (lastConnectedServer != null) {
            return lastConnectedServer;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getCurrentServerEntry() != null) {
            lastConnectedServer = client.getCurrentServerEntry();
        }
        return lastConnectedServer;
    }

    public static void onChatMessage(String text) {
        if (text == null) return;
        if (CONFIG == null || CONFIG.autoReconnect == null) return;

        // Auto Login
        if (CONFIG.autoReconnect.autoLogin && !hasLoggedIn && CONFIG.autoReconnect.password != null && !CONFIG.autoReconnect.password.trim().isEmpty()) {
            String lower = text.toLowerCase();
            // Server auth prompts don't contain player chat separator '⏵'
            if (!text.contains("⏵") && (lower.contains("/login") || lower.contains("/l ") || lower.contains("авториз") || lower.contains("введите пароль") || lower.contains("войдите в игру"))) {
                hasLoggedIn = true;
                pendingLoginTicks = 10; // 0.5s delay
            }
        }

        // Auto Join SkyBlockM ("по первому появлению символа '⏵' в чате с ником игрока, реагировать только на первое появление")
        if (CONFIG.autoReconnect.autoJoinSkyBlock) {
            if (text.contains("⏵")) {
                if (!hubDetected && !manualServerSwitch) {
                    hubDetected = true;
                    int delay = Math.max(0, CONFIG.autoReconnect.hubJoinDelaySeconds);
                    scheduledServerJoinTime = System.currentTimeMillis() + (delay * 1000L);
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null) {
                        initialYaw = client.player.getYaw();
                        initialPitch = client.player.getPitch();
                        trackingRotation = true;
                    } else {
                        trackingRotation = false;
                    }
                    if (client != null && client.inGameHud != null) {
                        if (delay == 0) {
                            client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §eОбнаружен вход в хаб. Вход на sb-1..."));
                        } else {
                            client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §eОбнаружен вход в хаб. Вход на sb-1 через " + delay + " сек."));
                        }
                    }
                }   
            }
        }
    }

    public static void onManualServerSwitch() {
        manualServerSwitch = true;
        if (scheduledServerJoinTime > 0) {
            scheduledServerJoinTime = 0L;
            trackingRotation = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §eПереключение сервера: авто-вход на sb-1 отменён."));
            }
        }
    }

    public static void onServerOrWorldChange() {
        if (scheduledServerJoinTime > 0) {
            scheduledServerJoinTime = 0L;
            trackingRotation = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.inGameHud != null) {
                client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §eПереключение сервера/мира: авто-вход на sb-1 отменён."));
            }
        }
    }

    public static void cancelAutoJoinByPlayerAction(MinecraftClient client) {
        scheduledServerJoinTime = 0L;
        trackingRotation = false;
        manualServerSwitch = true;
        if (client != null && client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §eДействие игрока: авто-вход на sb-1 отменён."));
        }
    }

    public static void resetSessionState() {
        hubDetected = false;
        manualServerSwitch = false;
        scheduledServerJoinTime = 0L;
        hasLoggedIn = false;
        pendingLoginTicks = -1;
        trackingRotation = false;
    }

    public static void onDisconnectedScreenInit(DisconnectedScreen screen, Screen parent, TextWidget widget) {
        resetSessionState();
        if (CONFIG == null || CONFIG.autoReconnect == null || !CONFIG.autoReconnect.enabled) {
            cancel();
            return;
        }

        ServerInfo server = getLastConnectedServer();
        if (server == null) {
            cancel();
            return;
        }

        int delaySec = Math.max(0, CONFIG.autoReconnect.delaySeconds);
        activeDisconnectedScreen = screen;
        activeParentScreen = parent;
        countdownWidget = widget;
        targetReconnectTime = System.currentTimeMillis() + (delaySec * 1000L);
        lastDisplayedSeconds = delaySec;
        isAutoReconnecting = true;

        if (countdownWidget != null) {
            countdownWidget.setMessage(getReconnectMessage(delaySec));
        }
    }

    public static void tick(MinecraftClient client) {
        if (isAutoReconnecting) {
            if (client == null || client.currentScreen != activeDisconnectedScreen) {
                cancel();
            } else {
                long remainingMs = targetReconnectTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    int seconds = (int) Math.max(1, Math.ceil(remainingMs / 1000.0));
                    if (seconds != lastDisplayedSeconds) {
                        lastDisplayedSeconds = seconds;
                        if (countdownWidget != null) {
                            countdownWidget.setMessage(getReconnectMessage(seconds));
                        }
                    }
                } else {
                    Screen parent = activeParentScreen;
                    cancel();
                    reconnect(parent, client);
                }
            }
        }

        // In-game automation (login and server join)
        if (client != null && client.player != null && client.getNetworkHandler() != null && CONFIG != null && CONFIG.autoReconnect != null) {
            if (pendingLoginTicks > 0) {
                pendingLoginTicks--;
                if (pendingLoginTicks == 0) {
                    String pwd = CONFIG.autoReconnect.password;
                    if (pwd != null) {
                        pwd = pwd.trim();
                        if (pwd.toLowerCase().startsWith("/login ")) {
                            pwd = pwd.substring(7).trim();
                        } else if (pwd.toLowerCase().startsWith("/l ")) {
                            pwd = pwd.substring(3).trim();
                        }
                        if (!pwd.isEmpty()) {
                            try {
                                client.getNetworkHandler().sendChatCommand("login " + pwd);
                            } catch (Exception e) {
                                ModUtils.LOGGER.error("Failed to send /login command", e);
                            }
                        }
                    }
                }
            }

            if (scheduledServerJoinTime > 0) {
                if (!trackingRotation) {
                    initialYaw = client.player.getYaw();
                    initialPitch = client.player.getPitch();
                    trackingRotation = true;
                }

                float deltaYaw = Math.abs(MathHelper.wrapDegrees(client.player.getYaw() - initialYaw));
                float deltaPitch = Math.abs(MathHelper.wrapDegrees(client.player.getPitch() - initialPitch));
                boolean turned = deltaYaw > 1.0f || deltaPitch > 1.0f;

                boolean moved = client.options.forwardKey.isPressed()
                        || client.options.backKey.isPressed()
                        || client.options.leftKey.isPressed()
                        || client.options.rightKey.isPressed()
                        || client.options.jumpKey.isPressed()
                        || client.options.sneakKey.isPressed()
                        || client.options.sprintKey.isPressed();

                boolean interacted = client.options.attackKey.isPressed() || client.options.useKey.isPressed();

                if (turned || moved || interacted) {
                    cancelAutoJoinByPlayerAction(client);
                } else if (System.currentTimeMillis() >= scheduledServerJoinTime) {
                    scheduledServerJoinTime = 0L;
                    trackingRotation = false;
                    if (CONFIG.autoReconnect.autoJoinSkyBlock) {
                        if (client.inGameHud != null) {
                            client.inGameHud.getChatHud().addMessage(Text.literal("§7[§6SkyBlockM§7] §aПереключение на sb-1 (/server sb-1)..."));
                        }
                        try {
                            client.getNetworkHandler().sendChatCommand("server sb-1");
                        } catch (Exception e) {
                            ModUtils.LOGGER.error("Failed to send /server sb-1 command", e);
                        }
                    }
                }
            }
        }
    }

    public static void cancel() {
        isAutoReconnecting = false;
        activeDisconnectedScreen = null;
        activeParentScreen = null;
        countdownWidget = null;
        targetReconnectTime = 0L;
        lastDisplayedSeconds = -1;
    }

    public static void reconnect(Screen parent, MinecraftClient client) {
        cancel();
        ServerInfo server = getLastConnectedServer();
        if (server != null && client != null) {
            //? if >=1.20.5 {
            ConnectScreen.connect(parent, client, ServerAddress.parse(server.address), server, false, null);
            //?} else {
            /*ConnectScreen.connect(parent, client, ServerAddress.parse(server.address), server, false);
            *///?}
        }
    }

    public static Text getReconnectMessage(int seconds) {
        if (seconds <= 0) {
            return Text.literal("Автоматическое переподключение...");
        }
        return Text.literal("Автоматическое переподключение через " + seconds);
    }
}
