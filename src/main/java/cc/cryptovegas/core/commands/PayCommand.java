package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

public class PayCommand extends RDCommand {
    private Core core;
    private CurrencyHandler currencyHandler;

    public PayCommand() {
        super("pay");
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
        this.currencyHandler = core.getCurrencyHandler();
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        CryptoPlayer cryptoPlayer = core.getPlayer(player);

        if (!cryptoPlayer.isVerified() && !player.hasPermission("cryptovegas.pay.bypass")) {
            player.sendMessage(ChatColor.RED + "You must be verified to use this command!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "/pay <play|real> <player> <amount>");
            return;
        }

        Mode mode;

        switch (args[0].toLowerCase()) {
            case "real":
                mode = Mode.REAL_MONEY;
                break;
            case "play":
                mode = Mode.PLAY_MONEY;
                break;
            default:
                player.sendMessage(ChatColor.RED + "That was an invalid mode, use 'real' or 'play'.");
                return;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "That was an invalid player!");
            return;
        }

        if (target == player) {
            player.sendMessage(ChatColor.RED + "You cannot send money to yourself!");
            return;
        }

        try {
            Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Please enter a valid amount!");
            return;
        }

        Coin amount = Coin.parseCoin(args[2]);

        if (cryptoPlayer.getCoins(mode).isLessThan(amount)) {
            player.sendMessage(ChatColor.RED + "You do not have enough " + mode + " coins.");
            return;
        }

        CryptoPlayer cryptoTarget = core.getPlayer(target);

        player.sendMessage(ChatColor.GREEN + "You sent " + amount.toFriendlyString() + " of " + mode + " coins to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You received " + amount.toFriendlyString() + " of " + mode + " coins from " + player.getName() + ".");

        cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).subtract(amount));
        cryptoTarget.setCoins(mode, cryptoTarget.getCoins(mode).add(amount));

        if (mode == Mode.PLAY_MONEY) {
            currencyHandler.removePlayCoins(cryptoPlayer.getUniqueId().toString(), amount);
            currencyHandler.addPlayCoins(cryptoTarget.getUniqueId().toString(), amount);
        } else {
            currencyHandler.removeRealCoins(cryptoPlayer.getUniqueId().toString(), amount);
            currencyHandler.addRealCoins(cryptoTarget.getUniqueId().toString(), amount);
        }
    }
}
