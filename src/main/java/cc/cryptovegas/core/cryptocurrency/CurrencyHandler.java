package cc.cryptovegas.core.cryptocurrency;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.PlayerInventory;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ReadOnlyResource;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;
import tech.rayline.core.util.RunnableShorthand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Injectable
public class CurrencyHandler {
    private final Core core;
    private final Set<UseListener> listeners = new HashSet<>();

    @ResourceFile(raw = true, filename = "coinbase.yml")
    @ReadOnlyResource
    private YAMLConfigurationFile config;


    @InjectionProvider
    public CurrencyHandler(Core core) {
        this.core = core;

        core.getResourceFileGraph().addObject(this);

        registerListeners();
    }

    public void addUseListener(UseListener listener) {
        listeners.add(listener);
    }

    public boolean removeUseListener(UseListener listener) {
        return listeners.remove(listener);
    }

    public void setRealCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("update-real-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    public void setPlayCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("update-play-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    public void addRealCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("add-real-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    public void removeRealCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("remove-real-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    public void addPlayCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("add-play-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    public void removePlayCoins(String playerUUID, Coin amount) {
        RunnableShorthand.forPlugin(core).async().with(() -> {
            try (Connection connection = core.getSQLConnection()) {
                try (PreparedStatement statement = core.getStatement("remove-play-coins", connection)) {
                    statement.setLong(1, amount.getValue());
                    statement.setString(2, playerUUID);
                    statement.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).go();
    }

    private void registerListeners() {
        core.observeEvent(PlayerJoinEvent.class).doOnNext(event -> {
            Player player = event.getPlayer();
            PlayerInventory inventory = player.getInventory();

            inventory.clear();
            Arrays.stream(CurrencyItem.values()).forEach(currency -> inventory.setItem(currency.ordinal(), currency.getItem()));
            player.updateInventory();
        }).subscribe();

        core.observeEvent(EventPriority.HIGHEST, true, PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.LEFT_CLICK_BLOCK)
                .filter(event -> !core.isLockdown())
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    CurrencyItem currency = CurrencyItem.getByItem(player.getItemInHand());

                    if (currency != null) {
                        notifyListeners(core.getPlayer(player), currency, event.getClickedBlock());

                        event.setCancelled(true);
                    }
                }).subscribe();

        core.observeEvent(PlayerDropItemEvent.class).doOnNext(event -> event.setCancelled(true)).subscribe();
    }

    private void notifyListeners(CryptoPlayer player, CurrencyItem currency, Block rightClicked) {
        for (UseListener listener : listeners) {
            if (listener.onUse(player, currency, rightClicked)) {
                break;
            }
        }
    }

    public interface UseListener {
        boolean onUse(CryptoPlayer player, CurrencyItem currency, Block rightClicked);
    }
}
