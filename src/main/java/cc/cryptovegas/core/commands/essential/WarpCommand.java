package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.WarpManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.RDCommand;

import java.util.Map;

@CommandMeta(aliases = {"warps"})
public class WarpCommand extends RDCommand {
    private final WarpManager manager;

    public WarpCommand(WarpManager manager) {
        super("warp");

        this.manager = manager;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        if (args.length == 0) {
            player.sendMessage(formatAt("warps").with("warps", manager.getWarps()).get());
            return;
        }

        Map.Entry<String, Location> warp = manager.getWarp(args[0]);

        if (warp == null) {
            player.sendMessage(formatAt("warp-not-found").get());
            return;
        }

        player.teleport(warp.getValue());
        player.sendMessage(formatAt("warp").with("warp", warp.getKey()).get());
    }
}
