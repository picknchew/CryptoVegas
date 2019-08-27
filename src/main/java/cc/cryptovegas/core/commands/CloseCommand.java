package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import org.bukkit.command.CommandSender;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.close")
public class CloseCommand extends RDCommand {
    private Core core;

    public CloseCommand() {
        super("close");
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
        core.restart();
    }
}
