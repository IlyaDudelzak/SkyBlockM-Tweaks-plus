package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.ModUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {

    @Shadow @Nullable protected ServerInfo serverInfo;

    @Shadow @Final protected ClientConnection connection;

    @Inject(method = "onResourcePackSend", at = @At("HEAD"), cancellable = true)
    public void onResourcePackSend(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        if (CONFIG.serverPackUnlocker.enabled && this.serverInfo != null && this.serverInfo.getResourcePackPolicy() == ServerInfo.ResourcePackPolicy.DISABLED) {
            this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            ci.cancel();
            return;
        }
        
        if (CONFIG.skyblockPackOptimization.enabled && this.serverInfo != null) {
            String address = this.serverInfo.address.toLowerCase();
            if (address.contains("justmc.ru") || address.contains("justmc.io")) {
                String expectedHash = packet.hash();
                if (expectedHash != null && !expectedHash.isEmpty() && expectedHash.equals(CONFIG.skyblockPackOptimization.lastHash)) {
                    // Check if file/SkyBlockM.zip is applied
                    boolean isApplied = MinecraftClient.getInstance().getResourcePackManager().getEnabledIds().contains("file/SkyBlockM.zip");
                    if (isApplied) {
                        ModUtils.LOGGER.info("SkyBlockM Tweaks: Skipping resource pack download because hash matches and SkyBlockM.zip is already applied.");
                        this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
