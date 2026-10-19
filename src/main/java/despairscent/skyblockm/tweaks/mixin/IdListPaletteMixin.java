package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.world.chunk.IdListPalette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IdListPalette.class)
public class IdListPaletteMixin<T> {
    @Shadow
    @Final
    private IndexedIterable<T> idList;

    @Unique
    private static final Logger skyblockm$LOGGER = LoggerFactory.getLogger("skyblockm-tweaks");

    @Unique
    private static final java.util.Set<Integer> skyblockm$LOGGED_IDS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void skyblockm$onGet(int id, CallbackInfoReturnable<T> cir) {
        T object = this.idList.get(id);
        if (object == null) {
            T fallback = this.idList.get(0);
            if (skyblockm$LOGGED_IDS.add(id)) {
                skyblockm$LOGGER.warn("SkyBlockM Tweaks: Missing palette entry for ID {}, falling back to default entry to prevent chunk load drop", id);
            }
            cir.setReturnValue(fallback);
        }
    }
}
