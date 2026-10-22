package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "reload()V", at = @At("HEAD"))
    private void onReload(CallbackInfo ci) {
        if (CONFIG != null && CONFIG.itemDisplayBaking != null && CONFIG.itemDisplayBaking.enabled) {
            ItemDisplayBakingManager.onChunkReload();
        }
    }
}
