package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.cryptocurrency.CoinbaseHandler;
import cc.cryptovegas.core.gui.CashierGUI;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.RDCommand;

@CommandMeta(aliases={"withdraw", "deposit"})
public class CashierCommand extends RDCommand {
    private final CoinbaseHandler coinbaseHandler;
    private Core core;

    public CashierCommand(CoinbaseHandler coinbaseHandler) {
        super("cashier");
        this.coinbaseHandler = coinbaseHandler;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        new CashierGUI(core, coinbaseHandler, player).openFor(player);
    }
}
