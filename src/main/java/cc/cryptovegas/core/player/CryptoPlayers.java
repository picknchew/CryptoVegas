package cc.cryptovegas.core.player;

import cc.cryptovegas.core.ClaimCooldownStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.inventivetalent.packetlistener.PacketListenerAPI;
import org.inventivetalent.packetlistener.handler.PacketHandler;
import org.inventivetalent.packetlistener.handler.ReceivedPacket;
import org.inventivetalent.packetlistener.handler.SentPacket;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.util.RunnableShorthand;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Injectable
public final class CryptoPlayers {
    private final Core core;
    private final Cache<UUID, CryptoPlayer> players = CacheBuilder.newBuilder().weakKeys().build();
    private final Coin realCoinsStartingBalance;
    private final Coin playCoinsStartingBalance;

    @InjectionProvider
    public CryptoPlayers(Core core) {
        this.core = core;
        this.realCoinsStartingBalance = Coin.parseCoin(core.getConfig().getString("real-coins-starting-balance"));
        this.playCoinsStartingBalance = Coin.parseCoin(core.getConfig().getString("play-coins-starting-balance"));
    }

    public void init() {
        PacketListenerAPI.addPacketHandler(new PacketHandler(core) {
            @Override
            public void onSend(SentPacket sentPacket) {
            }

            @Override
            public void onReceive(ReceivedPacket receivedPacket) {
                if (receivedPacket.getPacketName().equals("PacketPlayInTabComplete")) {
                    receivedPacket.setCancelled(true);
                }
            }
        });

        core.observeEvent(PlayerJoinEvent.class)
                .map(PlayerEvent::getPlayer)
                .filter(player -> core.getPlayer(player) == null)
                .doOnNext(player -> player.kickPlayer("The server is starting. Please try again in a few seconds."))
                .subscribe();

        core.observeEvent(InventoryClickEvent.class)
                .filter(event -> event.getInventory().getType() == InventoryType.PLAYER)
                .filter(event -> !event.getWhoClicked().isOp())
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> ((Player) event.getWhoClicked()).updateInventory())
                .subscribe();

        core.observeEvent(AsyncPlayerPreLoginEvent.class)
                .filter(event -> event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
                .map(AsyncPlayerPreLoginEvent::getUniqueId)
                .doOnNext(uuid -> {
                    try (Connection connection = core.getSQLConnection()) {
                        if (cachePlayer(connection, uuid)) {
                            loadStats(connection, players.getIfPresent(uuid));
                            return;
                        }

                        insertPlayer(connection, uuid);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                })
                .subscribe();

        core.observeEvent(PlayerJoinEvent.class)
                .filter(event -> core.getPlayer(event.getPlayer()) != null)
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    String ip = player.getAddress().getAddress().getHostAddress();

                    if (!ClaimCooldownStore.contains(player.getAddress().getAddress().getHostAddress())) {
                        RunnableShorthand.forPlugin(core).async().with(() -> {
                            try (Connection connection = core.getSQLConnection()) {
                                try (PreparedStatement statement = core.getStatement("get-ip", connection)) {
                                    statement.setString(1, ip);

                                    try (ResultSet resultSet = statement.executeQuery()) {
                                        if (resultSet.next()) {
                                            ClaimCooldownStore.addPlayer(ip, resultSet.getString("uuid"));
                                        } else {
                                            ClaimCooldownStore.addPlayer(ip, player.getUniqueId().toString());

                                            try (PreparedStatement statement1 = core.getStatement("insert-ip", connection)) {
                                                statement1.setString(1, ip);
                                                statement1.setString(2, player.getUniqueId().toString());
                                                statement1.execute();
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }).go();
                    }
                })
                .doOnNext(event -> {
                    if (get(event.getPlayer()).isVerified()) {
                        event.getPlayer().addAttachment(core).setPermission("deluxetags.tag.verified", true);
                    }
                })
                .doOnNext(event -> event.getPlayer().setGameMode(GameMode.CREATIVE))
                .subscribe();

        core.observeEvent(PlayerJoinEvent.class)
                .filter(event -> core.getPlayer(event.getPlayer()) != null)
                .doOnNext(event -> event.getPlayer().spigot().setCollidesWithEntities(false))
                .doOnNext(event -> core.getPlayer(event.getPlayer()).getStats().setLastJoin(Instant.now()))
                /*.doOnNext(event -> {
                    int protocolVersion = Via.getAPI().getPlayerVersion(event.getPlayer().getUniqueId());

                    RunnableShorthand.forPlugin(core).with(() -> {
                        if (protocolVersion <= 47) {
                            // 1.8 and below
                            event.getPlayer().setResourcePack("http://cryptovegas.cc/coins%201.8-below.zip");
                        } else if (protocolVersion <= 210) {
                            // 1.9 - 1.10
                            event.getPlayer().setResourcePack("http://cryptovegas.cc/coins%201.9-1.10.zip");
                        } else if (protocolVersion <= 340) {
                            // 1.11 - 1.12
                            event.getPlayer().setResourcePack("http://cryptovegas.cc/coins%201.11-1.12.zip");
                        }
                    }).later(1L);
                })*/
                .subscribe();

        core.observeEvent(PlayerQuitEvent.class)
                .filter(event -> core.getPlayer(event.getPlayer()) != null)
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    String ip = player.getAddress().getAddress().getHostAddress();

                    if (Bukkit.getOnlinePlayers().stream()
                            .map(Player::getAddress).map(InetSocketAddress::getAddress)
                            .map(InetAddress::getHostAddress)
                            .filter(address -> address.equals(ip))
                            .count() == 1) {
                        ClaimCooldownStore.invalidate(ip);
                    }
                })
                .doOnNext(event -> {
                    CryptoPlayer cryptoPlayer = core.getPlayer(event.getPlayer());
                    Duration timePlayed = cryptoPlayer.getStats().getTimePlayed();

                    cryptoPlayer.getStats().setTimePlayed(timePlayed);

                    RunnableShorthand.forPlugin(core).async().with(() -> {
                        try (Connection connection = core.getSQLConnection()) {
                            try (PreparedStatement statement = core.getStatement("update-time-played", connection)) {
                                statement.setInt(1, (int) timePlayed.getSeconds());
                                statement.setString(2, cryptoPlayer.getUniqueId().toString());
                                statement.execute();
                            }

                            try (PreparedStatement statement = core.getStatement("update-claim-cooldown", connection)) {
                                statement.setInt(1, (int) cryptoPlayer.getClaimCooldown().getSeconds());
                                statement.setString(2, cryptoPlayer.getUniqueId().toString());
                                statement.execute();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }).go();
                })
                .doOnNext(event -> players.invalidate(event.getPlayer().getUniqueId()))
                .subscribe();
    }

    private void insertPlayer(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = core.getStatement("insert-player", connection)) {
            statement.setString(1, uuid.toString());
            statement.setLong(2, realCoinsStartingBalance.getValue());
            statement.setLong(3, playCoinsStartingBalance.getValue());
            statement.execute();

            CryptoPlayer cryptoPlayer = new CryptoPlayer(uuid, Mode.PLAY_MONEY, realCoinsStartingBalance, playCoinsStartingBalance, Duration.ZERO, Instant.EPOCH, false);

            cryptoPlayer.getStats().setTimePlayed(Duration.ZERO);
            cryptoPlayer.getStats().setRealBetsMade(0);

            players.put(uuid, cryptoPlayer);
        }

        try (PreparedStatement statement = core.getStatement("insert-player-stats", connection)) {
            statement.setString(1, uuid.toString());
            statement.execute();
        }
    }

    private boolean cachePlayer(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = core.getStatement("get-player", connection)) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Mode mode = Mode.get(resultSet.getInt("mode"));
                    Coin realCoins = Coin.valueOf(resultSet.getLong("real_coins"));
                    Coin playCoins = Coin.valueOf(resultSet.getLong("play_coins"));
                    int claimCooldownSeconds = resultSet.getInt("claim_cooldown");
                    Duration claimCooldown = claimCooldownSeconds == 0 ? Duration.ZERO : Duration.ofSeconds(claimCooldownSeconds);
                    Timestamp timestampRefilled = resultSet.getTimestamp("last_refilled");
                    Instant lastRefilled = timestampRefilled == null ? Instant.EPOCH : timestampRefilled.toInstant();
                    Boolean verified = resultSet.getBoolean("verified");

                    CryptoPlayer cryptoPlayer = new CryptoPlayer(uuid, mode, realCoins, playCoins, claimCooldown, lastRefilled, verified);

                    players.put(uuid, cryptoPlayer);
                    return true;
                }
            }
        }

        return false;
    }

    private void loadStats(Connection connection, CryptoPlayer cryptoPlayer) throws SQLException {
        try (PreparedStatement statement = core.getStatement("get-player-stats", connection)) {
            statement.setString(1, cryptoPlayer.getUniqueId().toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    PlayerStats playerStats = cryptoPlayer.getStats();
                    int realBets = resultSet.getInt("real_bets");
                    int playTime = resultSet.getInt("play_time");

                    playerStats.setTimePlayed(Duration.ofSeconds(playTime));
                    playerStats.setRealBetsMade(realBets);
                }
            }
        }
    }

    public CryptoPlayer get(Player player) {
        return players.getIfPresent(player.getUniqueId());
    }
}
