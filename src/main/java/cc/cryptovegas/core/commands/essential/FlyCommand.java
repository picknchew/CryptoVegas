package cc.cryptovegas.core.commands.essential;

import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.fly")
public class FlyCommand extends RDCommand {

    public FlyCommand() {
        super("fly");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        boolean flyingAllowed = player.getAllowFlight();
        player.setAllowFlight(!flyingAllowed);

        if (flyingAllowed) {
            player.sendMessage(formatAt("fly-disabled").get());
            return;
        }

        player.sendMessage(formatAt("fly").get());
    }
}
