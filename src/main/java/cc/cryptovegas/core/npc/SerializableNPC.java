package cc.cryptovegas.core.npc;

import cc.cryptovegas.core.npc.action.Action;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SerializableNPC implements ConfigurationSerializable {
    private UUID skinUUID;
    private String name;
    private SerializableLocation location;
    private Action.Type actionType;
    private Action action;

    public SerializableNPC(UUID skinUUID, String name, SerializableLocation location) {
        this(skinUUID, name, location, Action.Type.NONE, null);
    }

    public SerializableNPC(UUID skinUUID, String name, SerializableLocation location, Action.Type actionType, Action action) {
        this.skinUUID = skinUUID;
        this.name = name;
        this.location = location;
        this.actionType = actionType;
        this.action = action;
    }

    public Action.Type getActionType() {
        return actionType;
    }

    public Action getAction() {
        return action;
    }

    public UUID getUniqueId() {
        return skinUUID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location.get();
    }

    public void setActionType(Action.Type actionType) {
        this.actionType = actionType;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setSkin(UUID uuid) {
        this.skinUUID = uuid;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("uuid", skinUUID != null ? skinUUID.toString() : null);
        map.put("name", name);
        map.put("location", location);
        map.put("action_type", actionType.toString());
        map.put("action", action);

        return map;
    }

    public static SerializableNPC deserialize(Map<String, Object> map) {
        UUID uuid = null;
        Object stringUUID = map.get("uuid");

        if (stringUUID != null) {
            uuid = UUID.fromString((String) stringUUID);
        }

        String name = (String) map.get("name");
        SerializableLocation location = (SerializableLocation) map.get("location");
        Action.Type type = Action.Type.valueOf((String) map.get("action_type"));
        Action action = (Action) map.get("action");

        return new SerializableNPC(uuid, name, location, type, action);
    }
}
