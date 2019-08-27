package cc.cryptovegas.core;

import cc.cryptovegas.core.commands.*;
import cc.cryptovegas.core.commands.essential.*;
import cc.cryptovegas.core.commands.game.*;
import cc.cryptovegas.core.cryptocurrency.CoinbaseHandler;
import cc.cryptovegas.core.cryptocurrency.CurrencyHandler;
import cc.cryptovegas.core.game.blackjack.BlackJackManager;
import cc.cryptovegas.core.game.horserace.HorseRaceManager;
import cc.cryptovegas.core.game.minefield.MineFieldManager;
import cc.cryptovegas.core.game.roulette.Number;
import cc.cryptovegas.core.game.roulette.RouletteManager;
import cc.cryptovegas.core.npc.NPCManager;
import cc.cryptovegas.core.npc.SerializableNPC;
import cc.cryptovegas.core.npc.SerializableSkin;
import cc.cryptovegas.core.npc.action.ConsoleCommandAction;
import cc.cryptovegas.core.npc.action.PlayerCommandAction;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.player.CryptoPlayers;
import cc.cryptovegas.core.player.JoinListeners;
import cc.cryptovegas.core.player.VPNListener;
import cc.cryptovegas.core.scoreboard.ScoreboardManager;
import cc.cryptovegas.core.util.CountdownTimer;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.inventivetalent.apihelper.APIManager;
import org.inventivetalent.nicknamer.api.NickNamerAPI;
import org.inventivetalent.npclib.NPCLib;
import org.inventivetalent.packetlistener.PacketListenerAPI;
import tech.rayline.core.inject.Inject;
import tech.rayline.core.plugin.RedemptivePlugin;
import tech.rayline.core.plugin.UsesFormats;
import tech.rayline.core.sql.HikariCPBridge;
import tech.rayline.core.util.RunnableShorthand;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@UsesFormats
public final class Core extends RedemptivePlugin {
    private static final int RESTART_HOUR = 3;

    @Inject
    private HikariCPBridge hikariCPBridge;

//    @Inject
//    private RedisBridge redisBridge;

    @Inject
    private CryptoPlayers players;

    @Inject
    private JoinListeners joinListeners;

    @Inject
    private ScoreboardManager scoreboardManager;

    @Inject
    private CurrencyHandler currencyHandler;

    private CoinbaseHandler coinbaseHandler;

    private FileConfiguration statements;

    @Inject
    private TeleportManager teleportManager;

    @Inject
    private SpawnManager spawnManager;

    @Inject
    private ChatManager chatManager;

    @Inject
    private WarpManager warpManager;

    private NPCManager npcManager;

    private MineFieldManager minefieldManager;

    private boolean lockdown = false;

    static {
        ConfigurationSerialization.registerClass(SerializableLocation.class, "SerializableLocation");
        ConfigurationSerialization.registerClass(ConsoleCommandAction.class, "ConsoleCommandAction");
        ConfigurationSerialization.registerClass(PlayerCommandAction.class, "PlayerCommandAction");
        ConfigurationSerialization.registerClass(SerializableNPC.class, "SerializableNPC");
        ConfigurationSerialization.registerClass(SerializableSkin.class, "SerializableSkin");
        ConfigurationSerialization.registerClass(Number.class, "Number");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        PacketListenerAPI packetListener = new PacketListenerAPI();
        NickNamerAPI nickNamerAPI = new NickNamerAPI();
        NPCLib npcLib = new NPCLib();

        APIManager.registerAPI(packetListener);
        APIManager.registerAPI(nickNamerAPI);
        APIManager.registerAPI(npcLib);

        APIManager.require(PacketListenerAPI.class, this);
        APIManager.require(NickNamerAPI.class, this);
        APIManager.require(NPCLib.class, this);
    }

    @Override
    public void onModuleEnable() throws Exception {
        APIManager.initAPI(PacketListenerAPI.class);
        APIManager.initAPI(NickNamerAPI.class);
        APIManager.initAPI(NPCLib.class);

        new VPNListener(this);

        npcManager = new NPCManager(this);
        minefieldManager = new MineFieldManager(this);

        loadStatements();
        executeDefaultStatements();
        coinbaseHandler = new CoinbaseHandler(this);
        players.init();
        joinListeners.init();
        registerCommands();

        scheduleRestart();
    }

    @Override
    public void onModuleDisable() {
        npcManager.disable();
        APIManager.disableAPI(PacketListenerAPI.class);
        APIManager.disableAPI(NickNamerAPI.class);
        APIManager.disableAPI(NPCLib.class);
    }

    private void registerCommands() {
        registerCommand(new HelpCommand());
        registerCommand(new ClaimCommand());
        registerCommand(new ModeCommand());
        registerCommand(new NPCCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new CashierCommand(coinbaseHandler));
        registerCommand(new TPAcceptCommand(teleportManager));
        registerCommand(new TPACommand(teleportManager));
        registerCommand(new TeleportCommand());
        registerCommand(new SetSpawnCommand(spawnManager));
        registerCommand(new SpawnCommand(spawnManager));
        registerCommand(new FlyCommand());
        registerCommand(new BartenderCommand());
        registerCommand(new BlackJackCommand(new BlackJackManager(this)));
        registerCommand(new MineFieldCommand(minefieldManager));
        registerCommand(new RouletteCommand(new RouletteManager(this)));
        registerCommand(new HorseRaceCommand(new HorseRaceManager(this)));
        registerCommand(new HealCommand());
        registerCommand(new WarpCommand(warpManager));
        registerCommand(new SetWarpCommand(warpManager));
        registerCommand(new DelWarpCommand(warpManager));
        registerCommand(new CashoutCommand(minefieldManager));
        registerCommand(new CirculationCommand());
        registerCommand(new SetBalanceCommand());
        registerCommand(new VerifiedCommand());
        registerCommand(new RefillCommand());
        registerCommand(new PayCommand());
        registerCommand(new LockdownCommand());
        registerCommand(new CheckBalanceCommand());
        registerCommand(new RankCommand());
        registerCommand(new CloseCommand());
        registerCommand(new ClearInventoryCommand());
        registerCommand(new UptimeCommand());
        registerCommand(new BalanceTopCommand());
    }

    private void scheduleRestart() {
        ZoneId est = ZoneId.of("US/Eastern");
        ZonedDateTime currentTime = ZonedDateTime.now(est);
        ZonedDateTime scheduledTime = currentTime.withHour(RESTART_HOUR);

        // if current time greater than scheduled time
        if (currentTime.compareTo(scheduledTime) > 0) {
            scheduledTime = scheduledTime.plusDays(1L);
        }

        long delay = Duration.between(currentTime, scheduledTime).getSeconds() * 20L;

        RunnableShorthand.forPlugin(this).with(this::restart).later(delay);
    }

    public void restart() {
        CountdownTimer.create(this, 30L).forEach(value -> {
            if (value == 0) {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.RED + "The server is now restarting.");
                Bukkit.broadcastMessage("");
                return;
            }

            if (value == 1) {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.RED + "The server is restarting in 1 second...");
                Bukkit.broadcastMessage("");
                return;
            }

            if (value == 30 || value == 15 || value < 5) {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.RED + "The server is restarting in " + value + " seconds...");
                Bukkit.broadcastMessage("");
            }
        }).onFinish(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart")).start();
    }

    private void executeDefaultStatements() throws SQLException {
        try (Connection connection = getSQLConnection()) {
            try (PreparedStatement statement = getStatement("create-data-table", connection)) {
                statement.execute();
            }

            try (PreparedStatement statement = getStatement("create-stats-table", connection)) {
                statement.execute();
            }

            try (PreparedStatement statement = getStatement("create-claimed-table", connection)) {
                statement.execute();
            }
        }
    }

    private void loadStatements() {
        try (Reader reader = new InputStreamReader(getClassLoader().getResourceAsStream("internal/statements.yml"))) {
            statements = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLockdown() {
        return lockdown;
    }

    public void setLockdown(boolean lockdown) {
        this.lockdown = lockdown;
    }

//    public JedisPool getPool() {
//        return redisBridge.getPool();
//    }

    public Connection getSQLConnection() throws SQLException {
        return hikariCPBridge.getPool().getConnection();
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public CurrencyHandler getCurrencyHandler() {
        return currencyHandler;
    }

    public PreparedStatement getStatement(String key, Connection connection) throws SQLException {
        return connection.prepareStatement(statements.getString(key));
    }

    public CryptoPlayer getPlayer(Player player) {
        return players.get(player);
    }
}
