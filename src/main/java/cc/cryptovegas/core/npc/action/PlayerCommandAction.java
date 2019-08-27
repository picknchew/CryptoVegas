package cc.cryptovegas.core.npc.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerCommandAction implements Action {
    private final String command;

    public PlayerCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(Player player) {
        Bukkit.dispatchCommand(player, command.replace("{{player}}", player.getName()));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("command", command);

        return map;
    }

    public static PlayerCommandAction deserialize(Map<String, Object> map) {
        return new PlayerCommandAction((String) map.get("command"));
    }
}
