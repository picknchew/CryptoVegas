package cc.cryptovegas.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.rank")
public class RankCommand extends RDCommand {

    public RankCommand() {
        super("rank");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommandUnspecific(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/rank <player> <rank>");
            return;
        }

        ConsoleCommandSender commandSender = Bukkit.getConsoleSender();

        Bukkit.dispatchCommand(commandSender, "permissions player " + args[0] + " purge");
        Bukkit.dispatchCommand(commandSender, "setrank " + args[0] + " " + args[1]);

        sender.sendMessage(ChatColor.GREEN + "Commands executed.");
    }
}
