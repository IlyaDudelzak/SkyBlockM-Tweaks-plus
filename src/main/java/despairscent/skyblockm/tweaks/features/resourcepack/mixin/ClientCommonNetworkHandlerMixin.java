package despairscent.skyblockm.tweaks.features.resourcepack.mixin;

//? if >=1.20.2 {
import despairscent.skyblockm.tweaks.ModUtils;
import despairscent.skyblockm.tweaks.features.resourcepack.SkyBlockPackManager;
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
        if (CONFIG != null && CONFIG.serverPackUnlocker != null && CONFIG.serverPackUnlocker.enabled && this.serverInfo != null && this.serverInfo.getResourcePackPolicy() == ServerInfo.ResourcePackPolicy.DISABLED) {
            this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            ci.cancel();
            return;
        }
        
        String serverAddress = this.serverInfo != null ? this.serverInfo.address : null;
        if (SkyBlockPackManager.shouldBypassServerPack(packet, serverAddress)) {
            ModUtils.LOGGER.info("SkyBlockM Tweaks: Bypassing duplicate server pack download because SkyBlockM pack is already active.");
            this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            ci.cancel();
            return;
        }

        SkyBlockPackManager.onServerPackSend(packet, serverAddress);
    }
}
//?}
