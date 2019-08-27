package cc.cryptovegas.core.player;

import org.bukkit.event.player.PlayerJoinEvent;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.plugin.RedemptivePlugin;

@Injectable
public class JoinListeners {
    private final RedemptivePlugin plugin;

    @InjectionProvider
    public JoinListeners(RedemptivePlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.observeEvent(PlayerJoinEvent.class)
                .doOnNext(event -> event.setJoinMessage(""))
                .filter(event -> !event.getPlayer().hasPlayedBefore())
                .subscribe(event -> event.setJoinMessage(plugin.formatAt("first-join").with("player", event.getPlayer().getName()).get()));

        plugin.observeEvent(PlayerJoinEvent.class)
                .doOnNext(event -> event.setJoinMessage(""))
                .filter(event -> event.getPlayer().hasPlayedBefore())
                .subscribe(event -> event.setJoinMessage(plugin.formatAt("join").with("player", event.getPlayer().getName()).get()));
    }
}
