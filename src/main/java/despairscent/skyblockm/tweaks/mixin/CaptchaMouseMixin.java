package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.features.captcha.CaptchaDetector;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class CaptchaMouseMixin {

    //? if >=1.20.2 {
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouseInject(double timeDelta, CallbackInfo ci) {
        if (CaptchaDetector.shouldBlockPlayerInput()) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouseInject(CallbackInfo ci) {
        if (CaptchaDetector.shouldBlockPlayerInput()) {
            ci.cancel();
        }
    }
    *///?}
}
