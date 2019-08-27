package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;
import tech.rayline.core.util.RunnableShorthand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class RefillCommand extends RDCommand {
    private Core plugin;
    private final long cooldown = 30;
    private Coin claimAmount;

    public RefillCommand() {
        super("refill");
    }

    @Override
    public void onCommandRegister() {
        plugin = (Core) getPlugin();
        claimAmount = Coin.parseCoin("1");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        CryptoPlayer cryptoPlayer = plugin.getPlayer(player);
        Instant lastRefilled = cryptoPlayer.getLastRefilled();

        // duration between now and the next available time that the player can claim coins
        Duration duration = Duration.between(lastRefilled.plus(cooldown, ChronoUnit.MINUTES), Instant.now());

        // If current time - (last claimed + cooldown) is zero or positive, then player can claim coins.
        if (duration.isNegative()) {
            duration = duration.abs();
            long minutes = duration.toMinutes();
            long seconds = duration.minusMinutes(minutes).getSeconds();
            player.sendMessage(ChatColor.RED + "You must wait " + minutes + " minute(s) and " + seconds + " second(s) before you can refill play coins!");
            return;
        }

        player.sendMessage(ChatColor.RED + "You have refilled your play coins.");
        cryptoPlayer.setCoins(Mode.PLAY_MONEY, cryptoPlayer.getRealCoins().add(claimAmount));
        cryptoPlayer.setLastRefilled(Instant.now());

        plugin.getCurrencyHandler().addPlayCoins(cryptoPlayer.getUniqueId().toString(), claimAmount);

        RunnableShorthand.forPlugin(plugin).async().with(() -> {
            try (Connection connection = plugin.getSQLConnection()) {
                try (PreparedStatement statement = plugin.getStatement("update-last-refilled", connection)) {
                    statement.setLong(1, claimAmount.getValue());
                    statement.setString(2, player.getUniqueId().toString());
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }
}
