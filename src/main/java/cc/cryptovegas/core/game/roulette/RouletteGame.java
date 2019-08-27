package cc.cryptovegas.core.game.roulette;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.game.GameState;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.BlockUtil;
import cc.cryptovegas.core.util.CountdownTimer;
import cc.cryptovegas.core.util.PlayerUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import rx.Subscription;
import tech.rayline.core.util.RunnableShorthand;

import java.security.SecureRandom;
import java.util.*;

public class RouletteGame {
    private static final ThreadLocal<SecureRandom> random = ThreadLocal.withInitial(SecureRandom::new);

    private final CurrencyHandler currencyHandler;
    private final Block sign;
    private final Map<Block, Number> numbers;
    private final Table<Number, CryptoPlayer, Bet> bets = HashBasedTable.create();

    private List<Number> numberList;

    private final Map<CryptoPlayer, BukkitTask> startedPlaying = new HashMap<>();
    private GameState state = GameState.WAITING;

    private final Set<Subscription> subscriptions = new HashSet<>();
    private final CurrencyHandler.UseListener useListener;

    public RouletteGame(Core core, Block sign, Map<Block, Number> numbers) {
        this.currencyHandler = core.getCurrencyHandler();
        this.sign = sign;
        this.numbers = numbers;
        this.numberList = new ArrayList<>(numbers.values());

        subscriptions.add(core.observeEvent(EventPriority.HIGH, true, BlockBreakEvent.class)
                .filter(event -> numbers.containsKey(event.getBlock()) || event.getBlock().equals(sign))
                .doOnNext(event -> event.setCancelled(true))
                .subscribe());

        subscriptions.add(core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> numbers.containsKey(event.getClickedBlock()))
                .filter(event -> !startedPlaying.containsKey(core.getPlayer(event.getPlayer())))
                .filter(event -> !bets.containsColumn(event.getPlayer()))
                .filter(event -> !core.getPlayer(event.getPlayer()).isInGame())
                .filter(event -> state == GameState.WAITING)
                .doOnNext(event -> {
                            Player player = event.getPlayer();
                            CryptoPlayer cryptoPlayer = core.getPlayer(player);

                            player.sendMessage(ChatColor.YELLOW + "You started playing roulette! Left-click to start betting.");
                            cryptoPlayer.setInGame(true);

                            BukkitTask task = RunnableShorthand.forPlugin(core).with(() -> {
                                if (startedPlaying.containsKey(cryptoPlayer)) {
                                    player.sendMessage(ChatColor.RED + "You stopped playing roulette because you did not bet in time.");
                                    startedPlaying.remove(cryptoPlayer).cancel();
                                    cryptoPlayer.setInGame(false);
                                }
                            }).later(300L);

                            startedPlaying.put(cryptoPlayer, task);

                            event.setCancelled(true);
                        }
                ).subscribe());

        core.getCurrencyHandler().addUseListener(useListener = (cryptoPlayer, currency, block) -> {
            if (numbers.containsKey(block)) {
                if (state != GameState.WAITING) {
                    return true;
                }

                if (!startedPlaying.containsKey(cryptoPlayer) && !bets.containsColumn(cryptoPlayer.getPlayer())) {
                    startedPlaying.remove(cryptoPlayer).cancel();
                    return true;
                }

                Number number = numbers.get(block);

                if (bets.contains(number, cryptoPlayer)) {
                    Coin bet = currency.getValue();
                    Mode mode = cryptoPlayer.getMode();

                    if (!cryptoPlayer.hasEnoughCoins(bet, mode)) {
                        cryptoPlayer.sendMessage(ChatColor.RED + "You do not have enough coins!");
                        return true;
                    }

                    if (mode == Mode.REAL_MONEY) {
                        currencyHandler.removeRealCoins(cryptoPlayer.getUniqueId().toString(), bet);
                    } else {
                        currencyHandler.removePlayCoins(cryptoPlayer.getUniqueId().toString(), bet);
                    }

                    cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).subtract(bet));

                    Bet totalBet = bets.get(number, cryptoPlayer);
                    totalBet.add(bet);
                    cryptoPlayer.sendMessage(ChatColor.GREEN + "You added to your bet, you now have a bet of " + totalBet.coin.toFriendlyString() + " on " + number.getColor().chatColor + number.getNumber());
                    return true;
                }

                Coin bet = currency.getValue();
                Mode mode = cryptoPlayer.getMode();

                if (!cryptoPlayer.hasEnoughCoins(bet, mode)) {
                    cryptoPlayer.sendMessage(ChatColor.RED + "You do not have enough coins!");
                    return true;
                }

                if (mode == Mode.REAL_MONEY) {
                    currencyHandler.removeRealCoins(cryptoPlayer.getUniqueId().toString(), bet);
                } else {
                    currencyHandler.removePlayCoins(cryptoPlayer.getUniqueId().toString(), bet);
                }

                cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).subtract(bet));

                bets.put(number, cryptoPlayer, new Bet(bet));
                cryptoPlayer.setInGame(true);
                cryptoPlayer.sendMessage(ChatColor.GREEN + "You bet " + bet.toFriendlyString() + " on " + number.getColor().chatColor + number.getNumber());
                return true;
            }

            return false;
        });

        CountdownTimer timer = CountdownTimer.create(core, 30L);

        timer.forEach(count -> BlockUtil.updateSign((Sign) sign.getState(), null, Long.toString(count) + " seconds", "remaining", null));

        timer.onFinish(() -> {
            Number number = pickRandomNumber();

            startedPlaying.forEach((cryptoPlayer, task) -> {
                task.cancel();
                cryptoPlayer.getPlayer().sendMessage(ChatColor.RED + "You stopped playing roulette because you did not bet in time.");
                cryptoPlayer.setInGame(false);
            });

            bets.columnKeySet().stream().distinct().forEach(cryptoPlayer -> {
                cryptoPlayer.setInGame(false);
                PlayerUtil.incrRealBets(cryptoPlayer, core);
            });

            sendMessageToPlayers(ChatColor.GREEN + "The number chosen was " + number.getColor().chatColor + number.getNumber());
            rewardPlayers(number);
            resetGame();

            timer.reset();
            timer.start();
        });

        timer.start();
    }

    public void sendMessageToPlayers(String message) {
        bets.columnKeySet().forEach(cryptoPlayer -> cryptoPlayer.sendMessage(message));
    }

    private void resetGame() {
        state = GameState.WAITING;
        bets.clear();
    }

    private void rewardPlayers(Number number) {
        bets.row(number).forEach((cryptoPlayer, bet) -> {
            CustomSound.DING.playFor(cryptoPlayer);
            // TODO: give currency.
            // cryptoPlayer.sendMessage(core.formatAt("game-win").with("coin", payout.toFriendlyString()).get());
        });
    }

    public Block getSign() {
        return sign;
    }

    public boolean isBlockUsed(Block block) {
        return numbers.containsKey(block);
    }

    public void removeBlock(Block block) {
        numbers.remove(block);
    }

    public void addNumber(Block block, Number number) {
        numbers.put(block, number);
    }

    public Map<Block, Number> getNumbers() {
        return numbers;
    }

    private Number pickRandomNumber() {
        return numberList.get(random.get().nextInt(numbers.size()));
    }

    public void delete() {
        subscriptions.forEach(Subscription::unsubscribe);
        currencyHandler.removeUseListener(useListener);
    }

    public class Bet {
        private Coin coin;

        public Bet(Coin coin) {
            this.coin = coin;
        }

        public void add(Coin coin) {
            this.coin = this.coin.add(coin);
        }
    }

    public enum Color {
        RED(ChatColor.RED),
        BLACK(ChatColor.BLACK),
        GREEN(ChatColor.GREEN);

        private final ChatColor chatColor;

        Color(ChatColor chatColor) {
            this.chatColor = chatColor;
        }
    }
}
