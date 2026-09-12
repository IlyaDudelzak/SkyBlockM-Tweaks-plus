package despairscent.skyblockm.tweaks.modules.compactgenome;

enum GenomeVariant {
    DOMINANT,
    RECESSIVE;

    static GenomeVariant parse(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim()) {
            case "Доминантный", "Домінантний", "Dominant" -> DOMINANT;
            case "Рецессивный", "Рецесивний", "Recessive" -> RECESSIVE;
            default -> null;
        };
    }
}
