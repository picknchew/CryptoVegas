package cc.cryptovegas.core.game.blackjack;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.game.GameState;
import cc.cryptovegas.core.game.blackjack.card.Card;
import cc.cryptovegas.core.game.blackjack.card.Deck;
import cc.cryptovegas.core.game.blackjack.card.Rank;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.BlockUtil;
import cc.cryptovegas.core.util.CountdownTimer;
import cc.cryptovegas.core.util.HashingUtil;
import cc.cryptovegas.core.util.PlayerUtil;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import rx.Subscription;
import tech.rayline.core.util.RunnableShorthand;

import java.util.*;
import java.util.stream.Collectors;

public class BlackJackGame {
    private final Core core;
    private final CurrencyHandler currencyHandler;

    private Deck deck = new Deck();

    private final Block dealerSign;

    private final Set<Block> signs;

    private final Map<CryptoPlayer, Block> players = new HashMap<>();
    private final Map<CryptoPlayer, List<Card>> playerCards = new HashMap<>();
    private final Map<CryptoPlayer, Coin> bets = new HashMap<>();
    private final Map<CryptoPlayer, Mode> modes = new HashMap<>();

    private final List<CryptoPlayer> complete = new ArrayList<>();

    private final List<Card> dealerCards = new ArrayList<>();
    private final Map<CryptoPlayer, BukkitTask> startedPlaying = new HashMap<>();

    private GameState state = GameState.WAITING;

    private final Set<Subscription> subscriptions = new HashSet<>();
    private final CurrencyHandler.UseListener useListener;

    // provably fair
    private String key;
    private String hash;
    private String unhashed;

    public BlackJackGame(Core core, Block dealerSign) {
        this(core, dealerSign, new HashSet<>());
    }

    public BlackJackGame(Core core, Block dealerSign, Set<Block> signs) {
        this.core = core;
        this.currencyHandler = core.getCurrencyHandler();
        this.dealerSign = dealerSign;
        this.signs = signs;

        subscriptions.add(core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> signs.contains(event.getClickedBlock()))
                .filter(event -> !startedPlaying.containsKey(core.getPlayer(event.getPlayer())))
                .filter(event -> !core.getPlayer(event.getPlayer()).isInGame())
                .filter(event -> state == GameState.WAITING || state == GameState.STARTING)
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    CryptoPlayer cryptoPlayer = core.getPlayer(player);

                    cryptoPlayer.sendMessage(ChatColor.YELLOW + "You started playing black jack! Left-click to start betting.");
                    cryptoPlayer.setInGame(true);

                    BukkitTask task = RunnableShorthand.forPlugin(core).with(() -> {
                        if (startedPlaying.containsKey(cryptoPlayer)) {
                            cryptoPlayer.sendMessage(ChatColor.RED + "You stopped playing black jack because you did not bet in time.");
                            startedPlaying.remove(cryptoPlayer).cancel();
                            cryptoPlayer.setInGame(false);
                        }
                    }).later(300L);

                    startedPlaying.put(cryptoPlayer, task);

                    event.setCancelled(true);
                }
        ).subscribe());

        core.getCurrencyHandler().addUseListener(useListener = (cryptoPlayer, currency, block) -> {
            if (!signs.contains(block)) {
                return false;
            }

            if (state != GameState.WAITING && state != GameState.STARTING) {
                return true;
            }

            // check if player is clicking their sign again to add more to their bet.
            if (players.containsKey(cryptoPlayer) && players.get(cryptoPlayer).equals(block)) {
                Coin bet = currency.getValue();
                Coin totalBet = bets.get(cryptoPlayer).add(bet);
                Mode mode = modes.get(cryptoPlayer);

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
                bets.put(cryptoPlayer, totalBet);
                modes.put(cryptoPlayer, mode);
                updateSign(cryptoPlayer, null, totalBet.toFriendlyString());

                return true;
            }

            // player is already playing or someone else is occupying this spot.
            if (players.containsValue(block)) {
                return true;
            }

            if (!startedPlaying.containsKey(cryptoPlayer)) {
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

            startedPlaying.remove(cryptoPlayer).cancel();
            addPlayer(cryptoPlayer, block, currency.getValue());
            modes.put(cryptoPlayer, mode);

            if (players.size() == 4) {
                startedPlaying.forEach((startedPlayer, task) -> {
                    task.cancel();
                    startedPlayer.sendMessage(ChatColor.RED + "You stopped playing black jack because you did not bet in time.");
                    cryptoPlayer.setInGame(false);
                });

                startedPlaying.clear();
            }

            if (state == GameState.WAITING) {
                start();
            }

            return true;
        });

        subscriptions.add(core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)
                .filter(event -> state == GameState.PLAYING)
                .filter(event -> !complete.contains(core.getPlayer(event.getPlayer())))
                .doOnNext(event -> {
                    CryptoPlayer cryptoPlayer = core.getPlayer(event.getPlayer());

                    if (!players.containsKey(cryptoPlayer)) {
                        return;
                    }

                    Block sign = players.get(cryptoPlayer);

                    if (!sign.equals(event.getClickedBlock())) {
                        return;
                    }

                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        complete.add(cryptoPlayer);
                        updateSign(cryptoPlayer, "Stand");
                        return;
                    }

                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        addCards(cryptoPlayer, 1);
                    }
                }).subscribe());

        subscriptions.add(core.observeEvent(BlockBreakEvent.class)
                .filter(event -> {
                    Block block = event.getBlock();

                    return signs.contains(block) || dealerSign.equals(block);
                })
                .doOnNext(event -> event.setCancelled(true))
                .subscribe());

        resetSigns();
    }

    public void addSign(Block sign) {
        signs.add(sign);
        resetSigns();
    }

    public void removeSign(Block sign) {
        signs.remove(sign);
    }

    public boolean isDealerSign(Block sign) {
        return sign.equals(dealerSign);
    }

    public boolean isSignUsed(Block sign) {
        return signs.contains(sign) || sign.equals(dealerSign);
    }

    public Block getDealerSign() {
        return dealerSign;
    }

    public Set<Block> getSigns() {
        return signs;
    }

    private void start() {
        state = GameState.STARTING;

        key = HashingUtil.generateRandomKey();
        unhashed = UUID.randomUUID().toString().replace("-", "") + ":" + deck.toString();
        hash = HashingUtil.hashString(unhashed, key);

        CountdownTimer.create(core, 10L).forEach((durationRemaining) -> {
            players.keySet().forEach(cryptoPlayer -> updateSign(cryptoPlayer, null, null, null, durationRemaining + " seconds"));
        }).onFinish(() -> {
            signs.stream().filter(sign -> !players.containsValue(sign)).forEach(sign -> BlockUtil.clearSign((Sign) sign.getState()));

            startedPlaying.forEach((cryptoPlayer, task) -> {
                task.cancel();
                cryptoPlayer.sendMessage(ChatColor.RED + "You stopped playing black jack because you did not bet in time.");
                cryptoPlayer.setInGame(false);
            });

            startedPlaying.clear();
            players.keySet().forEach((cryptoPlayer) -> {
                cryptoPlayer.sendMessage(ChatColor.RED + "Deck hash: " + ChatColor.YELLOW + hash);
                addCards(cryptoPlayer, 1);
            });

            dealerCards.add(deck.getCard());

            String[] lines = formatCards(dealerCards);

            lines[0] = ChatColor.RED + "Dealer";
            lines[3] = Integer.toString(getTotalValueOfCards(dealerCards));

            updateDealerSign(lines);

            state = GameState.PLAYING;

            run();
        }).start();
    }

    private void run() {
        CountdownTimer timer = CountdownTimer.create(core, 30L);

        playerCards.forEach((cryptoPlayer, cards) -> updateSign(cryptoPlayer, null, null, null, Integer.toString(getTotalValueOfCards(cards))));

        timer.forEach((durationRemaining) -> {
            players.keySet().stream()
                    .filter(cryptoPlayer -> !complete.contains(cryptoPlayer))
                    .forEach(cryptoPlayer -> updateSign(cryptoPlayer, ChatColor.RED + Long.toString(durationRemaining) + " seconds"));

            if (complete.containsAll(players.keySet())) {
                timer.cancel();
            }
        }).onFinish(() -> {
            Set<CryptoPlayer> incomplete = new HashSet<>(players.keySet());

            incomplete.removeAll(complete);
            incomplete.forEach((cryptoPlayer) -> updateSign(cryptoPlayer, "Stand"));

            finishGame();
        }).start();
    }

    private void finishGame() {
        int dealerValue;

        while ((dealerValue = getTotalValueOfCards(dealerCards)) < 17) {
            dealerCards.add(deck.getCard());
        }

        String[] lines = formatCards(dealerCards);

        lines[0] = "Dealer";
        lines[3] = Integer.toString(getTotalValueOfCards(dealerCards));

        updateDealerSign(lines);

        BaseComponent[] component = new ComponentBuilder("Provably Fair: ")
                .color(net.md_5.bungee.api.ChatColor.RED)
                .append("Click to open the url.")
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://verify.cryptovegas.cc/provably-fair/blackjack/" + key + "/" + unhashed + "/" + hash))
                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                .create();

        for (Map.Entry<CryptoPlayer, List<Card>> entry : playerCards.entrySet()) {
            int playerValue = getTotalValueOfCards(entry.getValue());
            CryptoPlayer cryptoPlayer = entry.getKey();
            Mode mode = modes.get(cryptoPlayer);

            cryptoPlayer.sendMessage(component);

            if (mode == Mode.REAL_MONEY) {
                PlayerUtil.incrRealBets(cryptoPlayer, core);
            }

            if ((playerValue < dealerValue && dealerValue <= 21) || playerValue > 21) {
                cryptoPlayer.sendMessage(core.formatAt("game-lose").get());
            } else if (playerValue == dealerValue) {
                cryptoPlayer.sendMessage(core.formatAt("game-tie").get());

                Coin payout = bets.get(cryptoPlayer);

                if (mode == Mode.REAL_MONEY) {
                    currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
                } else {
                    currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
                }

                cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));
            } else {
                CustomSound.DING.playFor(cryptoPlayer);

                Coin payout = bets.get(cryptoPlayer).multiply(2L);

                cryptoPlayer.sendMessage(core.formatAt("game-win").with("coin", payout.toFriendlyString()).get());

                if (mode == Mode.REAL_MONEY) {
                    currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
                } else {
                    currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
                }

                cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));
            }

            cryptoPlayer.setInGame(false);
        }

        RunnableShorthand.forPlugin(core).with(this::resetGame).later(60L);
    }

    public void resetGame() {
        resetSigns();
        players.clear();
        modes.clear();
        playerCards.clear();
        bets.clear();
        complete.clear();
        dealerCards.clear();

        key = null;
        hash = null;
        unhashed = null;

        deck = new Deck();

        state = GameState.WAITING;
    }

    private void addPlayer(CryptoPlayer cryptoPlayer, Block sign, Coin bet) {
        players.put(cryptoPlayer, sign);
        playerCards.put(cryptoPlayer, new ArrayList<>());
        bets.put(cryptoPlayer, bet);

        updateSign(cryptoPlayer, ChatColor.GREEN + cryptoPlayer.getPlayer().getName(), bet.toFriendlyString());
    }

    private void addCards(CryptoPlayer cryptoPlayer, int amount) {
        List<Card> cards = playerCards.get(cryptoPlayer);

        for (int index = 0; index < amount; index++) {
            cards.add(deck.getCard());
        }

        String[] lines = formatCards(playerCards.get(cryptoPlayer));

        if (getTotalValueOfCards(cards) > 21) {
            lines[0] = ChatColor.RED + "Bust";
            complete.add(cryptoPlayer);
        }

        lines[3] = Integer.toString(getTotalValueOfCards(cards));

        updateSign(cryptoPlayer, lines);
    }

    private int getTotalValueOfCards(Collection<Card> cards) {
        int aces = 0;
        int totalValue = 0;

        for (Card card : cards) {
            if (card.getRank() == Rank.ACE) {
                aces++;
            }

            totalValue += card.getRank().getValue()[0];
        }

        // aces can be 1 or 11.
        if (totalValue > 21) {
            for (int index = 0; index < aces; index++) {
                totalValue -= 10;

                if (totalValue <= 21) {
                    break;
                }
            }
        }

        return totalValue;
    }

    private String[] formatCards(List<Card> cards) {
        String[] lines = new String[4];

        int currentLine = 1;

        for (List<Card> cardLine : Lists.partition(cards, 4)) {
            lines[currentLine] = cardLine.stream().map(Object::toString).collect(Collectors.joining(" "));

            currentLine += 1;
        }

        return lines;
    }

    private void updateSign(CryptoPlayer cryptoPlayer, String... lines) {
        Block sign = players.get(cryptoPlayer);

        BlockUtil.updateSign((Sign) sign.getState(), lines);
    }

    private void updateDealerSign(String... lines) {
        BlockUtil.updateSign((Sign) dealerSign.getState(), lines);
    }

    private void resetSigns() {
        BlockUtil.clearSign((Sign) dealerSign.getState());

        for (Block sign : signs) {
            BlockUtil.updateSign((Sign) sign.getState(), "", "Place your bet", "", "");
        }
    }

    public void delete() {
        subscriptions.forEach(Subscription::unsubscribe);
        currencyHandler.removeUseListener(useListener);
    }
}
