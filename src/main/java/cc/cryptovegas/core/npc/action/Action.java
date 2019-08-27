package cc.cryptovegas.core.npc.action;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

public interface Action extends ConfigurationSerializable {
    void execute(Player player);

    enum Type {
        NONE(false), CONSOLE(true), PLAYER(true);

        private final boolean requirement;

        Type(boolean requirement) {
            this.requirement = requirement;
        }

        public boolean hasRequirement() {
            return requirement;
        }
    }
}
