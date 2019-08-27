package cc.cryptovegas.core.util;

import org.bukkit.block.Sign;

public final class BlockUtil {

    private BlockUtil() {
    }

    public static void clearSign(Sign sign) {
        for (int index = 0; index < 4; index++) {
            sign.setLine(index, "");
        }

        sign.update();
    }

    public static void updateSign(Sign sign, String... lines) {
        for (int index = 0; index < lines.length; index++) {
            if (lines[index] != null) {
                sign.setLine(index, lines[index]);
            }
        }

        sign.update();
    }
}
