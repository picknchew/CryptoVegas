package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.TeleportManager;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

public class TPAcceptCommand extends RDCommand {
    private final TeleportManager manager;

    public TPAcceptCommand(TeleportManager manager) {
        super("tpaccept");
        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (manager.requestValid(player)) {
            Player target = manager.consumeRequest(player);

            player.sendMessage(formatAt("tpa-accept").with("player", target.getName()).get());
            target.sendMessage(formatAt("tpa-teleport").with("player", player.getName()).get());
            return;
        }

        player.sendMessage(formatAt("invalid-teleport-request").get());
    }
}
