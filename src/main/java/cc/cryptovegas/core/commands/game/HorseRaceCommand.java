package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.game.horserace.HorseRaceManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rx.Observable;
import rx.Single;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

import java.util.HashSet;
import java.util.Set;

@CommandPermission("cryptovegas.admin")
public class HorseRaceCommand extends RDCommand {
    private final HorseRaceManager manager;
    private final Set<Player> interacting = new HashSet<>();

    public HorseRaceCommand(HorseRaceManager manager) {
        super("horserace");

        this.manager = manager;
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(ChatColor.RED + "/horserace set" +
                "\n/horserace remove" +
                "\n/horserace setcountdown" +
                "\n/horserace removecountdown" +
                "\n/horserace reset");
    }

    @Override
    public void onCommandRegister() {
        getPlugin().observeEvent(PlayerQuitEvent.class).doOnNext(event -> interacting.remove(event.getPlayer()));

        registerSubCommand(new SetCommand(), new RemoveCommand(), new SetCountdownCommand(), new RemoveCountdownCommand(), new ResetCommand());
    }

    private Single<Block> awaitInteraction(Player player) {
        interacting.add(player);

        Observable<Block> observable = getPlugin().observeEvent(PlayerInteractEvent.class)
                .filter(event -> player == event.getPlayer())
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> interacting.remove(event.getPlayer()))
                .map(event -> {
                    Block block = event.getClickedBlock();

                    if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
                        player.sendMessage(ChatColor.RED + "That was not a sign!");
                        return null;
                    }

                    return block;
                });

        return observable.take(1).toSingle();
    }

    private class SetCommand extends RDCommand {

        private SetCommand() {
            super("set");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "/horserace set <1-4>");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a horse race modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the sign you want to be a betting sign");

            int lane;

            try {
                lane = Integer.parseInt(args[0]);

                if (lane < 1 || lane > 4) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ignored) {
                player.sendMessage(ChatColor.RED + "An invalid lane was entered.");
                return;
            }

            awaitInteraction(player).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                if (manager.isSignUsed(sign)) {
                    player.sendMessage(ChatColor.RED + "That sign is already associated with a horse race game!");
                    return;
                }

                manager.setSign(sign, lane - 1);
                player.sendMessage(ChatColor.RED + "Successfully set that sign for betting.");
            });
        }
    }

    private class ResetCommand extends RDCommand {

        private ResetCommand() {
            super("reset");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a horse race modification command.");
                return;
            }

            manager.resetGame(true);
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
                player.sendMessage(ChatColor.RED + "You are already using a horse race modification command.");
                return;
            }

            player.sendMessage(ChatColor.RED + "Right-click to remove a sign from the game.");

            awaitInteraction(player).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                if (!manager.isSignUsed(sign)) {
                    player.sendMessage(ChatColor.RED + "That sign is not being used for horse race!");
                    return;
                }

                manager.removeSign(sign);
                player.sendMessage(ChatColor.RED + "Successfully removed the sign from the game.");
            });
        }
    }

    private class SetCountdownCommand extends RDCommand {

        private SetCountdownCommand() {
            super("setcountdown");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a horse race modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the sign you want to be a countdown sign");

            awaitInteraction(player).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                if (manager.isSignUsed(sign) || manager.isCountdownSign(sign)) {
                    player.sendMessage(ChatColor.RED + "That sign is already associated with a horse race game!");
                    return;
                }

                manager.setCountdownSign(sign);
                player.sendMessage(ChatColor.RED + "Successfully set that sign for countdown.");
            });
        }
    }

    private class RemoveCountdownCommand extends RDCommand {

        private RemoveCountdownCommand() {
            super("removecountdown");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a horse race modification command.");
                return;
            }

            player.sendMessage(ChatColor.RED + "Right-click to remove a sign from the game.");

            awaitInteraction(player).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                if (!manager.isCountdownSign(sign)) {
                    player.sendMessage(ChatColor.RED + "That sign is not being used for horse race!");
                    return;
                }

                manager.removeCountdownSign(sign);
                player.sendMessage(ChatColor.RED + "Successfully removed the sign from the game.");
            });
        }
    }
}
