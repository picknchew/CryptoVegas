package cc.cryptovegas.core.player;

import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.Multiplier;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.scoreboard.ScoreboardManager;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class CryptoPlayer {
    private final UUID uuid;

    private Mode mode;
    private Coin realCoins;
    private Coin playCoins;
    // when the claims
    private Instant lastClaimed;
    private Duration claimCooldown;
    private Instant lastRefilled;
    private Multiplier multiplier;
    private boolean inGame = false;
    private boolean verified = false;

    private final PlayerStats stats;

    public CryptoPlayer(UUID uuid, Mode mode, Coin realCoins, Coin playCoins, Duration claimCooldown, Instant lastRefilled, boolean verified) {
        this(uuid, mode, realCoins, playCoins, claimCooldown, lastRefilled, null, verified, new PlayerStats());
    }

    public CryptoPlayer(UUID uuid, Mode mode, Coin realCoins, Coin playCoins, Duration claimCooldown, Instant lastRefilled, Multiplier multiplier, boolean verified, PlayerStats stats) {
        this.uuid = uuid;
        this.mode = mode;
        this.realCoins = realCoins;
        this.playCoins = playCoins;
        this.claimCooldown = claimCooldown;
        this.lastRefilled = lastRefilled;
        this.multiplier = multiplier;
        this.verified = verified;
        this.stats = stats;
    }

    public void sendMessage(String message) {
        if (!isOnline()) {
            return;
        }

        getPlayer().sendMessage(message);
    }

    public void sendMessage(BaseComponent... component) {
        if (!isOnline()) {
            return;
        }

        getPlayer().spigot().sendMessage(component);
    }

    public void addPotionEffect(PotionEffect effect) {
        if (!isOnline()) {
            return;
        }

        getPlayer().addPotionEffect(effect);
    }

    public void playSound(Sound sound, float volume, float pitch) {
        if (!isOnline()) {
            return;
        }

        Player player = getPlayer();

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setMultiplier(Multiplier multiplier) {
        this.multiplier = multiplier;
    }

    public Multiplier getMultiplier() {
        return multiplier;
    }

    public Coin getRealCoins() {
        return realCoins;
    }

    public Coin getPlayCoins() {
        return playCoins;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public void setCoins(Mode mode, Coin coin) {
        if (mode == Mode.REAL_MONEY) {
            realCoins = coin;

            if (isOnline()) {
                ScoreboardManager.update(this);
            }

            return;
        }

        playCoins = coin;

        if (isOnline()) {
            ScoreboardManager.update(this);
        }
    }

    public Coin getCoins(Mode mode) {
        if (mode == Mode.REAL_MONEY) {
            return realCoins;
        }

        return playCoins;
    }

    public boolean hasEnoughCoins(Coin coin, Mode mode) {
        if (mode == Mode.REAL_MONEY) {
            return !realCoins.isLessThan(coin);
        }

        return !playCoins.isLessThan(coin);
    }

    public void setClaimCooldown(Duration claimCooldown) {
        this.claimCooldown = claimCooldown;
    }

    public void setLastClaimed(Instant lastClaimed) {
        this.lastClaimed = lastClaimed;
    }

    public Duration getClaimCooldown() {
        Duration cooldownRemaining;

        if (lastClaimed != null) {
            cooldownRemaining = claimCooldown.minus(Duration.between(lastClaimed, Instant.now()));
        } else {
            cooldownRemaining = claimCooldown.minus(Duration.between(getStats().getLastJoin(), Instant.now()));
        }

        if (cooldownRemaining.compareTo(Duration.ZERO) <= 0) {
            return Duration.ZERO;
        }

        return cooldownRemaining;
    }

    public void setLastRefilled(Instant lastRefilled) {
        this.lastRefilled = lastRefilled;
    }

    public Instant getLastRefilled() {
        return lastRefilled;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
}
