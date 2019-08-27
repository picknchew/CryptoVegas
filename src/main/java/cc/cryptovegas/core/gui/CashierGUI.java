package cc.cryptovegas.core.gui;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CoinbaseHandler;
import cc.cryptovegas.core.player.CryptoPlayer;
import com.google.common.collect.ImmutableList;
import me.picknchew.coinbase.commerce.model.Charge;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tech.rayline.core.gui.InventoryGUI;
import tech.rayline.core.gui.SimpleInventoryGUIButton;
import tech.rayline.core.util.ItemShorthand;
import tech.rayline.core.util.RunnableShorthand;

import java.io.IOException;

public class CashierGUI {
    private static final String INVENTORY_TITLE = ChatColor.GREEN.toString() + ChatColor.BOLD + "Cashier";
    private static final int INVENTORY_SIZE = 27;

    private final Core core;
    private final InventoryGUI gui;
    private final CryptoPlayer cryptoPlayer;
    private final CoinbaseHandler coinbaseHandler;

    public CashierGUI(Core core, CoinbaseHandler coinbaseHandler, Player player) {
        this.core = core;
        this.gui = new InventoryGUI(core, INVENTORY_SIZE, INVENTORY_TITLE);
        this.coinbaseHandler = coinbaseHandler;
        this.cryptoPlayer = core.getPlayer(player);

        populateInventory();
        gui.updateInventory();
    }

    protected void populateInventory() {
        ItemShorthand deposit = ItemShorthand.withMaterial(Material.BOOK).setName(ChatColor.GREEN + ChatColor.BOLD.toString() + "DEPOSIT");

        // deposit
        gui.setButton(new SimpleInventoryGUIButton(deposit.get(), action -> {
            Player player = action.getPlayer();

            gui.closeFor(player);
            player.sendMessage(ChatColor.RED + "Please wait while we generate an address.");

            RunnableShorthand.forPlugin(core).async().with(() -> {
                try {
                    Charge charge = coinbaseHandler.createCharge(player.getUniqueId().toString());
                    TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&',
                            core.getFormatsFile().getConfig().getString("deposit")));

                    if (charge != null) {
                        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, charge.getHostedUrl()));
                        player.spigot().sendMessage(component);
                        return;
                    }

                    player.sendMessage(ChatColor.RED + "There are too many requests at the moment! Please try again in a few seconds.");
                } catch (IOException e) {
                    player.sendMessage(ChatColor.RED + "Failed to create a transaction. Please try again later.");
                }
            }).go();
        }), 10);

        ItemShorthand balance = ItemShorthand.withMaterial(Material.PAPER)
                .setName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Balance")
                .withLore(ImmutableList.of(ChatColor.DARK_AQUA + "Real coins: " + ChatColor.RESET + cryptoPlayer.getCoins(Mode.REAL_MONEY).toFriendlyString(),
                        ChatColor.DARK_AQUA + "Play coins: " + ChatColor.RESET + cryptoPlayer.getCoins(Mode.PLAY_MONEY).toFriendlyString()));

        // balance
        gui.setButton(new SimpleInventoryGUIButton(balance.get(), action -> {}), 13);

        ItemShorthand withdraw = ItemShorthand.withMaterial(Material.BOOK).setName(ChatColor.RED + ChatColor.BOLD.toString() + "WITHDRAW");

        // withdraw
        gui.setButton(new SimpleInventoryGUIButton(withdraw.get(), action -> {
            Player player = action.getPlayer();

            gui.closeFor(player);

            if (core.getPlayer(player).getStats().getRealBetsMade() < 10) {
                player.sendMessage(ChatColor.RED + "You must make at least 10 bets before withdrawing.");
                return;
            }

            player.sendMessage(core.formatAt("withdraw").get());
        }), 16);

        gui.updateInventory();
    }

    public void openFor(Player player) {
        gui.openFor(player);
    }
}
