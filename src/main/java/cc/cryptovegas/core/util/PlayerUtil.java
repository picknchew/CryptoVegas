package cc.cryptovegas.core.util;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.player.PlayerStats;
import com.google.common.base.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import tech.rayline.core.util.RunnableShorthand;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerUtil {
    private static final UUID invalidUserUUID = UUID.nameUUIDFromBytes("InvalidUsername".getBytes(Charsets.UTF_8));
    private static Constructor<?> gameProfileConstructor;
    private static Constructor<?> craftOfflinePlayerConstructor;

    static {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> serverClass = Bukkit.getServer().getClass();
            Class<?> craftOfflinePlayerClass = Class.forName(serverClass.getPackage().getName()+ ".CraftOfflinePlayer");

            gameProfileConstructor = gameProfileClass.getDeclaredConstructor(UUID.class, String.class);
            gameProfileConstructor.setAccessible(true);

            craftOfflinePlayerConstructor = craftOfflinePlayerClass.getDeclaredConstructor(serverClass, gameProfileClass);
            craftOfflinePlayerConstructor.setAccessible(true);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static OfflinePlayer getOfflinePlayerSkipLookup(String name) {
        try {
            Object gameProfile = gameProfileConstructor.newInstance(invalidUserUUID, name);
            Object craftOfflinePlayer = craftOfflinePlayerConstructor.newInstance(Bukkit.getServer(), gameProfile);

            return (OfflinePlayer) craftOfflinePlayer;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return Bukkit.getOfflinePlayer(name);
        }
    }

    public static void incrRealBets(CryptoPlayer cryptoPlayer, Core core) {
        PlayerStats stats = cryptoPlayer.getStats();

        stats.setRealBetsMade(stats.getRealBetsMade() + 1);

        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("increment-real-bets", connection)) {
                    statement.setString(1, cryptoPlayer.getUniqueId().toString());
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }
}
