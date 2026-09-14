package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphBaker;
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
    GlyphBaker glyphBaker;

    @Shadow
    @Final
    private FontStorage.GlyphPair blankBakedGlyphPair;

    @Inject(method = "getBaked", at = @At("HEAD"), cancellable = true)
    private void onGetBaked(int codePoint, CallbackInfoReturnable<FontStorage.GlyphPair> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.glyphBaker != null && this.glyphBaker.fontId != null && "electric_storage".equals(this.glyphBaker.fontId.getNamespace())) {
                String path = this.glyphBaker.fontId.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    cir.setReturnValue(this.blankBakedGlyphPair);
                }
            }
        }
    }
}
