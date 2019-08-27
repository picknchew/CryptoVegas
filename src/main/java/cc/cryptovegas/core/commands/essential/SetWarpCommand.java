package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.WarpManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.admin")
public class SetWarpCommand extends RDCommand {
    private final WarpManager manager;

    public SetWarpCommand(WarpManager manager) {
        super("setwarp");

        this.manager = manager;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED +"/setwarp <warp>");
        }

        manager.setWarp(args[0], player.getLocation());
        player.sendMessage(ChatColor.RED + "Warp set!");
    }
}
