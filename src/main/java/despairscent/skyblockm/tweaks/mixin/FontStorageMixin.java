package despairscent.skyblockm.tweaks.mixin;

//? if >=1.21.11 {
/*import net.minecraft.client.font.BakedGlyph;
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

import java.util.function.Supplier;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(FontStorage.class)
public class FontStorageMixin {

    @Shadow
    @Final
    GlyphBaker glyphBaker;

    @Unique
    private static Supplier<BakedGlyph> wrapInvisible(Supplier<BakedGlyph> originalSupplier) {
        if (originalSupplier == null) return null;
        return () -> {
            BakedGlyph original = originalSupplier.get();
            if (original == null) return null;
            return new BakedGlyph() {
                @Override
                public GlyphMetrics getMetrics() {
                    return original.getMetrics();
                }

                @Override
                public TextDrawable.DrawnGlyphRect create(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
                    return null;
                }
            };
        };
    }

    @Inject(method = "getBaked", at = @At("RETURN"), cancellable = true)
    private void onGetBaked(int codePoint, CallbackInfoReturnable<FontStorage.GlyphPair> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.glyphBaker != null && this.glyphBaker.fontId != null && "electric_storage".equals(this.glyphBaker.fontId.getNamespace())) {
                String path = this.glyphBaker.fontId.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    FontStorage.GlyphPair original = cir.getReturnValue();
                    if (original != null) {
                        Supplier<BakedGlyph> invisibleAny = wrapInvisible(original.any());
                        Supplier<BakedGlyph> invisibleAdv = wrapInvisible(original.advanceValidating());
                        cir.setReturnValue(new FontStorage.GlyphPair(invisibleAny, invisibleAdv));
                    }
                }
            }
        }
    }
}
*///?} elif >=1.21.3 {
/*import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.EmptyBakedGlyph;
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

    @Inject(method = "getBaked(I)Lnet/minecraft/client/font/BakedGlyph;", at = @At("HEAD"), cancellable = true)
    private void onGetBaked(int codePoint, CallbackInfoReturnable<BakedGlyph> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.id != null && "electric_storage".equals(this.id.getNamespace())) {
                String path = this.id.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    cir.setReturnValue(EmptyBakedGlyph.INSTANCE);
                }
            }
        }
    }
}
*///?} else {
import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.GlyphRenderer;
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

    @Inject(method = "getGlyphRenderer(I)Lnet/minecraft/client/font/GlyphRenderer;", at = @At("HEAD"), cancellable = true)
    private void onGetGlyphRenderer(int codePoint, CallbackInfoReturnable<GlyphRenderer> cir) {
        if (CONFIG != null && CONFIG.terminalStackCount != null && CONFIG.terminalStackCount.enabled && CONFIG.terminalStackCount.cleanTitle) {
            if (this.id != null && "electric_storage".equals(this.id.getNamespace())) {
                String path = this.id.getPath();
                if (path.contains("ascii_row") || path.contains("background")) {
                    cir.setReturnValue(EmptyGlyphRenderer.INSTANCE);
                }
            }
        }
    }
}
//?}
