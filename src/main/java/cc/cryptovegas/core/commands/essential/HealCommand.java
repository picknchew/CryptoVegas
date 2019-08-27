package cc.cryptovegas.core.commands.essential;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.heal")
public class HealCommand extends RDCommand {

    public HealCommand() {
        super("heal");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        player.sendMessage(ChatColor.YELLOW + "You were fully healed and your hunger was replenished.");
    }
}
