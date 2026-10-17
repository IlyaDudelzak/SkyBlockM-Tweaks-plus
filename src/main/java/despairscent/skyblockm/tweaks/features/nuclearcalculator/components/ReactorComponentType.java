package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

import com.google.gson.annotations.SerializedName;

public enum ReactorComponentType {
    // 1. Охлаждающие элементы
    @SerializedName("coolingElement10k")
    COOLING_ELEMENT_10K(210, "coolingElement10k", Category.COOLING_ELEMENT, 10_000, 0, 0, 0, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("coolingElement30k")
    COOLING_ELEMENT_30K(211, "coolingElement30k", Category.COOLING_ELEMENT, 30_000, 0, 0, 0, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("coolingElement60k")
    COOLING_ELEMENT_60K(212, "coolingElement60k", Category.COOLING_ELEMENT, 60_000, 0, 0, 0, 0, 0, 0, 0, 0, 0, false),

    // 2. Теплообменники
    @SerializedName("basicHeatExchanger")
    BASIC_HEAT_EXCHANGER(220, "basicHeatExchanger", Category.HEAT_EXCHANGER, 2_500, 12, 4, 0, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("advancedHeatExchanger")
    ADVANCED_HEAT_EXCHANGER(221, "advancedHeatExchanger", Category.HEAT_EXCHANGER, 10_000, 24, 8, 0, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("componentHeatExchanger")
    COMPONENT_HEAT_EXCHANGER(222, "componentHeatExchanger", Category.HEAT_EXCHANGER, 5_000, 24, 0, 0, 0, 0, 0, 0, 0, 0, false),

    // 3. Теплоотводы
    @SerializedName("basicHeatVent")
    BASIC_HEAT_VENT(230, "basicHeatVent", Category.HEAT_VENT, 1_000, 0, 0, 12, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("advancedHeatVent")
    ADVANCED_HEAT_VENT(231, "advancedHeatVent", Category.HEAT_VENT, 1_000, 0, 0, 24, 0, 0, 0, 0, 0, 0, false),

    @SerializedName("componentHeatVent")
    COMPONENT_HEAT_VENT(232, "componentHeatVent", Category.HEAT_VENT, 0, 0, 0, 0, 10, 0, 0, 0, 0, 0, false),

    @SerializedName("reactorHeatVent")
    REACTOR_HEAT_VENT(233, "reactorHeatVent", Category.HEAT_VENT, 1_000, 0, 0, 12, 0, 12, 0, 0, 0, 0, false),

    @SerializedName("overclockedHeatVent")
    OVERCLOCKED_HEAT_VENT(234, "overclockedHeatVent", Category.HEAT_VENT, 1_000, 0, 0, 40, 0, 60, 0, 0, 0, 0, false),

    // 4. Топливные стержни (Уран)
    @SerializedName("singleUranium")
    SINGLE_URANIUM(240, "singleUranium", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 320, 320, 4, 6, false),

    @SerializedName("dualUranium")
    DUAL_URANIUM(241, "dualUranium", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 1280, 640, 24, 36, false),

    @SerializedName("quadUranium")
    QUAD_URANIUM(242, "quadUranium", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 2560, 1280, 96, 144, false),

    // 5. Топливные стержни (MOX)
    @SerializedName("singleMox")
    SINGLE_MOX(243, "singleMox", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 320, 320, 4, 6, true),

    @SerializedName("dualMox")
    DUAL_MOX(244, "dualMox", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 1280, 640, 24, 36, true),

    @SerializedName("quadMox")
    QUAD_MOX(245, "quadMox", Category.FUEL_ROD, 0, 0, 0, 0, 0, 0, 2560, 1280, 96, 144, true);

    public enum Category {
        COOLING_ELEMENT,
        HEAT_EXCHANGER,
        HEAT_VENT,
        FUEL_ROD
    }

    private final int id;
    private final String serializedName;
    private final Category category;
    private final int maxHeat;
    private final int sideExchange;
    private final int reactorExchange;
    private final int selfCooling;
    private final int sideCooling;
    private final int reactorDrain;
    private final int baseEnergy;
    private final int bonusEnergy;
    private final int baseHeat;
    private final int bonusHeat;
    private final boolean isMox;

    ReactorComponentType(int id, String serializedName, Category category, int maxHeat,
                         int sideExchange, int reactorExchange,
                         int selfCooling, int sideCooling, int reactorDrain,
                         int baseEnergy, int bonusEnergy, int baseHeat, int bonusHeat,
                         boolean isMox) {
        this.id = id;
        this.serializedName = serializedName;
        this.category = category;
        this.maxHeat = maxHeat;
        this.sideExchange = sideExchange;
        this.reactorExchange = reactorExchange;
        this.selfCooling = selfCooling;
        this.sideCooling = sideCooling;
        this.reactorDrain = reactorDrain;
        this.baseEnergy = baseEnergy;
        this.bonusEnergy = bonusEnergy;
        this.baseHeat = baseHeat;
        this.bonusHeat = bonusHeat;
        this.isMox = isMox;
    }

    public int getId() { return id; }
    public String getSerializedName() { return serializedName; }
    public Category getCategory() { return category; }
    public int getMaxHeat() { return maxHeat; }
    public int getSideExchange() { return sideExchange; }
    public int getReactorExchange() { return reactorExchange; }
    public int getSelfCooling() { return selfCooling; }
    public int getSideCooling() { return sideCooling; }
    public int getReactorDrain() { return reactorDrain; }
    public int getBaseEnergy() { return baseEnergy; }
    public int getBonusEnergy() { return bonusEnergy; }
    public int getBaseHeat() { return baseHeat; }
    public int getBonusHeat() { return bonusHeat; }
    public boolean isMox() { return isMox; }

    public boolean isCoolingElement() { return category == Category.COOLING_ELEMENT; }
    public boolean isExchanger() { return category == Category.HEAT_EXCHANGER; }
    public boolean isVent() { return category == Category.HEAT_VENT; }
    public boolean isFuelRod() { return category == Category.FUEL_ROD; }

    public ReactorComponent create(int heat) {
        return switch (category) {
            case COOLING_ELEMENT -> new CoolingElement(this, heat);
            case HEAT_EXCHANGER -> new HeatExchanger(this, heat);
            case HEAT_VENT -> new HeatVent(this, heat);
            case FUEL_ROD -> new FuelRod(this, heat);
        };
    }

    public ReactorComponent create() {
        return create(0);
    }

    public static ReactorComponentType byId(int id) {
        for (ReactorComponentType type : values()) {
            if (type.id == id) return type;
        }
        return null;
    }

    public static ReactorComponentType find(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) {
            int num = n.intValue();
            for (ReactorComponentType type : values()) {
                if (type.id == num || type.ordinal() == num) return type;
            }
            return null;
        }
        String str = val.toString().trim();
        for (ReactorComponentType type : values()) {
            if (type.serializedName.equalsIgnoreCase(str) ||
                type.name().equalsIgnoreCase(str) ||
                String.valueOf(type.id).equals(str) ||
                String.valueOf(type.ordinal()).equals(str)) {
                return type;
            }
        }
        String norm = str.replace("_", "").toLowerCase();
        for (ReactorComponentType type : values()) {
            if (type.name().replace("_", "").toLowerCase().equals(norm) ||
                type.serializedName.toLowerCase().equals(norm)) {
                return type;
            }
        }
        return null;
    }
}
