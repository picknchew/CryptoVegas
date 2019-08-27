package cc.cryptovegas.core.gui;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.CountdownTimer;
import cc.cryptovegas.core.util.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.rayline.core.gui.SimpleInventoryGUIButton;
import tech.rayline.core.util.ItemShorthand;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BartenderGUI extends GUI {
    private static final String INVENTORY_TITLE = ChatColor.YELLOW + ChatColor.BOLD.toString() + "Bartender";
    private static final int INVENTORY_SIZE = 9;

    private static final Set<CryptoPlayer> players = new HashSet<>();

    private final CurrencyHandler currencyHandler;

    public BartenderGUI(Core core) {
        super(core, INVENTORY_TITLE, INVENTORY_SIZE);

        this.currencyHandler = core.getCurrencyHandler();
    }

    @Override
    protected void populateInventory() {
        for (int x = 0; x < Drink.values().length; x ++) {
            Drink drink = Drink.values()[x];

            gui.setButton(new SimpleInventoryGUIButton(drink.item, action -> {
                Player player = action.getPlayer();
                CryptoPlayer cryptoPlayer = core.getPlayer(player);

                if (players.contains(cryptoPlayer)) {
                    cryptoPlayer.sendMessage(ChatColor.RED + "You can only drink one drink at a time!");
                }

                gui.closeFor(player);

                new CoinGUI(core, (coin, mode) -> {
                    int num = ThreadLocalRandom.current().nextInt(0, 100);

                    if (mode == Mode.REAL_MONEY) {
                        currencyHandler.removeRealCoins(cryptoPlayer.getUniqueId().toString(), coin);
                        PlayerUtil.incrRealBets(core.getPlayer(player), core);
                    } else {
                        currencyHandler.removePlayCoins(cryptoPlayer.getUniqueId().toString(), coin);
                    }

                    cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).subtract(coin));

                    cryptoPlayer.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Drinking in...");
                    players.add(cryptoPlayer);

                    CountdownTimer.create(core, 4L)
                            .forEach(count -> {
                                cryptoPlayer.sendMessage(ChatColor.YELLOW + Long.toString(count + 1));

                                if (count == 0) {
                                    CustomSound.DRINK.playFor(cryptoPlayer);
                                }
                            })
                            .onFinish(() -> {
                                if (mode == Mode.REAL_MONEY ? num < drink.chance : num < drink.chance + 5) {
                                    Coin payout = Coin.valueOf((long) (coin.getValue() * drink.multiplier));

                                    CustomSound.DING.playFor(cryptoPlayer);
                                    cryptoPlayer.sendMessage(core.formatAt("game-win").with("coin", coin.toFriendlyString()).get());

                                    if (mode == Mode.REAL_MONEY) {
                                        currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
                                    } else {
                                        currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
                                    }

                                    cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));

                                    return;
                                }

                                cryptoPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 3));
                                cryptoPlayer.sendMessage(ChatColor.RED + "You drank and became drowsy.");
                                players.remove(cryptoPlayer);
                            }).start();
                }).open(player);


            }), drink.slot);
        }

        gui.updateInventory();
    }

    public enum Drink {
        BEER(2, ItemShorthand.withMaterial(Material.POTION).withName("&4&lBeer").setLore("&b60% chance of winning 1.5x your money!").get(), 1.5D, 60),
        WHISKY(3, ItemShorthand.withMaterial(Material.POTION).withName("&6&lWhisky").setLore("&b45% chance of winning 2x your money!").get(), 2D, 45),
        WINE(5, ItemShorthand.withMaterial(Material.POTION).withName("&5&lWine").setLore("&b30% chance of winning 3x your money!").get(), 3D, 30),
        LIQUOR(6, ItemShorthand.withMaterial(Material.POTION).withName("&d&lLiquor").setLore("&b15% chance of winning 5x your money!").get(), 5D, 15);

        private final int slot;
        private final ItemStack item;
        private final double multiplier;
        private final int chance;

        Drink(int slot, ItemStack item, double multiplier, int chance) {
            this.slot = slot;
            this.item = item;
            this.multiplier = multiplier;
            this.chance = chance;

            ItemMeta meta = item.getItemMeta();

            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            item.setItemMeta(meta);
        }
    }
}
