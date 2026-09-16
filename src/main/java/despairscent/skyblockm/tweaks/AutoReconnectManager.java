package despairscent.skyblockm.tweaks;

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

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class AutoReconnectManager {

    private static ServerInfo lastConnectedServer = null;

    private static DisconnectedScreen activeDisconnectedScreen = null;
    private static Screen activeParentScreen = null;
    private static TextWidget countdownWidget = null;
    private static long targetReconnectTime = 0L;
    private static int lastDisplayedSeconds = -1;
    private static boolean isAutoReconnecting = false;

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

    public static void onDisconnectedScreenInit(DisconnectedScreen screen, Screen parent, TextWidget widget) {
        if (CONFIG == null || CONFIG.autoReconnect == null || !CONFIG.autoReconnect.enabled) {
            cancel();
            return;
        }

        ServerInfo server = getLastConnectedServer();
        if (server == null) {
            cancel();
            return;
        }

        int delaySec = Math.max(1, CONFIG.autoReconnect.delaySeconds);
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
        if (!isAutoReconnecting) {
            return;
        }

        if (client == null || client.currentScreen != activeDisconnectedScreen) {
            cancel();
            return;
        }

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
        return Text.literal("Автоматическое переподключение через " + seconds);
    }
}
