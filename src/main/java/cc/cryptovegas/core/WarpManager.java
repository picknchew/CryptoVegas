package cc.cryptovegas.core;

import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Injectable
public class WarpManager {
    private final Map<String, Location> warps = new HashMap<>();

    @ResourceFile(raw = true, filename = "warps.yml")
    private YAMLConfigurationFile file;

    @InjectionProvider
    public WarpManager(Core core) {
        core.getResourceFileGraph().addObject(this);

        file.getConfig().getConfigurationSection("").getKeys(false).forEach(key -> {
            warps.put(key, ((SerializableLocation) file.getConfig().get(key)).get());
        });
    }

    public String getWarps() {
        return warps.keySet().stream().collect(Collectors.joining(", "));
    }

    public void setWarp(String name, Location location) {
        warps.put(name, location);
        file.getConfig().set(name, new SerializableLocation(location));
        file.saveConfig();
    }

    public boolean deleteWarp(String name) {
        Map.Entry<String, Location> warp = getWarp(name);

        if (warp == null) {
            return false;
        }

        warps.remove(warp.getKey());
        file.getConfig().set(name, null);
        file.saveConfig();

        return true;
    }

    public Map.Entry<String, Location> getWarp(String name) {
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry;
            }
        }

        return null;
    }
}
