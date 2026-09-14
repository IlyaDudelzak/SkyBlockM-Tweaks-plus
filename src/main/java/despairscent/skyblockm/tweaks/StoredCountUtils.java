package despairscent.skyblockm.tweaks;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

public class StoredCountUtils {

    private static final Pattern TITLE_GARBAGE = Pattern.compile("[\\uF800-\\uF8FF\\u1001-\\u103F\\u2000-\\u200F]*[0-9]+[0-9.KMBkmb]*[\\uF800-\\uF8FF\\u1001-\\u103F\\u2000-\\u200F]*");

    public static String getStoredCountFormatted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Long count = getStoredCount(stack);
        if (count == null || count <= 1) {
            return null;
        }
        return formatCount(count);
    }

    public static Long getStoredCount(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return null;
        }
        for (Text line : lore.lines()) {
            String str = line.getString();
            int idx = str.indexOf("Хранится:");
            if (idx != -1) {
                return parseCount(str.substring(idx + "Хранится:".length()));
            }
        }
        return null;
    }

    public static Long parseCount(String str) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0 && c != ' ' && c != ',' && c != '.') {
                break;
            }
        }
        if (digits.length() > 0) {
            try {
                return Long.parseLong(digits.toString());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static String formatCount(long count) {
        if (count < 1000) {
            return String.valueOf(count);
        } else if (count < 10000) {
            long rem = (count % 1000) / 100;
            if (rem == 0) {
                return (count / 1000) + "K";
            } else {
                return (count / 1000) + "." + rem + "K";
            }
        } else if (count < 1_000_000) {
            return (count / 1000) + "K";
        } else if (count < 10_000_000) {
            long rem = (count % 1_000_000) / 100_000;
            if (rem == 0) {
                return (count / 1_000_000) + "M";
            } else {
                return (count / 1_000_000) + "." + rem + "M";
            }
        } else if (count < 1_000_000_000) {
            return (count / 1_000_000) + "M";
        } else {
            long rem = (count % 1_000_000_000) / 100_000_000;
            if (rem == 0) {
                return (count / 1_000_000_000) + "B";
            } else {
                return (count / 1_000_000_000) + "." + rem + "B";
            }
        }
    }

    public static Text cleanTitle(Text title) {
        if (title == null) {
            return null;
        }
        String str = title.getString();
        boolean hasPua = false;
        boolean hasDigit = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '\uF800' && c <= '\uF8FF') hasPua = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasPua && hasDigit) break;
        }
        if (!hasPua || !hasDigit) {
            return title;
        }

        if (!title.getSiblings().isEmpty()) {
            MutableText cleaned = Text.empty().setStyle(title.getStyle());
            for (Text child : title.getSiblings()) {
                String childStr = child.getString();
                String res = TITLE_GARBAGE.matcher(childStr).replaceAll("");
                if (!res.isEmpty()) {
                    if (res.equals(childStr)) {
                        cleaned.append(child);
                    } else {
                        cleaned.append(Text.literal(res).setStyle(child.getStyle()));
                    }
                }
            }
            return cleaned;
        } else {
            String res = TITLE_GARBAGE.matcher(str).replaceAll("");
            return Text.literal(res).setStyle(title.getStyle());
        }
    }
}
