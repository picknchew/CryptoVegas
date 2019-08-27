package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;
import tech.rayline.core.util.RunnableShorthand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ModeCommand extends RDCommand {
    private Core plugin;

    public ModeCommand() {
        super("mode");
    }

    @Override
    public void onCommandRegister() {
        plugin = (Core) getPlugin();
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        CryptoPlayer cryptoPlayer = plugin.getPlayer(player);
        Mode mode = cryptoPlayer.getMode() == Mode.PLAY_MONEY ? Mode.REAL_MONEY : Mode.PLAY_MONEY;

        cryptoPlayer.setMode(mode);
        changeMode(cryptoPlayer, mode);
        player.sendMessage(plugin.formatAt("change-mode").with("mode", mode).get());
    }

    private void changeMode(CryptoPlayer cryptoPlayer, Mode mode) {
        RunnableShorthand.forPlugin(plugin).async().with(() -> {
            try (Connection connection = plugin.getSQLConnection()) {
                try (PreparedStatement statement = plugin.getStatement("update-mode", connection)) {
                    statement.setInt(1, mode.ordinal());
                    statement.setString(2, cryptoPlayer.getPlayer().getUniqueId().toString());
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }
}
