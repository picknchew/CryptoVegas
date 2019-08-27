package cc.cryptovegas.core.npc;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.npc.action.Action;
import cc.cryptovegas.core.util.MojangAPI;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.inventivetalent.mcwrapper.auth.GameProfileWrapper;
import org.inventivetalent.mcwrapper.auth.properties.PropertyWrapper;
import org.inventivetalent.nicknamer.api.SkinLoader;
import org.inventivetalent.npclib.NPCLib;
import org.inventivetalent.npclib.event.NPCDamageEvent;
import org.inventivetalent.npclib.npc.living.human.NPCPlayer;
import org.inventivetalent.npclib.registry.NPCRegistry;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class NPCManager {
    private final Core core;
    private final NPCRegistry registry;

    @ResourceFile(raw = true, filename = "internals/npcs.yml")
    private YAMLConfigurationFile file;

    @ResourceFile(raw = true, filename = "internals/skins.yml")
    private YAMLConfigurationFile skins;

    private Map<Entity, SerializableNPC> npcs = new HashMap<>();

    public NPCManager(Core core) {
        this.core = core;
        this.registry = NPCLib.createRegistry(core);

        core.getResourceFileGraph().addObject(this);

        init();
    }

    private void init() {
        if (file.getConfig().contains("npcs")) {
            for (SerializableNPC serializableNPC : (List<SerializableNPC>) file.getConfig().get("npcs")) {
                Location location = serializableNPC.getLocation();
                NPCPlayer npc = registry.spawnPlayerNPC(location, NPCPlayer.class, new GameProfileWrapper(UUID.randomUUID(), serializableNPC.getName()));
                UUID skinUUID = serializableNPC.getUniqueId();

                npc.setYaw(location.getYaw());
                npc.setPitch(location.getPitch());
                npc.setPersistent(false);

                if (skinUUID != null) {
                    setSkin(npc, skinUUID);
                }

                npcs.put(npc.getBukkitEntity(), serializableNPC);
            }
        }

        core.observeEvent(EventPriority.HIGH, true, PlayerInteractEntityEvent.class)
                .doOnNext(event -> {
                    Entity entity = event.getRightClicked();

                    if (!NPCLib.isNPC(entity)) {
                        return;
                    }

                    SerializableNPC serializableNPC = getSerializableNPC(entity);

                    if (serializableNPC == null) {
                        return;
                    }

                    if (serializableNPC.getActionType() != Action.Type.NONE) {
                        serializableNPC.getAction().execute(event.getPlayer());
                    }

                    event.setCancelled(true);
                })
                .subscribe();

        core.observeEvent(NPCDamageEvent.class).doOnNext(event -> event.setCancelled(true)).subscribe();
    }

    public void removeNPC(NPCPlayer npc) {
        npcs.remove(npc.getBukkitEntity());
        npc.despawn();
    }

    public void setSkin(NPCPlayer npc, String skinOwner) {
        GameProfileWrapper profile = SkinLoader.loadSkin(skinOwner);
        Collection<PropertyWrapper> values = profile.getProperties().values();

        if (values.isEmpty()) {
            return;
        }

        PropertyWrapper property = values.iterator().next();
        UUID uuid = profile.getId();

        if (!skins.getConfig().contains(uuid.toString())) {
            skins.getConfig().set(uuid.toString(), new SerializableSkin(property));
            skins.saveConfig();
        }

        npc.setSkinTexture(property.getValue(), property.getSignature());
        getSerializableNPC(npc.getBukkitEntity()).setSkin(uuid);
        npc.updateNearby();
    }

    public void setSkin(NPCPlayer npc, UUID skinUUID) {
        PropertyWrapper property;

        if (skins.getConfig().contains(skinUUID.toString())) {
            property = ((SerializableSkin) skins.getConfig().get(skinUUID.toString())).get();
        } else {
            try {
                property = MojangAPI.getTextures(skinUUID.toString().replace("-", ""));
                skins.getConfig().set(skinUUID.toString(), new SerializableSkin(property));
                skins.saveConfig();
            } catch (ExecutionException e) {
                return;
            }
        }

        npc.setSkinTexture(property.getValue(), property.getSignature());
    }

    public NPCRegistry getRegistry() {
        return registry;
    }

    public SerializableNPC getSerializableNPC(Entity entity) {
        return npcs.get(entity);
    }

    public NPCPlayer createNPC(String name, Location location) {
        NPCPlayer npc = registry.spawnPlayerNPC(location, NPCPlayer.class, new GameProfileWrapper(UUID.randomUUID(), name));
        npc.setPitch(location.getPitch());
        npc.setYaw(location.getYaw());
        npc.setPersistent(false);

        Entity entity = npc.getBukkitEntity();
        npcs.put(entity, new SerializableNPC(null, entity.getName(), new SerializableLocation(entity.getLocation())));
        save();

        return npc;
    }

    public void disable() {
        registry.destroy();
    }

    public void save() {
        file.getConfig().set("npcs", new ArrayList<>(npcs.values()));
        file.saveConfig();
    }
}
