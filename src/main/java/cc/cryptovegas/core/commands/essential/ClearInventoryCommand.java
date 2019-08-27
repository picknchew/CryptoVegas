package cc.cryptovegas.core.commands.essential;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.clearinventory")
@CommandMeta(aliases={"ci"})
public class ClearInventoryCommand extends RDCommand {

    public ClearInventoryCommand() {
        super("clearinventory");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.getInventory().clear();
        player.sendMessage(ChatColor.GREEN + "Inventory cleared!");
    }
}
