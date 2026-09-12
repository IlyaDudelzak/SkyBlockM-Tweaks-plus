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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

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
        
        if (CONFIG != null && CONFIG.skyblockPackOptimization != null && CONFIG.skyblockPackOptimization.enabled && this.serverInfo != null) {
            String address = this.serverInfo.address.toLowerCase();
            if (address.contains("justmc.ru") || address.contains("justmc.io")) {
                String expectedHash = packet.hash();
                MinecraftClient client = MinecraftClient.getInstance();
                Path packPath = client.getResourcePackDir().resolve("SkyBlockM.zip");
                boolean isApplied = client.getResourcePackManager().getEnabledIds().contains("file/SkyBlockM.zip");

                if (expectedHash != null && !expectedHash.isEmpty() && expectedHash.equalsIgnoreCase(CONFIG.skyblockPackOptimization.lastHash) && Files.exists(packPath) && isApplied) {
                    ModUtils.LOGGER.info("SkyBlockM Tweaks: Skipping resource pack download because SkyBlockM.zip is already applied and hash matches.");
                    this.connection.send(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                    ci.cancel();
                    return;
                }

                // If hash changed or pack was not loaded yet, watch for vanilla's download and copy to SkyBlockM.zip
                if (expectedHash != null && !expectedHash.isEmpty()) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            Path downloaded = client.runDirectory.toPath().resolve("downloads").resolve(packet.id().toString()).resolve(expectedHash);
                            // Wait up to 60 seconds for vanilla download to complete
                            for (int i = 0; i < 120; i++) {
                                Thread.sleep(500);
                                if (Files.exists(downloaded) && Files.size(downloaded) > 1000) {
                                    // Give file a moment to finish writing
                                    Thread.sleep(1000);
                                    Files.copy(downloaded, packPath, StandardCopyOption.REPLACE_EXISTING);
                                    CONFIG.skyblockPackOptimization.lastHash = expectedHash;
                                    CONFIG.save();
                                    ModUtils.LOGGER.info("SkyBlockM Tweaks: Successfully updated SkyBlockM.zip with newly downloaded pack.");
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            ModUtils.LOGGER.error("SkyBlockM Tweaks: Failed to cache downloaded resource pack", e);
                        }
                    });
                }
            }
        }
    }
}
