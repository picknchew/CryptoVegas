package cc.cryptovegas.core.commands.essential;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

import java.time.Duration;
import java.time.Instant;

@CommandPermission("cryptovegas.uptime")
public class UptimeCommand extends RDCommand {
    private final Instant initInstant = Instant.now();

    public UptimeCommand() {
        super("uptime");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        Duration duration = Duration.between(initInstant, Instant.now());
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();

        player.sendMessage(ChatColor.RED + "Current uptime: " + hours + " hours, " + minutes + " minutes and " + seconds + " seconds.");
    }
}
