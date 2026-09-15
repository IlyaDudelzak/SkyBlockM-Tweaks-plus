package despairscent.skyblockm.tweaks.mixin;

import despairscent.skyblockm.tweaks.modules.inventorydesyncfix.InventoryDesyncFixModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static despairscent.skyblockm.tweaks.ModUtils.CLIENT;
import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onUpdateSelectedSlot",
            at = @At("TAIL"))
    private void handlePacketInject(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
        if (CLIENT.isOnThread()) {
            InventoryDesyncFixModule.handleSelectedSlotUpdate();
        }
    }

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    private void onPlayerPositionLookInject(net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (CONFIG.itemDisplayHitbox.enabled && CONFIG.itemDisplayHitbox.antiRubberband && CLIENT.player != null) {
            double dx = CLIENT.player.getX() - packet.getX();
            double dy = CLIENT.player.getY() - packet.getY();
            double dz = CLIENT.player.getZ() - packet.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            double maxDist = CONFIG.itemDisplayHitbox.antiRubberbandDistance;
            
            // Ignore small rubberbands
            if (distSq > 0 && distSq < maxDist * maxDist) {
                // We MUST confirm the teleport to the server, otherwise it will ignore our future movements
                ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;
                handler.sendPacket(new net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket(packet.getTeleportId()));
                
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
