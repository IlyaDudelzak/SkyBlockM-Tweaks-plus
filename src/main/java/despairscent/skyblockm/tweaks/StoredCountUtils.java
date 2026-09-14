package despairscent.skyblockm.tweaks;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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
        if (stack == null) {
            return null;
        }
        NbtCompound display = stack.getSubNbt(ItemStack.DISPLAY_KEY);
        if (display == null || !display.contains(ItemStack.LORE_KEY, NbtElement.LIST_TYPE)) {
            return null;
        }
        NbtList loreList = display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);
        for (int i = 0; i < loreList.size(); i++) {
            String json = loreList.getString(i);
            int idx = json.indexOf("Хранится:");
            if (idx != -1) {
                return parseCount(json.substring(idx + "Хранится:".length()));
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
        if (str.length() > 20 && str.startsWith("\uF808")) {
            int slotIdx = -1;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\uF821' || c == 'ထ' || (i >= 7 && Character.isDigit(c))) {
                    slotIdx = i;
                    break;
                }
            }

            if (slotIdx != -1) {
                java.util.List<Text> siblings = title.getSiblings();
                if (!siblings.isEmpty()) {
                    MutableText clean = Text.empty().setStyle(title.getStyle());
                    int accumulated = 0;
                    for (Text sibling : siblings) {
                        String childStr = sibling.getString();
                        if (accumulated + childStr.length() <= slotIdx) {
                            clean.append(sibling);
                            accumulated += childStr.length();
                        } else {
                            int remain = slotIdx - accumulated;
                            if (remain > 0) {
                                clean.append(Text.literal(childStr.substring(0, remain)).setStyle(sibling.getStyle()));
                            }
                            break;
                        }
                    }
                    if (!clean.getString().isEmpty()) {
                        return clean;
                    }
                }

                String cleanStr = str.substring(0, slotIdx);
                return Text.literal(cleanStr).setStyle(title.getStyle());
            }
        }
        return title;
    }
}
