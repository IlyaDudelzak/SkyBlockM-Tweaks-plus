package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.config.Config;
import despairscent.skyblockm.tweaks.modules.compactgenome.CompactGenomeModule;
import despairscent.skyblockm.tweaks.modules.esterminalscroll.EsTerminalScrollModule;
import despairscent.skyblockm.tweaks.modules.inventorydesyncfix.InventoryDesyncFixModule;
import net.fabricmc.api.ClientModInitializer;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ModLoader implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();
        CONFIG.save();

        CompactGenomeModule.init();
        EsTerminalScrollModule.init();
        InventoryDesyncFixModule.init();

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.util.Identifier getFabricId() {
                    return new net.minecraft.util.Identifier("skyblockm-tweaks", "baking_reload_listener");
                }

                @Override
                public void reload(net.minecraft.resource.ResourceManager manager) {
                    ItemDisplayBakingManager.onResourceReload();
                }
            }
        );

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            despairscent.skyblockm.tweaks.ModUtils.CLIENT = client;
            SkyBlockPackManager.onClientStarted(client);
        });
    }

}
