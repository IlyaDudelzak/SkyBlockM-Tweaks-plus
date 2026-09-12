package despairscent.skyblockm.tweaks.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import despairscent.skyblockm.tweaks.ModUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.NoticeScreen;

import static despairscent.skyblockm.tweaks.ModUtils.i18n;

public class ModMenuImplementation implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config") || FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return ClothConfigHelper.getScreenFactory();
        } else {
            return parent -> new NoticeScreen(
                    () -> {
                        if (ModUtils.CLIENT != null) {
                            ModUtils.CLIENT.setScreen(parent);
                        }
                    },
                    i18n("config.missing_cloth.title"),
                    i18n("config.missing_cloth.desc")
            );
        }
    }

    private static class ClothConfigHelper {
        private static ConfigScreenFactory<?> getScreenFactory() {
            return ClothConfigImplementation::generate;
        }
    }
}
