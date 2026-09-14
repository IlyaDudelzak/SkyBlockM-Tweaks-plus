package despairscent.skyblockm.tweaks;

public class AdBlockerState {
    private static boolean blocking = false;
    private static int blockCount = 0;

    public static boolean processMessage(String text) {
        if (text.contains("Очень важное объявление")) {
            blocking = true;
            blockCount = 0;
            return true;
        }

        if (blocking) {
            blockCount++;
            if (text.startsWith("┗┅") || text.contains("┗┅")) {
                blocking = false;
                return true;
            }
            if (blockCount > 20) {
                // Safety reset in case the closing bracket is missed
                blocking = false;
                return false;
            }
            return true;
        }

        return false;
    }
}
