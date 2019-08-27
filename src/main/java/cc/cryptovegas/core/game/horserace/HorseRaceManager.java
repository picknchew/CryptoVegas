package cc.cryptovegas.core.game.horserace;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.CustomSound;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.game.GameState;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.*;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Horse;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;
import org.inventivetalent.npclib.NPCType;
import org.inventivetalent.npclib.event.NPCCollisionEvent;
import org.inventivetalent.npclib.npc.NPCAbstract;
import org.inventivetalent.npclib.npc.living.insentient.creature.ageable.animal.NPCHorse;
import org.inventivetalent.npclib.registry.NPCRegistry;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;
import tech.rayline.core.util.RunnableShorthand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HorseRaceManager {
    private static final BigDecimal VIGORISH = new BigDecimal("0.9");

    private final Core core;
    private final CurrencyHandler currencyHandler;
    private final NPCRegistry npcRegistry;
    private GameState state = GameState.WAITING;
    private final Table<Integer, CryptoPlayer, Coin> bets = HashBasedTable.create();
    private final Map<CryptoPlayer, Mode> modes = new HashMap<>();
    private double[] chances;
    private List<Double> speeds = new ArrayList<>(4);
    private final NPCAbstract[] horses = new NPCAbstract[4];
    private final Map<Block, Integer> signs = new HashMap<>();
    private final Set<Block> countdownSigns = new HashSet<>();
    private final DecimalFormat format = new DecimalFormat("#.#");
    private final Map<CryptoPlayer, BukkitTask> startedPlaying = new HashMap<>();

    private long countdown;

    // provably fair
    private String key;
    private String hash;
    private String unhashed;

    @ResourceFile(raw = true, filename = "internals/horserace.yml")
    private YAMLConfigurationFile file;

    public HorseRaceManager(Core core) {
        this.core = core;
        this.currencyHandler = core.getCurrencyHandler();
        this.npcRegistry = core.getNPCManager().getRegistry();

        format.setRoundingMode(RoundingMode.DOWN);

        core.getResourceFileGraph().addObject(this);

        if (file.getConfig().contains("countdown_signs")) {
            ((List<SerializableLocation>) file.getConfig().getList("countdown_signs")).stream()
                    .map(SerializableLocation::get)
                    .map(Location::getBlock)
                    .forEach(countdownSigns::add);
        }

        if (file.getConfig().contains("signs")) {
            for (String key : file.getConfig().getConfigurationSection("signs").getKeys(false)) {
                List<SerializableLocation> list = (List<SerializableLocation>) file.getConfig().getList("signs." + key);

                list.forEach(location -> signs.put(location.get().getBlock(), Integer.parseInt(key)));
            }
        }

        RunnableShorthand.forPlugin(core).with(() -> Arrays.stream(horses).forEach(horse -> {
            horse.invokeNPCMethod("tickEntity");
            horse.onBaseTick();
        })).repeat(1L);

        core.observeEvent(ChunkUnloadEvent.class)
                .filter(event -> HorsePath.CHUNKS.contains(event.getChunk()))
                .doOnNext(event -> event.setCancelled(true))
                .subscribe();

        core.observeEvent(NPCCollisionEvent.class).doOnNext(event -> event.setCancelled(true)).subscribe();

        core.observeEvent(BlockBreakEvent.class)
                .filter(event -> countdownSigns.contains(event.getBlock()))
                .doOnNext(event -> event.setCancelled(true))
                .subscribe();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> signs.containsKey(event.getClickedBlock()))
                .filter(event -> !startedPlaying.containsKey(core.getPlayer(event.getPlayer())))
                .filter(event -> !bets.containsColumn(event.getPlayer()))
                .filter(event -> !core.getPlayer(event.getPlayer()).isInGame())
                .filter(event -> state == GameState.WAITING || state == GameState.STARTING)
                .doOnNext(event -> {
                    CryptoPlayer cryptoPlayer = core.getPlayer(event.getPlayer());

                    cryptoPlayer.sendMessage(ChatColor.YELLOW + "You started playing horse race! Left-click to start betting.");
                    cryptoPlayer.setInGame(true);

                    BukkitTask task = RunnableShorthand.forPlugin(core).with(() -> {
                        if (startedPlaying.containsKey(cryptoPlayer)) {
                            cryptoPlayer.sendMessage(ChatColor.RED + "You stopped playing horse race because you did not bet in time.");
                            startedPlaying.remove(cryptoPlayer).cancel();
                            cryptoPlayer.setInGame(false);
                        }
                    }).later(300L);

                    startedPlaying.put(cryptoPlayer, task);

                    event.setCancelled(true);
                }).subscribe();

        HorsePath.LANE_ONE_START.getChunk().load();
        HorsePath.LANE_TWO_START.getChunk().load();
        HorsePath.LANE_THREE_START.getChunk().load();
        HorsePath.LANE_FOUR_START.getChunk().load();

        HorsePath.CHUNKS.forEach(Chunk::load);

        horses[0] = npcRegistry.spawnNPC(HorsePath.LANE_ONE_START, NPCType.HORSE);
        horses[1] = npcRegistry.spawnNPC(HorsePath.LANE_TWO_START, NPCType.HORSE);
        horses[2] = npcRegistry.spawnNPC(HorsePath.LANE_THREE_START, NPCType.HORSE);
        horses[3] = npcRegistry.spawnNPC(HorsePath.LANE_FOUR_START, NPCType.HORSE);

        for (int x = 0; x < horses.length; x++) {
            NPCHorse horse = (NPCHorse) horses[x];

            horse.setPersistent(true);
            horse.getBukkitEntity().setRemoveWhenFarAway(false);
            horse.getBukkitEntity().setCustomName(ChatColor.YELLOW.toString() + ChatColor.BOLD + "HORSE " + ChatColor.WHITE + ChatColor.BOLD + "#" + (x + 1));
            horse.getBukkitEntity().setStyle(Horse.Style.values()[x]);
            horse.getBukkitEntity().setColor(Horse.Color.values()[x]);
            horse.getBukkitEntity().setCustomNameVisible(true);
            horse.setYaw(-180F);
        }

        resetGame(false);

        core.getCurrencyHandler().addUseListener((cryptoPlayer, currency, block) -> {
            if (signs.containsKey(block)) {
                if (state != GameState.WAITING && state != GameState.STARTING) {
                    cryptoPlayer.sendMessage(ChatColor.RED + "The race has already started!");
                    return true;
                }

                int number = signs.get(block);

                if (bets.contains(number, cryptoPlayer)) {
                    Coin bet = currency.getValue();
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

                    Coin totalBet = bets.get(number, cryptoPlayer).add(bet);

                    bets.put(number, cryptoPlayer, totalBet);
                    cryptoPlayer.sendMessage(ChatColor.GREEN + "You added to your bet, you now have a bet of " + totalBet.toFriendlyString() + " on horse " + (number + 1));
                    return true;
                }

                if (!startedPlaying.containsKey(cryptoPlayer) && !bets.containsColumn(cryptoPlayer)) {
                    return true;
                }

                Coin bet = currency.getValue();
                Mode mode = modes.getOrDefault(cryptoPlayer, cryptoPlayer.getMode());

                if (!cryptoPlayer.hasEnoughCoins(bet, mode)) {
                    cryptoPlayer.sendMessage(ChatColor.RED + "You do not have enough coins!");
                    return true;
                }

                BukkitTask task = startedPlaying.remove(cryptoPlayer);

                if (task != null) {
                    task.cancel();
                }

                if (mode == Mode.REAL_MONEY) {
                    currencyHandler.removeRealCoins(cryptoPlayer.getUniqueId().toString(), bet);
                } else {
                    currencyHandler.removePlayCoins(cryptoPlayer.getUniqueId().toString(), bet);
                }

                if (!modes.containsKey(cryptoPlayer)) {
                    modes.put(cryptoPlayer, mode);
                }

                cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).subtract(bet));

                bets.put(number, cryptoPlayer, bet);
                cryptoPlayer.sendMessage(ChatColor.GREEN + "You bet " + bet.toFriendlyString() + " on horse " + (number + 1));

                if (state == GameState.WAITING) {
                    state = GameState.STARTING;

                    CountdownTimer.create(core, 30L).forEach((count) -> {
                        if (count == 30 || count == 15 || count <= 5) {
                            Bukkit.broadcastMessage(ChatColor.RED + Long.toString(count) +  " seconds before the race starts.");
                        }

                        if (count <= 3 && count > 0) {
                            Bukkit.getOnlinePlayers().forEach(player -> CustomSound.COUNTDOWN.playFor(core.getPlayer(player)));
                        }

                        if (count == 0) {
                            Bukkit.getOnlinePlayers().forEach(player -> CustomSound.END_COUNTDOWN.playFor(core.getPlayer(player)));
                        }

                        countdownSigns.forEach(sign -> BlockUtil.updateSign((Sign) sign.getState(), "THE HORSE RACE", "will begin in", ChatColor.GREEN + Long.toString(count) + " seconds"));
                        countdown = count;
                    }).onFinish(() -> {
                        countdown = -1;
                        bets.rowMap().forEach((horse, map) -> map.entrySet().forEach((entry) -> entry.setValue(entry.getValue().multiply(VIGORISH))));

                        Bukkit.broadcastMessage(ChatColor.RED + "LET THE RACE BEGIN!");
                        countdownSigns.forEach(sign -> BlockUtil.updateSign((Sign) sign.getState(), "", "THE RACE HAS", "STARTED!", ""));
                        startRace();
                    }).start();
                }

                return true;
            }

            return false;
        });

        for (int x = 0; x < horses.length; x++) {
            int finalX = x;

            HologramsAPI.registerPlaceholder(core, "{horse:" + (x + 1) + "}", 1, () -> format.format(chances[finalX]));
        }

        HologramsAPI.registerPlaceholder(core, "{horsecountdown}", 1, () -> {
            if (countdown > -1) {
                return ChatColor.RED + ChatColor.BOLD.toString() + "Race starts in " + Long.toString(countdown) + " seconds.";
            } else {
                return ChatColor.RED + ChatColor.BOLD.toString() + "The race has now begun!";
            }
        });
    }
    private void sendMessageToPlayers(String message) {
        bets.columnKeySet().stream().distinct().forEach(player -> player.sendMessage(message));
    }

    private void startRace() {
        state = GameState.PLAYING;

        startedPlaying.forEach((cryptoPlayer, task) -> {
            task.cancel();
            cryptoPlayer.getPlayer().sendMessage(ChatColor.RED + "You stopped playing horse race because you did not bet in time.");
            cryptoPlayer.setInGame(false);
        });

        startedPlaying.clear();

        RandomCollection<Integer> randomCollection = new RandomCollection<>();

        for (int x = 0; x < chances.length; x++) {
            randomCollection.add(chances[x], x);
        }

        int horseNumber = randomCollection.next();

        key = HashingUtil.generateRandomKey();
        unhashed = UUID.randomUUID().toString().replace("-", "") + ":" + (horseNumber + 1);
        hash = HashingUtil.hashString(unhashed, key);

        sendMessageToPlayers(ChatColor.RED + "Game hash: " + ChatColor.YELLOW + hash);

        HorsePathfinder.PathFinishedListener[] listeners = new HorsePathfinder.PathFinishedListener[4];

        listeners[horseNumber] = () -> {
            double chance = chances[horseNumber] / 100;
            double edge = 0.05;

            double payoutPercent = Math.floor(((((1 - edge) - edge) * (0.99 / chance))) * 100) / 100;

            Bukkit.broadcastMessage(ChatColor.RED + "Horse " + (horseNumber + 1) + " won the race with a payout of " + payoutPercent + "x!");

            CountdownTimer.create(core, 3L)
                    .forEach(count -> {
                        for (int x = 0; x < 3; x++) {
                            spawnRandomFirework(HorsePath.FIREWORKS);
                        }
                    }).start();

            if (bets.rowMap().containsKey(horseNumber)) {
                bets.rowMap().get(horseNumber).forEach((cryptoPlayer, bet) -> {
                    Mode mode = modes.get(cryptoPlayer);

                    CustomSound.DING.playFor(cryptoPlayer);
                    // payout = ((1 - edge) - edge) * ((0.099 * bet) / chance)
                    Coin payout = Coin.valueOf((long) (payoutPercent * bet.getValue()));

                    cryptoPlayer.sendMessage(core.formatAt("game-win").with("coin", payout.toFriendlyString()).get());

                    if (mode == Mode.REAL_MONEY) {
                        currencyHandler.addRealCoins(cryptoPlayer.getUniqueId().toString(), payout);
                    } else {
                        currencyHandler.addPlayCoins(cryptoPlayer.getUniqueId().toString(), payout);
                    }

                    cryptoPlayer.setCoins(mode, cryptoPlayer.getCoins(mode).add(payout));
                });
            }

            BaseComponent[] component = new ComponentBuilder("Provably Fair: ")
                    .color(net.md_5.bungee.api.ChatColor.RED)
                    .append("Click to open the url.")
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://verify.cryptovegas.cc/provably-fair/horserace/" + key + "/" + unhashed + "/" + hash))
                    .color(net.md_5.bungee.api.ChatColor.YELLOW)
                    .create();

            bets.columnKeySet().stream().distinct().forEach((cryptoPlayer) -> {
                if (modes.get(cryptoPlayer) == Mode.REAL_MONEY) {
                    PlayerUtil.incrRealBets(cryptoPlayer, core);
                }

                cryptoPlayer.sendMessage(component);
            });

            resetGame(false);
        };

        // speeds is sorted in descending order, max index is 3.
        speeds.add(horseNumber, speeds.remove(3));

        HorsePathfinder ai = new HorsePathfinder(HorsePath.LANE_ONE_POINTS, HorsePath.LANE_ONE_SPEED + speeds.get(0), listeners[0]);
        HorsePathfinder ai2 = new HorsePathfinder(HorsePath.LANE_TWO_POINTS, HorsePath.LANE_TWO_SPEED + speeds.get(1), listeners[1]);
        HorsePathfinder ai3 = new HorsePathfinder(HorsePath.LANE_THREE_POINTS, HorsePath.LANE_THREE_SPEED + speeds.get(2), listeners[2]);
        HorsePathfinder ai4 = new HorsePathfinder(HorsePath.LANE_FOUR_POINTS, HorsePath.LANE_FOUR_SPEED + speeds.get(3), listeners[3]);

        NPCHorse horse1 = (NPCHorse) horses[0];
        NPCHorse horse2 = (NPCHorse) horses[1];
        NPCHorse horse3 = (NPCHorse) horses[2];
        NPCHorse horse4 = (NPCHorse) horses[3];

        ai.setNpc(horse1);
        ai2.setNpc(horse2);
        ai3.setNpc(horse3);
        ai4.setNpc(horse4);

        horse1.registerAI(ai);
        horse2.registerAI(ai2);
        horse3.registerAI(ai3);
        horse4.registerAI(ai4);
    }

    public void resetGame(boolean force) {
        state = GameState.WAITING;
        bets.columnKeySet().stream().distinct().forEach(cryptoPlayer -> cryptoPlayer.setInGame(false));
        bets.clear();
        modes.clear();

        key = null;
        unhashed = null;
        hash = null;

        this.chances = generateChances();
        generateRandomSpeeds();

        if (force) {
            for (NPCAbstract horse : horses) {
                horse.clearAITasks();
            }
        }

        RunnableShorthand.forPlugin(core).with(() -> {
            horses[0].getBukkitEntity().teleport(HorsePath.LANE_ONE_START);
            horses[1].getBukkitEntity().teleport(HorsePath.LANE_TWO_START);
            horses[2].getBukkitEntity().teleport(HorsePath.LANE_THREE_START);
            horses[3].getBukkitEntity().teleport(HorsePath.LANE_FOUR_START);
        }).later(60L);

        signs.keySet().forEach(this::updateSign);
        countdownSigns.forEach(sign -> BlockUtil.updateSign((Sign) sign.getState(), "THE HORSE RACE", "will begin in", ChatColor.GREEN + "30 seconds"));
        countdown = 30;
    }

    private void updateSign(Block block) {
        int lane = signs.get(block);
        BlockUtil.updateSign((Sign) block.getState(), "Horse " + (lane + 1), "", format.format(chances[lane]) + "% chance", ChatColor.GREEN + "Click to bet!");
    }

    private double[] generateChances() {
        double[] chances = new double[4];
        double sum = 0;

        for (int x = 0; x < chances.length; x++) {
            chances[x] = ThreadLocalRandom.current().nextDouble();
            sum += chances[x];
        }

        for (int x = 0; x < chances.length; x++) {
            chances[x] = chances[x] / sum * 100;
        }

        return chances;
    }

    private void generateRandomSpeeds() {
        speeds.clear();

        speeds.add(0.13D);
        speeds.add(0.2D);
        speeds.add(0.27D);
        speeds.add(0.34D);

        Collections.sort(speeds);
    }

    public boolean isSignUsed(Block block) {
        return signs.containsKey(block);
    }

    public void setSign(Block block, int lane) {
        signs.put(block, lane);
        updateSign(block);
        save();
    }

    public void setCountdownSign(Block block) {
        countdownSigns.add(block);
        save();
    }

    public void removeCountdownSign(Block block) {
        countdownSigns.remove(block);
    }

    public boolean isCountdownSign(Block block) {
        return countdownSigns.contains(block);
    }

    public void removeSign(Block block) {
        signs.remove(block);
        save();
    }

    private void save() {
        file.getConfig().set("countdown_signs", countdownSigns.stream().map(Block::getLocation).map(SerializableLocation::new).collect(Collectors.toList()));
        file.getConfig().set("signs.0", signs.entrySet().stream().filter(entry -> entry.getValue() == 0).map(entry -> new SerializableLocation(entry.getKey().getLocation())).collect(Collectors.toList()));
        file.getConfig().set("signs.1", signs.entrySet().stream().filter(entry -> entry.getValue() == 1).map(entry -> new SerializableLocation(entry.getKey().getLocation())).collect(Collectors.toList()));
        file.getConfig().set("signs.2", signs.entrySet().stream().filter(entry -> entry.getValue() == 2).map(entry -> new SerializableLocation(entry.getKey().getLocation())).collect(Collectors.toList()));
        file.getConfig().set("signs.3", signs.entrySet().stream().filter(entry -> entry.getValue() == 3).map(entry -> new SerializableLocation(entry.getKey().getLocation())).collect(Collectors.toList()));

        file.saveConfig();
    }

    public void spawnRandomFirework(final Location loc) {
        Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
                .flicker(ThreadLocalRandom.current().nextBoolean())
                .withColor(getColor((ThreadLocalRandom.current().nextInt(17) + 1)))
                .withFade(getColor(ThreadLocalRandom.current().nextInt(17) + 1))
                .with(FireworkEffect.Type.values()[ThreadLocalRandom.current().nextInt(FireworkEffect.Type.values().length)])
                .trail(ThreadLocalRandom.current().nextBoolean())
                .build();

        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(ThreadLocalRandom.current().nextInt(2) + 1);
        firework.setFireworkMeta(fireworkMeta);
    }

    private Color getColor(final int i) {
        switch (i) {
            case 1:
                return Color.AQUA;
            case 2:
                return Color.BLACK;
            case 3:
                return Color.BLUE;
            case 4:
                return Color.FUCHSIA;
            case 5:
                return Color.GRAY;
            case 6:
                return Color.GREEN;
            case 7:
                return Color.LIME;
            case 8:
                return Color.MAROON;
            case 9:
                return Color.NAVY;
            case 10:
                return Color.OLIVE;
            case 11:
                return Color.ORANGE;
            case 12:
                return Color.PURPLE;
            case 13:
                return Color.RED;
            case 14:
                return Color.SILVER;
            case 15:
                return Color.TEAL;
            case 16:
                return Color.WHITE;
            case 17:
                return Color.YELLOW;
        }

        return null;
    }
}
