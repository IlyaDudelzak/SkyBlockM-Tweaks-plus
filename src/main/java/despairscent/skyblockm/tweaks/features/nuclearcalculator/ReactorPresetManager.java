package despairscent.skyblockm.tweaks.features.nuclearcalculator;

import com.google.gson.annotations.SerializedName;
import despairscent.skyblockm.tweaks.features.nuclearcalculator.components.ReactorComponent;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ReactorPresetManager {
    public static final int MAX_SLOTS = 3;
    private static final String FILE_NAME = "reactor_sim_config.json";

    public static class PresetSlot {
        @SerializedName("name")
        public String name;

        @SerializedName("grid")
        public ReactorComponent[] grid = new ReactorComponent[54];

        @SerializedName("neutronReflector")
        public boolean neutronReflector = false;

        @SerializedName("hullHeat")
        public int hullHeat = 0;

        public PresetSlot() {}

        public PresetSlot(String name, ReactorComponent[] grid, boolean neutronReflector, int hullHeat) {
            this.name = name;
            this.grid = new ReactorComponent[54];
            if (grid != null) {
                for (int i = 0; i < 54 && i < grid.length; i++) {
                    this.grid[i] = grid[i] != null ? grid[i].copy() : null;
                }
            }
            this.neutronReflector = neutronReflector;
            this.hullHeat = hullHeat;
        }

        public boolean isEmpty() {
            if (grid == null) return true;
            for (ReactorComponent c : grid) {
                if (c != null) return false;
            }
            return true;
        }

        public int getComponentCount() {
            if (grid == null) return 0;
            int count = 0;
            for (ReactorComponent c : grid) {
                if (c != null) count++;
            }
            return count;
        }
    }

    public static class PresetConfig {
        @SerializedName("slots")
        public PresetSlot[] slots = new PresetSlot[MAX_SLOTS];
    }

    private static PresetConfig config = new PresetConfig();
    private static boolean loaded = false;

    private static File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME).toFile();
    }

    public static synchronized void load() {
        File file = getConfigFile();
        boolean loadedFromFile = false;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                PresetConfig parsed = ReactorSimulator.GSON.fromJson(reader, PresetConfig.class);
                if (parsed != null && parsed.slots != null) {
                    config = parsed;
                    if (config.slots.length < MAX_SLOTS) {
                        PresetSlot[] newSlots = new PresetSlot[MAX_SLOTS];
                        System.arraycopy(config.slots, 0, newSlots, 0, config.slots.length);
                        config.slots = newSlots;
                    }
                    loadedFromFile = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!loadedFromFile) {
            try (java.io.InputStream is = ReactorPresetManager.class.getResourceAsStream("/tweaks/reactor_sim_config.json")) {
                if (is != null) {
                    try (java.io.InputStreamReader isr = new java.io.InputStreamReader(is, StandardCharsets.UTF_8)) {
                        PresetConfig parsed = ReactorSimulator.GSON.fromJson(isr, PresetConfig.class);
                        if (parsed != null && parsed.slots != null) {
                            config = parsed;
                            if (config.slots.length < MAX_SLOTS) {
                                PresetSlot[] newSlots = new PresetSlot[MAX_SLOTS];
                                System.arraycopy(config.slots, 0, newSlots, 0, config.slots.length);
                                config.slots = newSlots;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            save();
        }
        loaded = true;
    }

    public static synchronized void save() {
        File file = getConfigFile();
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                ReactorSimulator.GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized PresetSlot getSlot(int slotIndex) {
        if (!loaded) load();
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS || config.slots == null) return null;
        return config.slots[slotIndex];
    }

    public static synchronized void setSlot(int slotIndex, PresetSlot slot) {
        if (!loaded) load();
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            if (config.slots == null) {
                config.slots = new PresetSlot[MAX_SLOTS];
            }
            config.slots[slotIndex] = slot;
            save();
        }
    }

    public static synchronized boolean hasSlot(int slotIndex) {
        PresetSlot slot = getSlot(slotIndex);
        return slot != null && !slot.isEmpty();
    }

    public static synchronized void clearSlot(int slotIndex) {
        if (!loaded) load();
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS && config.slots != null) {
            config.slots[slotIndex] = null;
            save();
        }
    }
}
