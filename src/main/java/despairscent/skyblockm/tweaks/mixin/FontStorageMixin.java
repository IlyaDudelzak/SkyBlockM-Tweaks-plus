package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.FontStorage;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(FontStorage.class)
public class FontStorageMixin {

    @Shadow
    @Final
    private Identifier id;

    @Shadow
    private BakedGlyph blankBakedGlyph;

    @Inject(method = "getBaked(I)Lnet/minecraft/client/font/BakedGlyph;", at = @At("HEAD"), cancellable = true)
    private void onGetBaked(int codePoint, CallbackInfoReturnable<BakedGlyph> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.id != null && "electric_storage".equals(this.id.getNamespace())) {
                String path = this.id.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    cir.setReturnValue(this.blankBakedGlyph);
                }
            }
        }
    }
}
