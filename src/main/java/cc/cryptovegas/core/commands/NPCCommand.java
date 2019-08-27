package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.npc.NPCManager;
import cc.cryptovegas.core.npc.SerializableNPC;
import cc.cryptovegas.core.npc.action.Action;
import cc.cryptovegas.core.npc.action.ConsoleCommandAction;
import cc.cryptovegas.core.npc.action.PlayerCommandAction;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.inventivetalent.npclib.NPCLib;
import org.inventivetalent.npclib.npc.NPCAbstract;
import org.inventivetalent.npclib.npc.living.human.NPCPlayer;
import rx.Observable;
import rx.Single;
import tech.rayline.core.command.ArgumentRequirementException;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

import java.util.HashSet;
import java.util.Set;

@CommandPermission("cryptovegas.admin")
public class NPCCommand extends RDCommand {
    private NPCManager manager;
    private Set<Player> interacting = new HashSet<>();

    public NPCCommand() {
        super("npc");
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    public void onCommandRegister() {
        manager = ((Core) getPlugin()).getNPCManager();
        registerSubCommand(new SpawnCommand(), new SetNameCommand(), new SetSkinCommand(), new SetActionCommand(), new RemoveCommand());

        getPlugin().observeEvent(PlayerQuitEvent.class).map(PlayerEvent::getPlayer).doOnNext(interacting::remove).subscribe();
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(ChatColor.RED +
                "/npc spawn <name>\n" +
                "/npc setname <name>\n" +
                "/npc setaction <type> [action]\n" +
                "/npc setskin <name>\n" +
                "/npc remove");
    }

    private Single<NPCPlayer> awaitInteraction(Player player) {
        interacting.add(player);

        Observable<NPCPlayer> observable = getPlugin().observeEvent(PlayerInteractEntityEvent.class)
                .filter(event -> player == event.getPlayer())
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> interacting.remove(event.getPlayer()))
                .map(event -> {
                    NPCAbstract npc = NPCLib.getNPC(event.getRightClicked());

                    if (npc == null) {
                        player.sendMessage(ChatColor.RED + "That was not an NPC!");
                        return null;
                    }

                    return (NPCPlayer) npc;
                });

        return observable.take(1).toSingle();
    }

    private class SpawnCommand extends RDCommand {

        private SpawnCommand() {
            super("spawn");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (args.length < 1) {
                throw new ArgumentRequirementException("/npc spawn <name>");
            }

            manager.createNPC(ChatColor.translateAlternateColorCodes('&', args[0]), player.getLocation()).setPersistent(false);
        }
    }

    private class SetNameCommand extends RDCommand {

        private SetNameCommand() {
            super("setname");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (args.length < 1) {
                throw new ArgumentRequirementException("/npc setname <name>");
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using an NPC modifying command!!");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the NPC you want this name to be set to.");

            awaitInteraction(player).subscribe(npc -> {
                if (npc == null) {
                    return;
                }

                player.sendMessage(ChatColor.AQUA + "Name changed!");

                String name = ChatColor.translateAlternateColorCodes('&', args[0]);

                npc.setFullName(name);
                manager.getSerializableNPC(npc.getBukkitEntity()).setName(name);
                manager.save();

                npc.updateNearby(32, nearbyPlayer -> {
                    npc.respawnTo(nearbyPlayer);
                    return true;
                });
            });
        }
    }

    private class SetSkinCommand extends RDCommand {

        private SetSkinCommand() {
            super("setskin");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (args.length < 1) {
                throw new ArgumentRequirementException("/npc setskin <name>");
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using an NPC modifying command!!");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the NPC you want this skin to be set to.");

            awaitInteraction(player).subscribe(npc -> {
                if (npc == null) {
                    return;
                }

                player.sendMessage(ChatColor.AQUA + "Skin changed!");

                manager.setSkin(npc, args[0]);
                manager.save();
            });
        }
    }

    private class SetActionCommand extends RDCommand {
        private SetActionCommand() {
            super("setaction");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            Action.Type type;

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "That was not a valid action.");
                return;
            }

            try {
                type = Action.Type.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "That was not a valid action.");
                return;
            }

            if (type.hasRequirement() && args.length < 2) {
                throw new ArgumentRequirementException("/npc setaction <type> [action]");
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using an NPC modifying command!!");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the NPC you want this action to be set to.");

            awaitInteraction(player).subscribe(npc -> {
                if (npc == null) {
                    return;
                }

                SerializableNPC serializableNPC = manager.getSerializableNPC(npc.getBukkitEntity());

                serializableNPC.setActionType(type);

                StringBuilder builder = new StringBuilder(args[1]);

                for (int x = 2; x < args.length; x++) {
                    builder.append(" ");
                    builder.append(args[x]);
                }

                switch (type) {
                    case NONE:
                        serializableNPC.setAction(null);
                        break;
                    case CONSOLE:
                        serializableNPC.setAction(new ConsoleCommandAction(builder.toString()));
                        break;
                    case PLAYER:
                        serializableNPC.setAction(new PlayerCommandAction(builder.toString()));
                        break;
                }

                manager.save();

                player.sendMessage(ChatColor.AQUA + "Action changed!");
            });
        }
    }

    private class RemoveCommand extends RDCommand {
        private RemoveCommand() {
            super("remove");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using an NPC modifying command!!");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the NPC you want to remove.");

            awaitInteraction(player).subscribe(npc -> {
                if (npc == null) {
                    return;
                }

                manager.removeNPC(npc);
                manager.save();

                player.sendMessage(ChatColor.AQUA + "Action changed!");
            });
        }
    }
}
