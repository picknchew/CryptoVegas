package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.gui.BartenderGUI;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.RDCommand;

@CommandMeta(aliases={"bar"})
public class BartenderCommand extends RDCommand {
    private BartenderGUI gui;

    public BartenderCommand() {
        super("bartender");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    public void onCommandRegister() {
        this.gui = new BartenderGUI((Core) getPlugin());
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        gui.open(player);
    }
}
