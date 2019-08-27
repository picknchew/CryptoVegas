package cc.cryptovegas.core.gui;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyItem;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.gui.SimpleInventoryGUIButton;

public class CoinGUI extends GUI {
    private static final String INVENTORY_TITLE = ChatColor.YELLOW + ChatColor.BOLD.toString() + "Select your bet";
    private static final int INVENTORY_SIZE = 9;

    private final Listener listener;

    public CoinGUI(Core core, Listener listener) {
        super(core, INVENTORY_TITLE, INVENTORY_SIZE);

        this.listener = listener;
    }

    @Override
    protected void populateInventory() {
        for (int slot = 0; slot < CurrencyItem.values().length; slot++) {
            CurrencyItem item = CurrencyItem.values()[slot];

            gui.setButton(new SimpleInventoryGUIButton(item.getItem(), action -> {
                Player player = action.getPlayer();
                CryptoPlayer cryptoPlayer = core.getPlayer(player);
                Mode mode = cryptoPlayer.getMode();

                gui.closeFor(player);

                if (!cryptoPlayer.hasEnoughCoins(item.getValue(), mode)) {
                    cryptoPlayer.sendMessage(ChatColor.RED + "You do not have enough coins!");
                    return;
                }

                listener.onCoinSelect(item.getValue(), mode);
            }), slot);
        }

        gui.updateInventory();
    }

    public interface Listener {
        void onCoinSelect(Coin coin, Mode mode);
    }
}
