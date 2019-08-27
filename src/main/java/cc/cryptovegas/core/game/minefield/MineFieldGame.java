package cc.cryptovegas.core.game.minefield;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.CurrencyItem;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.game.GameState;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.BlockUtil;
import cc.cryptovegas.core.util.CountdownTimer;
import cc.cryptovegas.core.util.HashingUtil;
import cc.cryptovegas.core.util.PlayerUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;
import rx.Subscription;
import tech.rayline.core.util.ItemShorthand;
import tech.rayline.core.util.RunnableShorthand;

import java.util.*;

public class MineFieldGame {
    private static final double DEFAULT_CHANCE = 0.05D;

    private final Core core;
    private final CurrencyHandler currencyHandler;

    private final Block sign;

    private double chance;

    private GameState state = GameState.WAITING;
    private final Set<Block> blocks;
    private Player currentlyPlaying;
    private Mode mode;

    private CalculatedMine mine;
    private Coin bet;

    private boolean won;

    // amount that you "find" when you break a block.
    private long currentFound, increment;

    private CountdownTimer startingTask;
    private CountdownTimer endingTask;

    private final Map<CryptoPlayer, BukkitTask> startedPlaying = new HashMap<>();

    private final Set<Subscription> subscriptions = new HashSet<>();
    private final CurrencyHandler.UseListener useListener;

    // provably fair
    private String key;
    private String hash;
    private String unhashed;

    public MineFieldGame(Core core, Block sign) {
        this(core, sign, new HashSet<>(), DEFAULT_CHANCE);
    }

    public MineFieldGame(Core core, Block sign, Set<Block> blocks, double chance) {
        this.core = core;
        this.currencyHandler = core.getCurrencyHandler();
        this.sign = sign;
        this.blocks = blocks;
        this.chance = chance;

        subscriptions.add(core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> sign.equals(event.getClickedBlock()))
                .filter(event -> !startedPlaying.containsKey(core.getPlayer(event.getPlayer())))
                .filter(event -> !core.getPlayer(event.getPlayer()).isInGame())
                .filter(event -> state == GameState.WAITING || state == GameState.STARTING)
                .doOnNext(event -> {
                            Player player = event.getPlayer();
                            CryptoPlayer cryptoPlayer = core.getPlayer(player);

                            player.sendMessage(ChatColor.YELLOW + "You started playing minefield! Left-click to start betting.");
                            cryptoPlayer.setInGame(true);

                            BukkitTask task = RunnableShorthand.forPlugin(core).with(() -> {
                                if (startedPlaying.containsKey(cryptoPlayer)) {
                                    player.sendMessage(ChatColor.RED + "You stopped playing minefield because you did not bet in time.");
                                    startedPlaying.remove(cryptoPlayer).cancel();
                                    cryptoPlayer.setInGame(false);
                                }
                            }).later(300L);

                            startedPlaying.put(cryptoPlayer, task);

                            event.setCancelled(true);
                        }
                ).subscribe());

        core.getCurrencyHandler().addUseListener(useListener = (cryptoPlayer, currency, block) -> {
            if (sign.equals(block)) {
                Player player = cryptoPlayer.getPlayer();

                if (state == GameState.WAITING && startedPlaying.containsKey(cryptoPlayer)) {
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
                    this.bet = bet;
                    state = GameState.STARTING;
                    currentlyPlaying = player;
                    this.mode = mode;

                    startingTask = CountdownTimer.create(core, 10L).forEach(count -> {
                        BlockUtil.updateSign((Sign) sign.getState(), ChatColor.GREEN + player.getName(), this.bet.toFriendlyString(), "", Long.toString(count) + " seconds");
                    }).onFinish(() -> {
                        if (currentlyPlaying != null) {
                            startedPlaying.forEach((startedPlayer, task) -> {
                                task.cancel();
                                startedPlayer.getPlayer().sendMessage(ChatColor.RED + "You stopped playing minefield because you did not bet in time.");
                                startedPlayer.setInGame(false);
                            });

                            startedPlaying.clear();

                            player.sendMessage(ChatColor.GREEN + "You have started a game of minefield.\nBreak blocks. If you hit a mine, you lose!");
                            BlockUtil.updateSign((Sign) sign.getState(), null, null, null, "");

                            start(player);
                        }
                    });

                    startingTask.start();
                    return true;
                }

                if (state == GameState.STARTING && currentlyPlaying == player) {
                    Coin bet = currency.getValue();

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

                    this.bet = this.bet.add(bet);
                    BlockUtil.updateSign((Sign) sign.getState(), ChatColor.GREEN + player.getName(), this.bet.toFriendlyString());
                }

                return true;
            }

            return false;
        });

        subscriptions.add(core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> state == GameState.ENDING)
                .filter(event -> won)
                .filter(event -> event.getPlayer() == currentlyPlaying)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> event.getClickedBlock().equals(sign))
                .doOnNext(event -> {
                    Coin coin = Coin.valueOf(currentFound);
                    CryptoPlayer cryptoPlayer = core.getPlayer(currentlyPlaying);

                    currentlyPlaying.sendMessage(ChatColor.GREEN + "You have claimed " + coin.toFriendlyString());
                    CustomSound.DING.playFor(cryptoPlayer);

                    Coin payout = Coin.valueOf(currentFound);

                    if (mode == Mode.REAL_MONEY) {
                        currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
                    } else {
                        currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
                    }

                    cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));

                    endingTask.cancel();

                    event.setCancelled(true);
                }).subscribe());

        subscriptions.add(core.observeEvent(EventPriority.HIGH, true, BlockBreakEvent.class)
                .filter(event -> blocks.contains(event.getBlock()) || event.getBlock().equals(sign))
                .doOnNext(event -> event.setCancelled(true))
                .subscribe());

        subscriptions.add(core.observeEvent(BlockBreakEvent.class)
                .filter(event -> state == GameState.PLAYING)
                .filter(event -> blocks.contains(event.getBlock()))
                .doOnNext(event -> event.setCancelled(true))
                .filter(event -> event.getBlock().getType() != Material.TNT)
                .filter(event -> event.getPlayer() == currentlyPlaying)
                .doOnNext(event -> {
                    event.getBlock().setType(Material.AIR);

                    Player player = event.getPlayer();

                    if (mine.isMine(event.getBlock())) {
                        for (Block mine : getCalculatedMine().getMines()) {
                            mine.setType(Material.TNT);
                        }

                        state = GameState.ENDING;
                    } else {
                        long sizeOfBrokenBlocks = blocks.stream().filter(block -> block.getType() == Material.GRASS).count();

                        if (sizeOfBrokenBlocks == getCalculatedMine().getMines().size()) {
                            player.sendMessage(ChatColor.GREEN + "You won " + Coin.valueOf(currentFound).toFriendlyString() + ", right-click the sign to claim it.");

                            if (mode == Mode.REAL_MONEY) {
                                PlayerUtil.incrRealBets(core.getPlayer(player), core);
                            }

                            endingTask = CountdownTimer.create(core, 10L)
                                    .forEach(count -> BlockUtil.updateSign((Sign) sign.getState(), "", "Claim your prize", Long.toString(count), ""))
                                    .onFinish(() -> reset(false));

                            endingTask.start();

                            state = GameState.ENDING;
                            won = true;
                            return;
                        }

                        if (currentFound > 0) {
                            currentFound += increment;
                        } else {
                            currentFound += bet.getValue();
                        }

                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Total found: " + ChatColor.GREEN + Coin.valueOf(currentFound).toFriendlyString() + ". " +
                                ChatColor.AQUA + "Type /cashout to cashout.");

                        return;
                    }

                    PlayerUtil.incrRealBets(core.getPlayer(player), core);
                    player.sendMessage(ChatColor.RED + "You hit a mine!");
                    RunnableShorthand.forPlugin(core).with(() -> reset(false)).later(60L);
                }).subscribe());

        subscriptions.add(core.observeEvent(PlayerQuitEvent.class).filter(event -> event.getPlayer() == currentlyPlaying)
                .doOnNext(event -> {
                    if (state == GameState.STARTING) {
                        startingTask.cancel();
                    }

                    reset(false);
                }).subscribe());

        reset(false);
    }

    public Player getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    public Mode getCoinMode() {
        return mode;
    }

    public long getCurrentFound() {
        return currentFound;
    }

    public void endGame(boolean force) {
        reset(force);
    }

    private void start(Player player) {
        if (mode == Mode.PLAY_MONEY) {
            double newChance = chance - 0.10;

            if (newChance < 0) {
                newChance = 0;
            }

            mine = new CalculatedMine(newChance, blocks);
        } else {
            mine = new CalculatedMine(chance, blocks);
        }

        key = HashingUtil.generateRandomKey();
        unhashed = UUID.randomUUID().toString().replace("-", "") + ":" + mine.toString();
        hash = HashingUtil.hashString(unhashed, key);

        player.sendMessage(ChatColor.RED + "Mines hash: " + ChatColor.YELLOW + hash);

        endingTask = null;
        state = GameState.PLAYING;

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.addItem(ItemShorthand.withMaterial(Material.GOLD_SPADE).get());
        player.updateInventory();

        increment = (long) (0.05 * bet.value);
    }

    private void reset(boolean force) {
        if (currentlyPlaying != null) {
            PlayerInventory inventory = currentlyPlaying.getInventory();

            if (!force) {
                BaseComponent[] component = new ComponentBuilder("Provably Fair: ")
                        .color(net.md_5.bungee.api.ChatColor.RED)
                        .append("Click to open the url.")
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://verify.cryptovegas.cc/provably-fair/minefield/" + key + "/" + unhashed + "/" + hash))
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .create();

                currentlyPlaying.spigot().sendMessage(component);
            }

            inventory.clear();
            Arrays.stream(CurrencyItem.values()).forEach(currency -> inventory.setItem(currency.ordinal(), currency.getItem()));
            currentlyPlaying.updateInventory();
            core.getPlayer(currentlyPlaying).setInGame(false);
            currentlyPlaying = null;
        }

        key = null;
        unhashed = null;
        hash = null;
        state = GameState.WAITING;
        mode = null;
        won = false;
        increment = 0L;
        currentFound = 0L;
        fill();

        BlockUtil.updateSign((Sign) sign.getState(), "", "Place your bet", "", "");
    }

    private void fill() {
        blocks.forEach(block -> block.setType(Material.GRASS));
    }

    public void addBlock(Block block) {
        blocks.add(block);
    }

    public void removeBlock(Block block) {
        blocks.remove(block);
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public GameState getState() {
        return state;
    }

    public CalculatedMine getCalculatedMine() {
        return mine;
    }

    public double getChance() {
        return chance;
    }

    public Block getDealerSign() {
        return sign;
    }

    public boolean containsBlock(Block block) {
        return blocks.contains(block);
    }

    public Set<Block> getBlocks() {
        return blocks;
    }

    private double roundDouble(double value) {
        return (double) Math.round(value * 1000000d) / 1000000d;
    }

    public void delete() {
        subscriptions.forEach(Subscription::unsubscribe);
        currencyHandler.removeUseListener(useListener);
    }
}
