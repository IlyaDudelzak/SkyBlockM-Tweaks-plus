package despairscent.skyblockm.tweaks;

import net.minecraft.client.gui.hud.ChatHudLine;

import java.util.List;

public class AdBlockerState {
    private static boolean blocking = false;
    private static boolean blockNextIfEmpty = false;
    private static int blockCount = 0;

    public static boolean processMessage(String text, List<ChatHudLine> messages, Runnable resetAction) {
        if (blockNextIfEmpty) {
            blockNextIfEmpty = false;
            if (text.trim().isEmpty()) {
                return true;
            }
        }

        if (text.contains("Очень важное объявление")) {
            blocking = true;
            blockCount = 0;

            if (messages != null && !messages.isEmpty()) {
                try {
                    if (messages.get(0).content().getString().trim().isEmpty()) {
                        messages.remove(0);
                        if (resetAction != null) {
                            resetAction.run();
                        }
                    }
                } catch (Exception ignored) {}
            }

            return true;
        }

        if (blocking) {
            blockCount++;
            if (text.startsWith("┗┅") || text.contains("┗┅")) {
                blocking = false;
                blockNextIfEmpty = true;
                return true;
            }
            if (blockCount > 20) {
                blocking = false;
                return false;
            }
            return true;
        }

        return false;
    }
}
