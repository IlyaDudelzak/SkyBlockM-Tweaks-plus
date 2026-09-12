package despairscent.skyblockm.tweaks.mixin;

import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ResourcePackProfile.InsertionPosition.class)
public abstract class ResourcePackProfileInsertionPositionMixin {

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    public <T> void modifyInsertionPosition(
            List<T> items, T item, Function<T, ResourcePackProfile> profileGetter,
            boolean listInverted, CallbackInfoReturnable<Integer> cir
    ) {
        if (CONFIG != null && CONFIG.serverPackUnlocker != null && CONFIG.serverPackUnlocker.enabled) {
            if (item instanceof ResourcePackProfile profile && profile.getSource() == ResourcePackSource.SERVER) {
                int insertPos = 0;
                if (listInverted) {
                    insertPos = items.size();
                } else {
                    for (int i = 0; i < items.size(); i++) {
                        ResourcePackProfile p = profileGetter.apply(items.get(i));
                        if (p != null && p.isPinned() && p.getInitialPosition() == ResourcePackProfile.InsertionPosition.BOTTOM) {
                            insertPos = i + 1;
                        } else {
                            break;
                        }
                    }
                }
                items.add(insertPos, item);
                cir.setReturnValue(insertPos);
            }
        }
    }
}
