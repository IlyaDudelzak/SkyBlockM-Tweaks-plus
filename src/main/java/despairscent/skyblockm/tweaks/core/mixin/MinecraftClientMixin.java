package despairscent.skyblockm.tweaks.core.mixin;

import despairscent.skyblockm.tweaks.features.autoreconnect.AutoReconnectManager;
import despairscent.skyblockm.tweaks.features.captcha.CaptchaDetector;
import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;

import despairscent.skyblockm.tweaks.ModUtils;
import despairscent.skyblockm.tweaks.features.textinputlagfix.mixininner.IAnvilScreenMixin;
import despairscent.skyblockm.tweaks.core.mixininner.IMinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
//? if <1.21.8 {
import net.minecraft.client.color.item.ItemColors;
//?}
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraftClientAccessor {

    @Shadow
    @Nullable
    public Screen currentScreen;

    //? if <1.21.8 {
    @Shadow
    private ItemColors itemColors;

    @Override
    public ItemColors skyblockm$getItemColors() {
        return this.itemColors;
    }
    //?}

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(RunArgs args, CallbackInfo ci) {
        ModUtils.CLIENT = (MinecraftClient) (Object) this;
    }

    @Inject(method = "setScreen",
            at = @At("HEAD"))
    private void setScreenInject(Screen screen, CallbackInfo ci) {
        Screen previousScreen = this.currentScreen;
        if (previousScreen instanceof AnvilScreen previous && screen instanceof AnvilScreen) {
            ((IAnvilScreenMixin) screen).skyblockm_tweaks$handlePrevious(previous);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager.tickRebuilds();
        despairscent.skyblockm.tweaks.features.autoreconnect.AutoReconnectManager.tick((MinecraftClient) (Object) this);
        despairscent.skyblockm.tweaks.features.captcha.CaptchaDetector.tick((MinecraftClient) (Object) this);
    }

}
