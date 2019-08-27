package cc.cryptovegas.core.scoreboard;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ReadOnlyResource;
import tech.rayline.core.parse.ResourceFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Injectable
public class ScoreboardManager {
    private static ScoreboardTemplate scoreboardTemplate;
    private static final Map<CryptoPlayer, SimpleScoreboard> scoreboards = new HashMap<>();

    @ResourceFile(filename = "scoreboard.yml")
    @ReadOnlyResource
    private ScoreboardConfiguration config;

    @InjectionProvider
    public ScoreboardManager(Core core) {
        core.getResourceFileGraph().addObject(this);

        scoreboardTemplate = config.getTemplate();

        core.observeEvent(PlayerJoinEvent.class)
                .map(event -> core.getPlayer(event.getPlayer()))
                .doOnNext(player -> {
                    if (player != null) {
                        addPlayer(player);
                    }
                })
                .subscribe();

        core.observeEvent(PlayerQuitEvent.class)
                .map(event -> core.getPlayer(event.getPlayer()))
                .doOnNext(this::removePlayer)
                .subscribe();
    }

    private void addPlayer(CryptoPlayer cryptoPlayer) {
        SimpleScoreboard scoreboard = config.getNewScoreboard();

        scoreboards.put(cryptoPlayer, scoreboard);
        scoreboard.send(cryptoPlayer.getPlayer());
        update(cryptoPlayer);
    }

    private void removePlayer(CryptoPlayer cryptoPlayer) {
        scoreboards.remove(cryptoPlayer);
    }

    public static void update(CryptoPlayer player) {
        SimpleScoreboard scoreboard = scoreboards.get(player);
        ScoreboardTemplate.Replacer replacer = scoreboardTemplate.new Replacer();
        replacer.with("play_coins", player.getPlayCoins().toFriendlyString());
        replacer.with("real_coins", player.getRealCoins().toFriendlyString());

        List<String> lines = replacer.build();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);

            scoreboard.add(line, index);
        }

        scoreboard.update();
    }
}
