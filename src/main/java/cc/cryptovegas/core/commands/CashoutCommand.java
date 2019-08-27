package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.game.GameState;
import cc.cryptovegas.core.game.minefield.MineFieldGame;
import cc.cryptovegas.core.game.minefield.MineFieldManager;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

import java.util.Optional;

public class CashoutCommand extends RDCommand {
    private Core core;
    private CurrencyHandler currencyHandler;
    private final MineFieldManager manager;

    public CashoutCommand(MineFieldManager manager) {
        super("cashout");

        this.manager = manager;
    }

    @Override
    public void onCommandRegister() {
        core = (Core) getPlugin();
        currencyHandler = core.getCurrencyHandler();
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        Optional<MineFieldGame> optionalGame = manager.getGameByPlayer(player);

        if (!((Core) getPlugin()).getPlayer(player).isInGame() || !optionalGame.isPresent()) {
            player.sendMessage(ChatColor.RED + "You do not have any money to claim.");
            return;
        }

        MineFieldGame game = optionalGame.get();

        if (game.getState() == GameState.PLAYING) {
            if (game.getCurrentFound() == 0) {
                player.sendMessage(ChatColor.RED + "You do not have any money to claim.");
                return;
            }

            CryptoPlayer cryptoPlayer = core.getPlayer(player);

            CustomSound.DING.playFor(cryptoPlayer);
            cryptoPlayer.sendMessage(ChatColor.GREEN + "You have claimed " + Coin.valueOf(game.getCurrentFound()).toFriendlyString());

            Coin payout = Coin.valueOf(game.getCurrentFound());
            Mode mode = game.getCoinMode();

            if (mode == Mode.REAL_MONEY) {
                currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
            } else {
                currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
            }

            cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));

            game.endGame(false);
        }
    }
}
