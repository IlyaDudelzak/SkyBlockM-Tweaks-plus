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
        if (isCraftable(stack)) {
            return "+";
        }
        return null;
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

    private static final Pattern ES_NUMBER_CHARS = Pattern.compile("[0-9.KM+kmb\\u102B\\u102E\\u1030-\\u1039\\u104B\\u104D]+");
    private static final Pattern ES_BG_CHARS = Pattern.compile("[\\u1001-\\u1032\\u2001-\\u2032]+");

    public static boolean isTerminalTitle(Text title) {
        if (title == null) {
            return false;
        }
        java.util.List<Text> siblings = title.getSiblings();
        if (siblings.isEmpty()) {
            return false;
        }
        for (int i = 0; i < siblings.size() && i < 3; i++) {
            Text child = siblings.get(i);
            net.minecraft.util.Identifier font = child.getStyle().getFont();
            if (font != null && "electric_storage".equals(font.getNamespace()) && "interfaces".equals(font.getPath())) {
                return true;
            }
        }
        return false;
    }

    public static Text cleanTitle(Text title) {
        if (!isTerminalTitle(title)) {
            return title;
        }

        MutableText cleaned = title.copyContentOnly().setStyle(title.getStyle());
        for (Text child : title.getSiblings()) {
            cleanSiblingInto(child, cleaned);
        }
        return cleaned;
    }

    private static void cleanSiblingInto(Text child, MutableText destination) {
        net.minecraft.util.Identifier font = child.getStyle().getFont();
        String fontPath = font != null ? font.getPath() : "";
        String fontNamespace = font != null ? font.getNamespace() : "";

        if ("electric_storage".equals(fontNamespace)) {
            if ("interfaces".equals(fontPath) || fontPath.startsWith("terminal_slider")) {
                destination.append(child);
                return;
            }

            if (fontPath.startsWith("ascii_row")) {
                String text = child.getString();
                String stripped = ES_NUMBER_CHARS.matcher(text).replaceAll("");
                if (!stripped.isEmpty()) {
                    destination.append(Text.literal(stripped).setStyle(child.getStyle()));
                }
                return;
            }

            if (fontPath.startsWith("background")) {
                String text = child.getString();
                String stripped = ES_BG_CHARS.matcher(text).replaceAll("");
                if (!stripped.isEmpty()) {
                    destination.append(Text.literal(stripped).setStyle(child.getStyle()));
                }
                return;
            }
        }

        if (!child.getSiblings().isEmpty()) {
            MutableText subCleaned = child.copyContentOnly().setStyle(child.getStyle());
            for (Text sub : child.getSiblings()) {
                cleanSiblingInto(sub, subCleaned);
            }
            destination.append(subCleaned);
            return;
        }

        String text = child.getString();
        String stripped = ES_NUMBER_CHARS.matcher(text).replaceAll("");
        if (stripped.equals(text)) {
            destination.append(child);
        } else if (!stripped.isEmpty()) {
            destination.append(Text.literal(stripped).setStyle(child.getStyle()));
        }
    }
}
