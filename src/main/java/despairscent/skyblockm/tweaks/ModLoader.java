package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.config.Config;
import despairscent.skyblockm.tweaks.modules.compactgenome.CompactGenomeModule;
import despairscent.skyblockm.tweaks.modules.esterminalscroll.EsTerminalScrollModule;
import despairscent.skyblockm.tweaks.modules.inventorydesyncfix.InventoryDesyncFixModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ModLoader implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();
        CONFIG.save();

        CompactGenomeModule.init();
        EsTerminalScrollModule.init();
        InventoryDesyncFixModule.init();

        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(new despairscent.skyblockm.tweaks.BarrierModelPlugin());
        BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BARRIER, RenderLayer.getTranslucent());

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.util.Identifier getFabricId() {
                    return net.minecraft.util.Identifier.of("skyblockm-tweaks", "baking_reload_listener");
                }

                @Override
                public void reload(net.minecraft.resource.ResourceManager manager) {
                    ItemDisplayBakingManager.onResourceReload();
                }
            }
        );

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (CONFIG != null && CONFIG.skyblockPackOptimization != null && CONFIG.skyblockPackOptimization.enabled) {
                java.nio.file.Path packPath = client.getResourcePackDir().resolve("SkyBlockM.zip");
                if (java.nio.file.Files.exists(packPath)) {
                    java.util.List<String> enabled = new java.util.ArrayList<>(client.options.resourcePacks);
                    if (!enabled.contains("file/SkyBlockM.zip")) {
                        int insertIdx = 0;
                        for (int i = 0; i < enabled.size(); i++) {
                            String id = enabled.get(i);
                            if (id.equals("vanilla") || id.equals("fabric")) {
                                insertIdx = i + 1;
                            }
                        }
                        enabled.add(insertIdx, "file/SkyBlockM.zip");
                        client.options.resourcePacks.clear();
                        client.options.resourcePacks.addAll(enabled);
                        client.options.write();

                        client.getResourcePackManager().scanPacks();
                        client.getResourcePackManager().setEnabledProfiles(enabled);
                        client.reloadResources();
                    }
                }
            }
        });
    }

}
