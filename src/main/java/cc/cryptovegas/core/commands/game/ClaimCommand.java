package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.ClaimCooldownStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.Mode;
import cc.cryptovegas.core.cryptocurrency.model.Coin;
import cc.cryptovegas.core.player.CryptoPlayer;
import cc.cryptovegas.core.util.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.RDCommand;

import java.time.Duration;

public class ClaimCommand extends RDCommand {
    private Core plugin;
    private Duration cooldown;
    private Coin claimAmount;

    public ClaimCommand() {
        super("claim");
    }

    @Override
    public void onCommandRegister() {
        plugin = (Core) getPlugin();
        cooldown = Duration.ofMinutes(plugin.getConfig().getLong("claim-cooldown"));
        claimAmount = Coin.parseCoin(plugin.getConfig().getString("claim-amount"));
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        CryptoPlayer cryptoPlayer = plugin.getPlayer(player);
        String ip = player.getAddress().getAddress().getHostAddress();

        Duration durationPlayer = cryptoPlayer.getClaimCooldown();

        if (!ClaimCooldownStore.canClaim(ip, cryptoPlayer.getUniqueId().toString())) {
            player.sendMessage(ChatColor.RED + "Your ip is already associated with another account.");
            return;
        }

        if (durationPlayer != Duration.ZERO) {
            long minutes = durationPlayer.toMinutes();
            long seconds = durationPlayer.minusMinutes(minutes).getSeconds();

            player.sendMessage(plugin.formatAt("claim-cooldown").with("minutes", minutes).with("seconds", seconds).get());
            return;
        }

        player.sendMessage(plugin.formatAt("claim").with("coins", claimAmount.toFriendlyString()).get());
        cryptoPlayer.setCoins(Mode.REAL_MONEY, cryptoPlayer.getRealCoins().add(claimAmount));
        cryptoPlayer.setClaimCooldown(cooldown);
        PlayerUtil.incrRealBets(cryptoPlayer, plugin);

        plugin.getCurrencyHandler().addRealCoins(cryptoPlayer.getUniqueId().toString(), claimAmount);
    }
}
