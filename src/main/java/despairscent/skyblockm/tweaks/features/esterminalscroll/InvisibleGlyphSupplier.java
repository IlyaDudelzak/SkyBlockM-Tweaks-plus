package despairscent.skyblockm.tweaks.features.esterminalscroll;

//? if >=1.21.11 {
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.GlyphMetrics;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.text.Style;

import java.util.function.Supplier;

public class InvisibleGlyphSupplier implements Supplier<BakedGlyph> {
    private final Supplier<BakedGlyph> originalSupplier;

    public InvisibleGlyphSupplier(Supplier<BakedGlyph> originalSupplier) {
        this.originalSupplier = originalSupplier;
    }

    @Override
    public BakedGlyph get() {
        if (originalSupplier == null) return null;
        BakedGlyph original = originalSupplier.get();
        if (original == null) return null;
        return new InvisibleBakedGlyph(original);
    }

    public static class InvisibleBakedGlyph implements BakedGlyph {
        private final BakedGlyph original;

        public InvisibleBakedGlyph(BakedGlyph original) {
            this.original = original;
        }

        @Override
        public GlyphMetrics getMetrics() {
            return original.getMetrics();
        }

        @Override
        public TextDrawable.DrawnGlyphRect create(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
            return null;
        }
    }
}
//?}
