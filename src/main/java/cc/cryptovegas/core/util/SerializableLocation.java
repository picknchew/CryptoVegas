package cc.cryptovegas.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class SerializableLocation implements ConfigurationSerializable {
    private final Location location;

    public SerializableLocation(World world, double x, double y, double z, float yaw, float pitch) {
        this(new Location(world, x, y, z, yaw, pitch));
    }

    public SerializableLocation(Location location) {
        this.location = location;
    }

    public Location get() {
        return location;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());

        return map;
    }

    public static SerializableLocation deserialize(Map<String, Object> map) {
        World world = Bukkit.getWorld((String) map.get("world"));
        double x = (double) map.get("x");
        double y = (double) map.get("y");
        double z = (double) map.get("z");
        float yaw = ((Number) map.get("yaw")).floatValue();
        float pitch = ((Number) map.get("pitch")).floatValue();

        return new SerializableLocation(world, x, y, z, yaw, pitch);
    }
}
