package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.WarpManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.admin")
public class DelWarpCommand extends RDCommand {
    private final WarpManager manager;

    public DelWarpCommand(WarpManager manager) {
        super("delwarp");

        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "/delwarp <warp>");
            return;
        }

        if (manager.deleteWarp(args[0])) {
            player.sendMessage(ChatColor.RED + "The specified warp was deleted.");
            return;
        }

        player.sendMessage(ChatColor.RED + "A warp with that name could not be found!");
    }
}
