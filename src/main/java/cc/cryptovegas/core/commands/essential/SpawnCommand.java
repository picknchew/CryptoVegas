package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.SpawnManager;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

public class SpawnCommand extends RDCommand {
    private final SpawnManager manager;

    public SpawnCommand(SpawnManager manager) {
        super("spawn");
        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        manager.teleportToSpawn(player);
        player.sendMessage(formatAt("spawn").get());
    }
}
