package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.mixininner.IAnvilScreenMixin;
import despairscent.skyblockm.tweaks.mixininner.IMinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CLIENT;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraftClientAccessor {

    @Shadow
    private ItemColors itemColors;

    @Override
    public ItemColors skyblockm$getItemColors() {
        return this.itemColors;
    }

    @Inject(method = "setScreen",
            at = @At("HEAD"))
    private void setScreenInject(Screen screen, CallbackInfo ci) {
        Screen previousScreen = CLIENT.currentScreen;
        if (previousScreen instanceof AnvilScreen previous && screen instanceof AnvilScreen) {
            ((IAnvilScreenMixin) screen).skyblockm_tweaks$handlePrevious(previous);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        despairscent.skyblockm.tweaks.ItemDisplayBakingManager.tickRebuilds();
    }

}
