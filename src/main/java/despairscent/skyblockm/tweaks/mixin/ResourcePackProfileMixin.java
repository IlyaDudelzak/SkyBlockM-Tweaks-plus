package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ResourcePackProfile.class)
public abstract class ResourcePackProfileMixin {

    @Shadow public abstract ResourcePackSource getSource();

    @Inject(method = "isPinned", at = @At("HEAD"), cancellable = true)
    public void unpinServerPacks(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG != null && CONFIG.serverPackUnlocker != null && CONFIG.serverPackUnlocker.enabled && this.getSource() == ResourcePackSource.SERVER) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isAlwaysEnabled", at = @At("HEAD"), cancellable = true)
    public void unrequireServerPacks(CallbackInfoReturnable<Boolean> cir) {
        if (CONFIG != null && CONFIG.serverPackUnlocker != null && CONFIG.serverPackUnlocker.enabled && this.getSource() == ResourcePackSource.SERVER) {
            cir.setReturnValue(false);
        }
    }
}
