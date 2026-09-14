package despairscent.skyblockm.tweaks;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

public class StoredCountUtils {

    public static String getStoredCountFormatted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Long count = getStoredCount(stack);
        if (count != null) {
            if (count > 1) {
                return formatCount(count);
            } else if (count == 1) {
                return "1";
            }
            return null;
        }
        if (!hasAutocraft(stack) && isCraftable(stack)) {
            return "+";
        }
        return null;
    }

    public static boolean hasAutocraft(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return false;
        }
        for (Text line : lore.lines()) {
            String str = line.getString().toLowerCase(java.util.Locale.ROOT);
            String compact = str.replace(" ", "");
            if ((compact.contains("shift+пкм") || compact.contains("shift-пкм") || compact.contains("shift+rmb") || compact.contains("shift-rmb"))
                    && (str.contains("создать") || str.contains("створити") || str.contains("craft"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCraftable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return false;
        }
        for (Text line : lore.lines()) {
            String str = line.getString().toLowerCase(java.util.Locale.ROOT);
            if (str.contains("создать предмет") || str.contains("створити предмет") || str.contains("craft item") || (str.contains("создать") && str.contains("лкм"))) {
                return true;
            }
        }
        return false;
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

    public static boolean isTerminalTitle(Text title) {
        if (title == null) {
            return false;
        }
        return containsInterfacesFont(title);
    }

    private static boolean containsInterfacesFont(Text text) {
        net.minecraft.util.Identifier font = text.getStyle().getFont();
        if (font != null && "electric_storage".equals(font.getNamespace()) && "interfaces".equals(font.getPath())) {
            return true;
        }
        for (Text child : text.getSiblings()) {
            if (containsInterfacesFont(child)) {
                return true;
            }
        }
        return false;
    }

    public static Text cleanTitle(Text title) {
        return title;
    }
}
