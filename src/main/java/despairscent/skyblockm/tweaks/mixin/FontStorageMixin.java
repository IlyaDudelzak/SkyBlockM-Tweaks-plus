package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphBaker;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(FontStorage.class)
public class FontStorageMixin {

    @Unique
    private static final BakedGlyph EMPTY_GLYPH = new BakedGlyph() {
        private final GlyphMetrics metrics = GlyphMetrics.empty(0.0f);

        @Override
        public GlyphMetrics getMetrics() {
            return metrics;
        }

        @Override
        public TextDrawable.DrawnGlyphRect create(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
            return null;
        }
    };

    @Unique
    private static final FontStorage.GlyphPair EMPTY_GLYPH_PAIR = new FontStorage.GlyphPair(() -> EMPTY_GLYPH, () -> EMPTY_GLYPH);

    @Shadow
    @Final
    GlyphBaker glyphBaker;

    @Inject(method = "getBaked", at = @At("HEAD"), cancellable = true)
    private void onGetBaked(int codePoint, CallbackInfoReturnable<FontStorage.GlyphPair> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.glyphBaker != null && this.glyphBaker.fontId != null && "electric_storage".equals(this.glyphBaker.fontId.getNamespace())) {
                String path = this.glyphBaker.fontId.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    cir.setReturnValue(EMPTY_GLYPH_PAIR);
                }
            }
        }
    }
}
