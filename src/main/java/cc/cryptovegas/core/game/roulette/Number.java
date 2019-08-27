package cc.cryptovegas.core.game.roulette;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class Number implements ConfigurationSerializable {
    private final int number;
    private final RouletteGame.Color color;

    public Number(int number, RouletteGame.Color color) {
        this.number = number;
        this.color = color;
    }

    public int getNumber() {
        return number;
    }

    public RouletteGame.Color getColor() {
        return color;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("number", number);
        map.put("color", color.toString());

        return map;
    }

    public static Number deserialize(Map<String, Object> map) {
        return new Number((Integer) map.get("number"), RouletteGame.Color.valueOf((String) map.get("color")));
    }
}