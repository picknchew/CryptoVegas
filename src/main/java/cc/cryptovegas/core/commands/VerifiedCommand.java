package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.player.PlayerStats;
import cc.cryptovegas.core.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.RDCommand;
import tech.rayline.core.util.RunnableShorthand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

@CommandMeta(aliases={"/verify"})
public class VerifiedCommand extends RDCommand {
    private Core core;

    private static final Duration TIME_PLAYED_REQUIREMENT = Duration.ofDays(1);
    private static final int REAL_BETS_REQUIREMENT = 30;

    public VerifiedCommand() {
        super("verified");
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        CryptoPlayer cryptoPlayer = core.getPlayer(player);

        if (cryptoPlayer.isVerified()) {
            player.sendMessage(ChatColor.YELLOW + "You are already verified.");
            return;
        }

        PlayerStats stats = cryptoPlayer.getStats();

        Duration timePlayed = stats.getTimePlayed();
        int bets = stats.getRealBetsMade();

        if (player.isOp() || (bets >= REAL_BETS_REQUIREMENT && timePlayed.compareTo(TIME_PLAYED_REQUIREMENT) >= 0)) {
            player.sendMessage(ChatColor.GREEN + "You met all of the requirements and have been verified.");
            cryptoPlayer.setVerified(true);
            player.addAttachment(core).setPermission("deluxetags.tag.verified", true);

            RunnableShorthand.forPlugin(core).async().with(() -> {
                try (Connection connection = core.getSQLConnection()) {
                    try (PreparedStatement statement = core.getStatement("update-verified", connection)) {
                        statement.setString(1, player.getUniqueId().toString());
                        statement.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }).go();

            return;
        }

        player.sendMessage(ChatColor.GREEN + "Verification progress:\n" +
                ChatColor.DARK_AQUA + "Play time: " + ChatColor.RED + TimeUtil.toDaysPart(timePlayed) + " days, " + TimeUtil.toHoursPart(timePlayed) + " hours, " + TimeUtil.toMinutesPart(timePlayed) + " minutes, " + TimeUtil.toSecondsPart(timePlayed) + " seconds.\n" +
                ChatColor.DARK_AQUA + "Bets (real coins): " + ChatColor.RED + bets + "\n" +
                ChatColor.GOLD + "You must have placed at least 30 bets (real-money) and have 1 or more days of play-time.");
    }
}
