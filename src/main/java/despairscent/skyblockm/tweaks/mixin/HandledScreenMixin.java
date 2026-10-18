package despairscent.skyblockm.tweaks.mixin;
import despairscent.skyblockm.tweaks.ModUtils;

import despairscent.skyblockm.tweaks.features.nuclearcalculator.NuclearReactorDetectorModule;
import despairscent.skyblockm.tweaks.features.renderiteminside.StoredCountUtils;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int playerInventoryTitleX;
    @Shadow protected int playerInventoryTitleY;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Text modifyTitle(Text title) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            return StoredCountUtils.cleanTitle(title);
        }
        return title;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hidePlayerInventoryTitleInReactor(CallbackInfo ci) {
        if (CONFIG != null && CONFIG.nuclearCalculator != null && CONFIG.nuclearCalculator.enabled) {
            if (NuclearReactorDetectorModule.isReactorScreen((HandledScreen<?>) (Object) this)) {
                this.playerInventoryTitleX = -9999;
                this.playerInventoryTitleY = -9999;
            }
        }
    }
}
