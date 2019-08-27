package cc.cryptovegas.core.npc;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.inventivetalent.mcwrapper.auth.properties.PropertyWrapper;

import java.util.HashMap;
import java.util.Map;

public class SerializableSkin implements ConfigurationSerializable {
    private final PropertyWrapper wrapper;

    public SerializableSkin(PropertyWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public PropertyWrapper get() {
        return wrapper;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("value", wrapper.getValue());
        map.put("signature", wrapper.getSignature());

        return map;
    }

    public static SerializableSkin deserialize(Map<String, Object> map) {
        return new SerializableSkin(new PropertyWrapper("textures", (String) map.get("value"), (String) map.get("signature")));
    }
}
