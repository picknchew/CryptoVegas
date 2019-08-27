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

@CommandPermission("cryptovegas.baltop")
public class BalanceTopCommand extends RDCommand {
    private Core core;

    public BalanceTopCommand() {
        super("baltop");
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
        sender.sendMessage(ChatColor.RED + "Fetching top balances...");

        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("top-balances", connection)) {
                    ResultSet resultSet = statement.executeQuery();

                    StringBuilder builder = new StringBuilder(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Top balances:");
                    int position = 1;

                    while (resultSet.next()) {
                        String uuid = resultSet.getString("uuid");
                        long realCoins = resultSet.getLong("real_coins");

                        builder.append("\n")
                                .append(ChatColor.DARK_AQUA)
                                .append(position)
                                .append(". ")
                                .append(uuid)
                                .append(": ")
                                .append(ChatColor.RESET)
                                .append(Coin.valueOf(realCoins).toFriendlyString());

                        position++;
                    }

                    sender.sendMessage(builder.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }
}
