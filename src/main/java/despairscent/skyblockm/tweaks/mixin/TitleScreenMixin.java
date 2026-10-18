package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.features.updater.UpdateChecker;
import despairscent.skyblockm.tweaks.features.updater.UpdatePromptScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!UpdateChecker.isPromptShown() && UpdateChecker.hasAvailableUpdate()) {
            UpdateChecker.setPromptShown(true);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new UpdatePromptScreen((TitleScreen) (Object) this, UpdateChecker.getAvailableUpdate()));
            }
        }
    }
}
