package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.SplashManager;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextResourceSupplierMixin {

    @Shadow
    @Final
    private List<String> splashTexts;

    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("TAIL"))
    private void onApply(List<String> list, ResourceManager manager, Profiler profiler, CallbackInfo ci) {
        SplashManager.loadSplashes(manager);
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onGet(CallbackInfoReturnable<SplashTextRenderer> cir) {
        if (CONFIG == null || CONFIG.splashes == null || !CONFIG.splashes.enabled) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        if ((calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DAY_OF_MONTH) == 24) ||
            (calendar.get(Calendar.MONTH) + 1 == 1 && calendar.get(Calendar.DAY_OF_MONTH) == 1) ||
            (calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DAY_OF_MONTH) == 31)) {
            return;
        }

        if (SplashManager.hasSplashes()) {
            boolean pickCustom = CONFIG.splashes.onlyCustomSplashes || (Math.random() < 0.75);
            if (pickCustom || (this.splashTexts != null && this.splashTexts.isEmpty())) {
                String splash = SplashManager.getRandomSplash();
                if (splash != null && !splash.isEmpty()) {
                    //? if >=1.21.11 {
                    /*cir.setReturnValue(new SplashTextRenderer(net.minecraft.text.Text.literal(splash)));
                    *///?} else {
                    cir.setReturnValue(new SplashTextRenderer(splash));
                    //?}
                }
            }
        }
    }
}
