package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tech.rayline.core.command.ArgumentRequirementException;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.tpa")
public class TPACommand extends RDCommand {
    private final TeleportManager manager;

    public TPACommand(TeleportManager manager) {
        super("tpa");

        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new ArgumentRequirementException("/tpa <target>");
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(formatAt("not-a-player").with("player", args[0]).get());
            return;
        }

        if (player == target) {
            player.sendMessage(formatAt("teleport-self").get());
            return;
        }

        manager.addRequest(target, player);
        player.sendMessage(formatAt("tpa").with("player", target.getName()).get());
        target.sendMessage(formatAt("tpa-request").with("player", player.getName()).get());
    }
}
