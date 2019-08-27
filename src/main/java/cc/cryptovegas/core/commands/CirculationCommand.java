package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;
import tech.rayline.core.util.RunnableShorthand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@CommandPermission("cryptovegas.admin")
public class CirculationCommand extends RDCommand {
    private Core core;

    public CirculationCommand() {
        super("circulation");
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
    protected void handleCommandUnspecific(CommandSender sender, String[] args) throws CommandException {
        sender.sendMessage(ChatColor.RED + "Calculating real coins in circulation...");

        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("sum-real-coins", connection)) {
                    ResultSet resultSet = statement.executeQuery();

                    resultSet.next();
                    sender.sendMessage(ChatColor.DARK_AQUA + "Total amount in circulation: " + ChatColor.YELLOW + Coin.valueOf(resultSet.getLong(1)).toFriendlyString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }
}
