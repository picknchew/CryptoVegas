package cc.cryptovegas.core.commands.essential;

import cc.cryptovegas.core.SpawnManager;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.admin")
public class SetSpawnCommand extends RDCommand {
    private final SpawnManager manager;

    public SetSpawnCommand(SpawnManager manager) {
        super("setspawn");
        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        manager.setSpawn(player.getLocation());
        player.sendMessage(formatAt("spawn-set").get());
    }
}
