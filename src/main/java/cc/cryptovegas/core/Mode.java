package cc.cryptovegas.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Mode {
    // mode 0 = play_money
    // mode 1  = fake_money
    REAL_MONEY("real"), PLAY_MONEY("play");

    private static final Map<Integer, Mode> modes = new HashMap<>();

    static {
        Arrays.stream(Mode.values()).forEach(mode -> modes.put(mode.ordinal(), mode));
    }

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Mode get(int ordinal) {
        return modes.get(ordinal);
    }
}
