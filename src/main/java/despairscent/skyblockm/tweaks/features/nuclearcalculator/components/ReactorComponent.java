package despairscent.skyblockm.tweaks.features.nuclearcalculator.components;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.reflect.Type;

public class ReactorComponent implements Serializable {
    @SerializedName(value = "name", alternate = {"type", "id"})
    protected final ReactorComponentType type;

    @SerializedName(value = "currentHeat", alternate = {"heat"})
    protected int currentHeat = 0;

    public ReactorComponent(ReactorComponentType type) {
        this(type, 0);
    }

    public ReactorComponent(ReactorComponentType type, int currentHeat) {
        this.type = type;
        setHeat(currentHeat);
    }

    public ReactorComponentType getType() {
        return type;
    }

    public int getCurrentHeat() {
        return currentHeat;
    }

    public int getMaxHeat() {
        return type != null ? type.getMaxHeat() : 0;
    }

    public boolean canAcceptHeat() {
        return getMaxHeat() > 0;
    }

    public boolean isBroken() {
        return getMaxHeat() > 0 && currentHeat >= getMaxHeat();
    }

    public int addHeat(int amount) {
        if (!canAcceptHeat() || isBroken()) return amount;
        int max = getMaxHeat();
        int space = max - currentHeat;
        if (amount <= space) {
            currentHeat += amount;
            return 0;
        } else {
            currentHeat = max;
            return amount - space; // Излишек уходит в корпус или взрывает компонент
        }
    }

    public void dissipate(int amount) {
        if (type != null && type.isCoolingElement()) return;
        this.currentHeat = Math.max(0, this.currentHeat - amount);
    }

    public void setHeat(int heat) {
        int max = getMaxHeat();
        if (max > 0) {
            this.currentHeat = Math.max(0, Math.min(max, heat));
        } else {
            this.currentHeat = 0;
        }
    }

    public ReactorComponent copy() {
        return type != null ? type.create(this.currentHeat) : new ReactorComponent(null, this.currentHeat);
    }

    // Helper checks
    public boolean isFuelRod() { return type != null && type.isFuelRod(); }
    public boolean isVent() { return type != null && type.isVent(); }
    public boolean isExchanger() { return type != null && type.isExchanger(); }
    public boolean isCoolingElement() { return type != null && type.isCoolingElement(); }

    // Heat Vent delegates
    public int getSelfCooling() { return type != null ? type.getSelfCooling() : 0; }
    public int getSideCooling() { return type != null ? type.getSideCooling() : 0; }
    public int getReactorDrain() { return type != null ? type.getReactorDrain() : 0; }

    // Heat Exchanger delegates
    public int getSideExchange() { return type != null ? type.getSideExchange() : 0; }
    public int getReactorExchange() { return type != null ? type.getReactorExchange() : 0; }

    // Fuel Rod delegates
    public boolean isMox() { return type != null && type.isMox(); }

    public int calculateEnergy(int adjacentRods) {
        return calculateEnergy(adjacentRods, 0, 10000);
    }

    public int calculateEnergy(int adjacentRods, int hullHeat, int maxHullHeat) {
        if (type == null || !type.isFuelRod()) return 0;
        int base = type.getBaseEnergy() + (type.getBonusEnergy() * adjacentRods);
        if (type.isMox() && maxHullHeat > 0) {
            double heatRatio = Math.min(1.0, (double) hullHeat / maxHullHeat);
            return (int) Math.round(base * (1.0 + 4.0 * heatRatio));
        }
        return base;
    }

    public int calculateHeat(int adjacentRods) {
        if (type == null || !type.isFuelRod()) return 0;
        return type.getBaseHeat() + (type.getBonusHeat() * adjacentRods);
    }

    @Override
    public String toString() {
        return "ReactorComponent{" + (type != null ? type.getSerializedName() : "null") + ", heat=" + currentHeat + "}";
    }

    /**
     * Serializer & Deserializer for Gson support:
     * - serializes to: {"name": "basicHeatExchanger", "currentHeat": 0}
     * - deserializes from:
     *     - {"name": "basicHeatExchanger", "currentHeat": 0}
     *     - {"id": 220, "heat": 0}
     *     - {"type": "BASIC_HEAT_EXCHANGER", "currentHeat": 0}
     *     - "basicHeatExchanger" (primitive string)
     *     - 220 (primitive number)
     */
    public static class Serializer implements JsonSerializer<ReactorComponent>, JsonDeserializer<ReactorComponent> {
        @Override
        public JsonElement serialize(ReactorComponent src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null || src.type == null) return JsonNull.INSTANCE;
            JsonObject obj = new JsonObject();
            obj.addProperty("name", src.type.getSerializedName());
            obj.addProperty("currentHeat", src.currentHeat);
            return obj;
        }

        @Override
        public ReactorComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) return null;

            if (json.isJsonPrimitive()) {
                JsonPrimitive p = json.getAsJsonPrimitive();
                ReactorComponentType t = p.isNumber() ? ReactorComponentType.find(p.getAsInt()) : ReactorComponentType.find(p.getAsString());
                return t != null ? t.create(0) : null;
            }

            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                ReactorComponentType type = null;
                if (obj.has("name")) {
                    type = ReactorComponentType.find(obj.get("name").getAsString());
                }
                if (type == null && obj.has("id")) {
                    JsonElement idElem = obj.get("id");
                    if (idElem.isJsonPrimitive()) {
                        type = idElem.getAsJsonPrimitive().isNumber()
                                ? ReactorComponentType.find(idElem.getAsInt())
                                : ReactorComponentType.find(idElem.getAsString());
                    }
                }
                if (type == null && obj.has("type")) {
                    type = ReactorComponentType.find(obj.get("type").getAsString());
                }
                if (type == null) return null;

                int heat = 0;
                if (obj.has("currentHeat")) {
                    heat = obj.get("currentHeat").getAsInt();
                } else if (obj.has("heat")) {
                    heat = obj.get("heat").getAsInt();
                }

                return type.create(heat);
            }
            return null;
        }
    }
}