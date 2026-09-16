package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.CaptchaDetector;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {

    //? if >=1.21.8 {
    /*@Inject(method = "tick", at = @At("TAIL"))
    private void onTickInject(CallbackInfo ci) {
        if (CaptchaDetector.shouldBlockPlayerInput()) {
            this.playerInput = net.minecraft.util.PlayerInput.DEFAULT;
        }
    }
    *///?} elif =1.21.3 {
    /*@Inject(method = "tick", at = @At("TAIL"))
    private void onTickInject(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (CaptchaDetector.shouldBlockPlayerInput()) {
            this.movementForward = 0.0f;
            this.movementSideways = 0.0f;
            this.playerInput = net.minecraft.util.PlayerInput.DEFAULT;
        }
    }
    *///?} else {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickInject(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (CaptchaDetector.shouldBlockPlayerInput()) {
            this.movementForward = 0.0f;
            this.movementSideways = 0.0f;
            this.jumping = false;
            this.sneaking = false;
        }
    }
    //?}
}
