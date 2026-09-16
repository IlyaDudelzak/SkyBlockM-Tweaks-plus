package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.modules.inventorydesyncfix.InventoryDesyncFixModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.20.2 {
/*import despairscent.skyblockm.tweaks.ModUtils;
import despairscent.skyblockm.tweaks.SkyBlockPackManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.ResourcePackSendS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
*///?}

import static despairscent.skyblockm.tweaks.ModUtils.CLIENT;
import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    //? if <1.20.2 {
    /*@Shadow @Nullable private ServerInfo serverInfo;

    @Shadow @Final private ClientConnection connection;

    @Inject(method = "onResourcePackSend", at = @At("HEAD"), cancellable = true)
    public void onResourcePackSend(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        if (CONFIG != null && CONFIG.serverPackUnlocker != null && CONFIG.serverPackUnlocker.enabled && this.serverInfo != null && this.serverInfo.getResourcePackPolicy() == ServerInfo.ResourcePackPolicy.DISABLED) {
            this.connection.send(new ResourcePackStatusC2SPacket(ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            ci.cancel();
            return;
        }

        String serverAddress = this.serverInfo != null ? this.serverInfo.address : null;
        if (SkyBlockPackManager.shouldBypassServerPack(packet, serverAddress)) {
            ModUtils.LOGGER.info("SkyBlockM Tweaks: Bypassing duplicate server pack download because SkyBlockM pack is already active.");
            this.connection.send(new ResourcePackStatusC2SPacket(ResourcePackStatusC2SPacket.Status.ACCEPTED));
            this.connection.send(new ResourcePackStatusC2SPacket(ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            ci.cancel();
            return;
        }

        SkyBlockPackManager.onServerPackSend(packet, serverAddress);
    }
    *///?}

    @Inject(method = "onUpdateSelectedSlot",
            at = @At("TAIL"))
    private void handlePacketInject(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (CLIENT.isOnThread()) {
            InventoryDesyncFixModule.handleSelectedSlotUpdate();
        }
    }

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    private void onPlayerPositionLookInject(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.antiRubberband && CLIENT.player != null) {
            //? if >=1.21.3 {
            /*net.minecraft.util.math.Vec3d targetPos = packet.change().position();
            double dx = CLIENT.player.getX() - targetPos.x;
            double dy = CLIENT.player.getY() - targetPos.y;
            double dz = CLIENT.player.getZ() - targetPos.z;
            *///?} else {
            double dx = CLIENT.player.getX() - packet.getX();
            double dy = CLIENT.player.getY() - packet.getY();
            double dz = CLIENT.player.getZ() - packet.getZ();
            //?}
            double distSq = dx * dx + dy * dy + dz * dz;

            double maxDist = CONFIG.itemDisplayHitbox.antiRubberbandDistance;
            
            // Ignore small rubberbands
            if (distSq > 0 && distSq < maxDist * maxDist) {
                // We MUST confirm the teleport to the server, otherwise it will ignore our future movements
                ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;
                //? if >=1.21.3 {
                /*handler.sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
                *///?} else {
                handler.sendPacket(new TeleportConfirmC2SPacket(packet.getTeleportId()));
                //?}
                
                // Cancel the packet so the client doesn't visually snap back
                ci.cancel();
            }
        }
    }

    @Inject(method = "clearWorld", at = @At("HEAD"))
    private void onClearWorld(CallbackInfo ci) {
        despairscent.skyblockm.tweaks.ItemDisplayBakingManager.clear();
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(String command, CallbackInfo ci) {
        if (command == null) return;
        String trimmed = command.trim();
        if (trimmed.equalsIgnoreCase("ads off")) {
            if (CONFIG != null && CONFIG.adBlocker != null) {
                CONFIG.adBlocker.enabled = true;
                CONFIG.save();
                if (CLIENT.inGameHud != null) {
                    CLIENT.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§6SkyBlockM§7] §aБлокировка рекламы включена"));
                }
            }
            ci.cancel();
        } else if (trimmed.equalsIgnoreCase("ads on")) {
            if (CONFIG != null && CONFIG.adBlocker != null) {
                CONFIG.adBlocker.enabled = false;
                CONFIG.save();
                if (CLIENT.inGameHud != null) {
                    CLIENT.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§6SkyBlockM§7] §cБлокировка рекламы выключена"));
                }
            }
            ci.cancel();
        } else if (trimmed.equalsIgnoreCase("ads") || trimmed.equalsIgnoreCase("ads toggle")) {
            if (CONFIG != null && CONFIG.adBlocker != null) {
                CONFIG.adBlocker.enabled = !CONFIG.adBlocker.enabled;
                CONFIG.save();
                if (CLIENT.inGameHud != null) {
                    CLIENT.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("§7[§6SkyBlockM§7] Блокировка рекламы: " + (CONFIG.adBlocker.enabled ? "§aВКЛ" : "§cВЫКЛ")));
                }
            }
            ci.cancel();
        }
    }
}
