package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandMeta;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.setbal")
@CommandMeta(aliases={"setbal"})
public class SetBalanceCommand extends RDCommand {
    private Core core;
    private CurrencyHandler currencyHandler;

    public SetBalanceCommand() {
        super("setbalance");
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
        this.currencyHandler = core.getCurrencyHandler();
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch(NumberFormatException ex) {
            return false;
        }

        return true;
    }

    @Override
    protected void handleCommandUnspecific(CommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "/setbalance <player> <mode> <amount> <password>");
            return;
        }

        if (!args[3].equals("Crypto101")) {
            sender.sendMessage(ChatColor.RED + "The password was not correct.");
            return;
        }

        if (sender instanceof Player) {
            Player playerSender = (Player) sender;

            if (!AdminIPStore.contains(playerSender.getAddress().getHostString())) {
                sender.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }
        }

        Player player = Bukkit.getPlayer(args[0]);

        if (player == null) {
            sender.sendMessage(ChatColor.RED + "That was an invalid player!");
            return;
        }

        Mode mode;

        try {
            mode = Mode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid mode, either PLAY_MONEY OR REAL_MONEY.");
            return;
        }

        if (isNumeric(args[2])) {
            CryptoPlayer cryptoPlayer = core.getPlayer(player);

            if (mode == Mode.REAL_MONEY) {
                Coin coin = Coin.parseCoin(args[2]);

                sender.sendMessage(ChatColor.DARK_AQUA + "Successfully set " + player.getName() + "'s real money balance to " + args[2] + " BTC.");

                cryptoPlayer.setCoins(Mode.REAL_MONEY, coin);
                currencyHandler.setRealCoins(player.getUniqueId().toString(), coin);
                return;
            }

            Coin coin = Coin.parseCoin(args[2]);

            sender.sendMessage(ChatColor.DARK_AQUA + "Successfully set " + player.getName() + "'s play money balance to " + args[2] + " BTC.");

            cryptoPlayer.setCoins(Mode.PLAY_MONEY, coin);
            currencyHandler.setPlayCoins(player.getUniqueId().toString(), coin);
        }
    }
}
