package cc.cryptovegas.core;

import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

@Injectable
public class SpawnManager {
    private final Core core;

    @ResourceFile(raw = true, filename = "internals/spawn.yml")
    private YAMLConfigurationFile file;

    private SerializableLocation spawnLocation;

    @InjectionProvider
    public SpawnManager(Core core) {
        this.core = core;

        core.getResourceFileGraph().addObject(this);

        if (!file.getConfig().contains("location")) {
            setSpawn(Bukkit.getWorld("world").getSpawnLocation());
        } else {
            spawnLocation = (SerializableLocation) file.getConfig().get("location");
        }

        registerListeners();
    }

    private void registerListeners() {
        core.observeEvent(PlayerJoinEvent.class)
                .map(PlayerEvent::getPlayer)
                .filter(player -> !player.hasPlayedBefore())
                .doOnNext(player -> player.teleport(spawnLocation.get()))
                .subscribe();
    }

    public void teleportToSpawn(Player player) {
        player.teleport(spawnLocation.get());
    }

    public void setSpawn(Location location) {
        spawnLocation = new SerializableLocation(location);
        file.getConfig().set("location", spawnLocation);
        file.saveConfig();
    }
}
