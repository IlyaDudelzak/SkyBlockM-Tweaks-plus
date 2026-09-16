package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.AutoReconnectManager;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Shadow @Final private Screen parent;
    @Shadow @Final private Text buttonLabel;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (CONFIG == null || CONFIG.autoReconnect == null || !CONFIG.autoReconnect.enabled) {
            return;
        }

        ServerInfo server = AutoReconnectManager.getLastConnectedServer();
        if (server == null) {
            return;
        }

        ButtonWidget vanillaButton = null;
        for (var element : this.children()) {
            if (element instanceof ButtonWidget button) {
                vanillaButton = button;
                break;
            }
        }

        if (vanillaButton != null) {
            int btnX = vanillaButton.getX();
            int btnY = vanillaButton.getY();
            int btnWidth = vanillaButton.getWidth();
            int btnHeight = vanillaButton.getHeight();

            this.remove(vanillaButton);

            int delaySec = Math.max(1, CONFIG.autoReconnect.delaySeconds);

            // Текст обратного отсчета над кнопками на всю ширину экрана (центрированный)
            //? if >=1.21.11 {
            /*TextWidget countdownWidget = new TextWidget(
                    0,
                    btnY - 16,
                    this.width,
                    12,
                    AutoReconnectManager.getReconnectMessage(delaySec),
                    this.textRenderer
            );
            *///?} else {
            TextWidget countdownWidget = new TextWidget(
                    0,
                    btnY - 16,
                    this.width,
                    12,
                    AutoReconnectManager.getReconnectMessage(delaySec),
                    this.textRenderer
            ).alignCenter();
            //?}

            // Кнопка немедленного переподключения
            ButtonWidget reconnectButton = ButtonWidget.builder(Text.literal("Переподключение"), button -> {
                AutoReconnectManager.reconnect(this.parent, this.client);
            }).dimensions(btnX, btnY, btnWidth, btnHeight).build();

            // Ванильная кнопка возврата в меню серверов
            ButtonWidget backButton = ButtonWidget.builder(this.buttonLabel, button -> {
                AutoReconnectManager.cancel();
                if (this.client != null) {
                    this.client.setScreen(this.parent);
                }
            }).dimensions(btnX, btnY + btnHeight + 4, btnWidth, btnHeight).build();

            this.addDrawableChild(countdownWidget);
            this.addDrawableChild(reconnectButton);
            this.addDrawableChild(backButton);

            AutoReconnectManager.onDisconnectedScreenInit((DisconnectedScreen) (Object) this, this.parent, countdownWidget);
        }
    }
}
