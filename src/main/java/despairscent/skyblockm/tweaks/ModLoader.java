package despairscent.skyblockm.tweaks;

import despairscent.skyblockm.tweaks.features.captcha.CaptchaDetector;
import despairscent.skyblockm.tweaks.features.captcha.DependencyDownloader;
import despairscent.skyblockm.tweaks.features.captcha.SmoothTargetBot;
import despairscent.skyblockm.tweaks.features.itemdisplay.BarrierModelPlugin;
import despairscent.skyblockm.tweaks.features.itemdisplay.ItemDisplayBakingManager;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.GameMenuCalculatorButton;import despairscent.skyblockm.tweaks.features.nuclearcalculator.NuclearReactorDetectorModule;import despairscent.skyblockm.tweaks.features.resourcepack.SkyBlockPackManager;

import despairscent.skyblockm.tweaks.config.Config;
import despairscent.skyblockm.tweaks.features.compactgenome.CompactGenomeModule;
import despairscent.skyblockm.tweaks.features.esterminalscroll.EsTerminalScrollModule;
import despairscent.skyblockm.tweaks.features.inventorydesyncfix.InventoryDesyncFixModule;
import net.fabricmc.api.ClientModInitializer;
//? if <1.21.8 {
/*import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
*///?}
import net.minecraft.block.Blocks;

import static despairscent.skyblockm.tweaks.ModUtils.CONFIG;

public class ModLoader implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CONFIG = Config.load();
        CONFIG.save();

        CompactGenomeModule.init();
        EsTerminalScrollModule.init();
        InventoryDesyncFixModule.init();
        CaptchaDetector.init();
        SmoothTargetBot.register();
        DependencyDownloader.initAsync();
        GameMenuCalculatorButton.init();
        NuclearReactorDetectorModule.init();

        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(new despairscent.skyblockm.tweaks.features.itemdisplay.BarrierModelPlugin());
        //? if <1.21.8 {
        /*BlockRenderLayerMap.INSTANCE.putBlock(Blocks.BARRIER, RenderLayer.getTranslucent());
        *///?}

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.util.Identifier getFabricId() {
                    //? if >=1.21 {
                    return net.minecraft.util.Identifier.of("skyblockm-tweaks", "baking_reload_listener");
                    //?} else {
                    /*return new net.minecraft.util.Identifier("skyblockm-tweaks", "baking_reload_listener");
                    *///?}
                }

                @Override
                public void reload(net.minecraft.resource.ResourceManager manager) {
                    ItemDisplayBakingManager.onResourceReload();
                    System.gc();
                }
            }
        );

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            despairscent.skyblockm.tweaks.ModUtils.CLIENT = client;
            SkyBlockPackManager.addDefaultServersIfFirstLaunch(client);
            SkyBlockPackManager.onClientStarted(client);
        });
    }

}
