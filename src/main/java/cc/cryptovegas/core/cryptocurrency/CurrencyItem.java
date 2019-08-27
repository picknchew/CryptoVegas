package cc.cryptovegas.core.cryptocurrency;

import cc.cryptovegas.core.cryptocurrency.model.Coin;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Dye;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum CurrencyItem {
    // 1 mBTC = 0.001 BTC
    TWO_HUNDREDTH_MBTC("0.00002", DyeColor.BLACK, "&b0.02 mBTC"),
    FIVE_HUNDREDTH_MBTC("0.00005", DyeColor.BLUE, "&10.05 mBTC"),
    ONE_TENTH_MBTC("0.0001", DyeColor.BROWN, "&60.1 mBTC"),
    TWO_TENTH_MBTC("0.0002", DyeColor.CYAN, "&30.2 mBTC"),
    ONE_HALF_MBTC("0.0001", DyeColor.GRAY, "&70.5 mBTC"),
    ONE_MBTC("0.001", DyeColor.MAGENTA, "&61 mBTC"),
    ONE_AND_ONE_HALF_MBTC("0.0015", DyeColor.LIGHT_BLUE, "&91.5 mBTC"),
    THREE_MBTC("0.003", DyeColor.LIME, "&a3 mBTC"),
    TEN_MBTC("0.01", DyeColor.GREEN, "&c10 mBTC");

    private static final Map<ItemStack, CurrencyItem> items = new HashMap<>();

    static {
        // populate lookup map
        for (CurrencyItem currency : CurrencyItem.values()) {
            items.put(currency.getItem(), currency);
        }
    }

    private final ItemStack item;
    private final Coin value;

    CurrencyItem(String value, DyeColor color, String name, String... lore) {
        this.value = Coin.parseCoin(value);

        Dye dye = new Dye();
        dye.setColor(color);

        ItemStack item = dye.toItemStack(1);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);

        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public Coin getValue() {
        return value;
    }

    public static CurrencyItem getByItem(ItemStack item) {
        return items.get(item);
    }
}
