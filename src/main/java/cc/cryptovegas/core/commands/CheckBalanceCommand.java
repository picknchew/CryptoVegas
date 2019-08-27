package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandMeta(aliases={"checkbal"})
@CommandPermission("cryptovegas.checkbal")
public class CheckBalanceCommand extends RDCommand {
    private Core core;

    public CheckBalanceCommand() {
        super("checkbalance");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
    }

    @Override
    protected void handleCommandUnspecific(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "/checkbal <player>");
            return;
        }

        Player player = Bukkit.getPlayer(args[0]);

        if (player == null) {
            sender.sendMessage(formatAt("check-balance-offline").with("player", args[0]).get());
            return;
        }

        CryptoPlayer cryptoPlayer = core.getPlayer(player);

        sender.sendMessage(ChatColor.GOLD + player.getName() + "'s balance:\n" +
                ChatColor.RED + "Real Money: " + ChatColor.WHITE + cryptoPlayer.getCoins(Mode.REAL_MONEY).toFriendlyString() + "\n" +
                ChatColor.RED + "Play Money: " + ChatColor.WHITE + cryptoPlayer.getCoins(Mode.PLAY_MONEY).toFriendlyString());
    }
}
