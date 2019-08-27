package cc.cryptovegas.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;

import java.util.concurrent.TimeUnit;

@Injectable
public class TeleportManager {
    private final Core core;
    private final Cache<Player, Player> requests = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build();

    @InjectionProvider
    public TeleportManager(Core core) {
        this.core = core;
        registerListeners();
    }

    private void registerListeners() {
        core.observeEvent(PlayerQuitEvent.class)
                .map(PlayerEvent::getPlayer)
                .doOnNext(requests::invalidate)
                .subscribe();
    }

    public void addRequest(Player player, Player target) {
        requests.put(player, target);
    }

    public boolean requestValid(Player player) {
        return requests.asMap().keySet().contains(player);
    }

    public Player consumeRequest(Player player) {
        Player target = requests.getIfPresent(player);

        target.teleport(player);
        requests.invalidate(player);

        return target;
    }
}
