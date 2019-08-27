package cc.cryptovegas.core.commands;

import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

public class HelpCommand extends RDCommand {

    public HelpCommand() {
        super("help");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(formatAt("help").with("player", player.getName()).get());
    }
}
