package cc.cryptovegas.core.commands.essential;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tech.rayline.core.command.*;

@CommandPermission("cryptovegas.admin")
@CommandMeta(aliases={"tp"})
public class TeleportCommand extends RDCommand {

    public TeleportCommand() {
        super("teleport");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new ArgumentRequirementException("/tp <target> [player]");
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(formatAt("not-a-player").with("player", args[0]).get());
            return;
        }

        if (args.length == 1) {
            player.teleport(target);
            player.sendMessage(formatAt("teleport-to").with("player", target.getName()).get());
            return;
        }

        Player target2 = Bukkit.getPlayer(args[1]);

        if (target2 == null) {
            player.sendMessage(formatAt("not-a-player").with("player", args[1]).get());
            return;
        }

        target.teleport(target2);
        target.sendMessage(formatAt("teleport-to").with("player", target2.getName()).get());
        player.sendMessage(formatAt("teleport-other").with("player", target.getName()).with("target", target2.getName()).get());
    }
}
