package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.AdBlockerState;
import despairscent.skyblockm.tweaks.config.Config;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (CONFIG != null && CONFIG.adBlocker != null && CONFIG.adBlocker.enabled) {
            if (AdBlockerState.processMessage(message.getString())) {
                ci.cancel();
            }
        }
    }
}
