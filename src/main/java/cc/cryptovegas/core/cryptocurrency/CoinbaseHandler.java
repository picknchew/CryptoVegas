package cc.cryptovegas.core.cryptocurrency;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import me.picknchew.coinbase.commerce.Coinbase;
import me.picknchew.coinbase.commerce.CoinbaseBuilder;
import me.picknchew.coinbase.commerce.Currency;
import me.picknchew.coinbase.commerce.Price;
import me.picknchew.coinbase.commerce.body.CreateChargeBody;
import me.picknchew.coinbase.commerce.event.model.ChargeStatusEvent;
import me.picknchew.coinbase.commerce.model.Charge;
import me.picknchew.coinbase.commerce.model.Payment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tech.rayline.core.inject.DisableHandler;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.util.RunnableShorthand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Injectable
public class CoinbaseHandler {
    private static final Coin MINIMUM_DEPOSIT = Coin.parseCoin("0.0003");

    private final Core core;
    private final JsonParser jsonParser = new JsonParser();
    private final Gson gson = new Gson();
    private final CurrencyHandler currencyHandler;
    private final Coinbase coinbase = new CoinbaseBuilder()
            .withApiKey("API_KEY")
            .withWebhookSecret("SECRET").build();

    private final Map<Currency, String> usdRates = new HashMap<>();

    private final Map<Currency, BigDecimal> exchangeRates = new ConcurrentHashMap<>();

    @InjectionProvider
    public CoinbaseHandler(Core core) {
        this.core = core;
        this.currencyHandler = core.getCurrencyHandler();

        try {
            updateExchangeRates();
        } catch (IOException e) {
            core.getLogger().warning("Failed to retrieve new exchange rates.");
        }

        updateUSDRates();

        RunnableShorthand.forPlugin(core).with(() -> {
            updateUSDRates();

            try {
                updateExchangeRates();
            } catch (IOException e) {
                core.getLogger().warning("Failed to retrieve new exchange rates.");
            }
        }).repeat(5 * 60 * 20);

        createTransactionsTable();
        initPaymentListener();

        registerPlaceholders();
    }

    @DisableHandler
    public void onDisable() {
        coinbase.getNotificationHandler().stop();
    }

    private void registerPlaceholders() {
        Arrays.stream(Currency.values()).filter(currency -> currency != Currency.LOCAL).forEach(currency -> {
            HologramsAPI.registerPlaceholder(core, "{" + currency.getSymbol() + "}", 60 * 5, () -> usdRates.get(currency));
        });
    }

    private void createTransactionsTable() {
        try (Connection connection = core.getSQLConnection()) {
            try (PreparedStatement statement = core.getStatement("create-transactions-table", connection)) {
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initPaymentListener() {
        coinbase.getNotificationHandler().registerListener(ChargeStatusEvent.class, event -> {
            ChargeStatusEvent.Status status = event.getStatus();
            Charge charge = event.getCharge();
            Map<String, String> metadata = charge.getMetadata();

            String transactionId = metadata.get("transaction_id");
            String playerUUID = metadata.get("player_uuid");

            if (status == ChargeStatusEvent.Status.CREATED) {
                createTransaction(transactionId, playerUUID);
                return;
            }

            if (status == ChargeStatusEvent.Status.CONFIRMED) {
                Coin total = Coin.ZERO;

                for (Payment payment : charge.getPayments()) {
                    if (payment.getStatus() == Payment.Status.CONFIRMED) {
                        Price price = payment.getValue().get("crypto");
                        total = total.add(convert(payment.getCurrency(), new BigDecimal(price.getAmount())));
                    }
                }

                Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));

                if (!total.isLessThan(MINIMUM_DEPOSIT)) {
                    if (player != null) {
                        CryptoPlayer cryptoPlayer = core.getPlayer(player);
                        cryptoPlayer.setCoins(Mode.REAL_MONEY, cryptoPlayer.getCoins(Mode.REAL_MONEY).add(total));
                        cryptoPlayer.sendMessage(core.formatAt("deposit-complete").with("amount", total).get());
                    }

                    currencyHandler.addRealCoins(playerUUID, total);
                } else {
                    if (player != null) {
                        player.sendMessage(core.formatAt("deposit-minimum").get());
                    }
                }

                updateTransaction(transactionId, playerUUID, status, total.toFriendlyString());
                return;
            }

            updateTransaction(transactionId, playerUUID, status, "0.00 BTC");
        });
    }

    public Charge createCharge(String playerUUID) throws IOException {
        CreateChargeBody body = new CreateChargeBody("CryptoVegas", "In-game currency for CryptoVegas", Charge.PricingType.NO_PRICE);
        Map<String, String> metadata = new HashMap<>();

        metadata.put("transaction_id", UUID.randomUUID().toString());
        metadata.put("player_uuid", playerUUID);

        body.setMetadata(metadata);

        return coinbase.getChargesService().createCharge(body).execute().body();
    }

    private void createTransaction(String transactionId, String playerUUID) {
        try (Connection connection = core.getSQLConnection()) {
            try (PreparedStatement statement = core.getStatement("insert-transaction", connection)) {
                statement.setString(1, transactionId);
                statement.setString(2, playerUUID);
                statement.setString(3, ChargeStatusEvent.Status.CREATED.name());
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTransaction(String transactionId, String playerUUID, ChargeStatusEvent.Status status, String amount) {
        try (Connection connection = core.getSQLConnection()) {
            try (PreparedStatement statement = core.getStatement("update-transaction", connection)) {
                statement.setString(1, status.name());
                statement.setString(2, amount);
                statement.setString(3, transactionId);
                statement.setString(4, playerUUID);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Coin convert(Currency currency, BigDecimal value) {
        // 1 btc / exchangeRates.get(currency) * value = x
        return Coin.parseCoin(exchangeRates.get(currency).multiply(value).setScale(8).toString());
    }

    private void updateExchangeRates() throws IOException {
        URL url = new URL("https://api.coinbase.com/v2/exchange-rates?currency=btc");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String data = reader.lines().collect(Collectors.joining());
            JsonObject object = jsonParser.parse(data).getAsJsonObject().getAsJsonObject("data");

            Type type = TypeToken.getParameterized(HashMap.class, String.class, String.class).getType();
            Map<String, String> rates = gson.fromJson(object.get("rates"), type);

            Arrays.stream(Currency.values()).filter(currency -> currency != Currency.LOCAL).forEach(currency -> {
                exchangeRates.put(currency, BigDecimal.ONE.divide(new BigDecimal(rates.get(currency.getSymbol())), 10, BigDecimal.ROUND_HALF_DOWN));
            });
        }
    }

    private void updateUSDRates() {
        Arrays.stream(Currency.values()).filter(currency -> currency != Currency.LOCAL).forEach(currency -> {
            try {
                URL url = new URL("https://api.coinbase.com/v2/exchange-rates?currency=" + currency.getSymbol().toLowerCase());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String data = reader.lines().collect(Collectors.joining());
                    JsonObject object = jsonParser.parse(data).getAsJsonObject().getAsJsonObject("data");

                    Type type = TypeToken.getParameterized(HashMap.class, String.class, String.class).getType();
                    Map<String, String> rates = gson.fromJson(object.get("rates"), type);

                    usdRates.put(currency, rates.get("USD"));
                }
            } catch (IOException e) {
                core.getLogger().warning("Failed to retrieve new USD rates.");
            }
        });
    }
}
