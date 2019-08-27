package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.game.roulette.Number;
import cc.cryptovegas.core.game.roulette.RouletteGame;
import cc.cryptovegas.core.game.roulette.RouletteManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rx.Observable;
import rx.Single;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CommandPermission("cryptovegas.admin")
public class RouletteCommand extends RDCommand {
    private final RouletteManager manager;
    private final Map<Player, RouletteGame> editing = new HashMap<>();
    private final Set<Player> interacting = new HashSet<>();
    private final Set<Player> removing = new HashSet<>();

    public RouletteCommand(RouletteManager manager) {
        super("roulette");
        this.manager = manager;
    }

    @Override
    public void onCommandRegister() {
        Core core = (Core) getPlugin();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> removing.contains(event.getPlayer()))
                .doOnNext(event -> {
                    Player player = event.getPlayer();
                    Block block = event.getClickedBlock();

                    RouletteGame game = editing.get(player);

                    if (!game.isBlockUsed(block)) {
                        player.sendMessage(ChatColor.RED + "That block is not used by the selected game.");
                    }

                    game.removeBlock(block);
                    manager.saveGames();
                    player.sendMessage(ChatColor.GREEN + "The block was removed from the game.");
                }).subscribe();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
                .filter(event -> removing.contains(event.getPlayer()))
                .doOnNext(event -> {
                    Player player = event.getPlayer();

                    removing.remove(player);
                    player.sendMessage(ChatColor.RED + "You have stopped removing blocks.");
                }).subscribe();

        core.observeEvent(PlayerQuitEvent.class)
                .map(PlayerEvent::getPlayer)
                .doOnNext(player -> {
                    editing.remove(player);
                    interacting.remove(player);
                    removing.remove(player);
                }).subscribe();

        registerSubCommand(new EditCommand(), new CreateCommand(), new AddCommand(), new RemoveCommand(), new DeleteCommand());
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(ChatColor.RED + "/roulette create\n"
                + "/roulette edit\n"
                + "/roulette addblocks\n"
                + "/roulette removeblocks\n"
                + "/roulette setchance\n"
                + "/roulette delete");
    }

    private Single<Block> awaitInteraction(Player player) {
        interacting.add(player);

        Observable<Block> observable = getPlugin().observeEvent(PlayerInteractEvent.class)
                .filter(event -> player == event.getPlayer())
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> interacting.remove(event.getPlayer()))
                .map(PlayerInteractEvent::getClickedBlock);

        return observable.take(1).toSingle();
    }

    private class EditCommand extends RDCommand {

        private EditCommand() {
            super("edit");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a minefield modifcation command.");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Right-click on a sign to select a game.");

            awaitInteraction(player).subscribe((sign) -> {
                RouletteGame game = manager.getGameBySign(sign);

                if (game == null) {
                    player.sendMessage(ChatColor.RED + "That sign is not a valid game.");
                    return;
                }

                editing.put(player, game);
                player.sendMessage(ChatColor.GREEN + "You have successfully selected a game.");
            });
        }
    }

    private class CreateCommand extends RDCommand {

        private CreateCommand() {
            super("create");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a minefield modifcation command.");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Right-click on a sign to create a game.");

            awaitInteraction(player).subscribe((sign) -> {
                if (manager.getGameBySign(sign) != null) {
                    player.sendMessage(ChatColor.RED + "That sign is already in use!");
                    return;
                }

                editing.put(player, manager.createGame(sign));
                manager.saveGames();
                player.sendMessage(ChatColor.GREEN + "You have successfully created a roulette game!");
            });
        }
    }

    private class AddCommand extends RDCommand {

        private AddCommand() {
            super("add");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (args.length != 2) {
                player.sendMessage("/roulette add <number> <color>");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a roulette modifcation command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /roulette edit");
                return;
            }

            int number;

            try {
                number = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + args[0] + " is not a valid number!");
                return;
            }

            RouletteGame.Color color;

            try {
                color = RouletteGame.Color.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + args[1] + " is not a valid color!");
                return;
            }

            player.sendMessage(ChatColor.RED + "Right-click to set the block as " + args[0] + " " + color.toString());

            RouletteGame game = editing.get(player);

            awaitInteraction(player).subscribe((block) -> {
                if (game.isBlockUsed(block)) {
                    player.sendMessage(ChatColor.RED + "That block is already being used by the game!");
                    return;
                }

                game.addNumber(block, new Number(number, color));
                manager.saveGames();
                player.sendMessage(ChatColor.GREEN + "You have successfully added that block to the game.");
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
                player.sendMessage(ChatColor.RED + "You are already using a roulette modifcation command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /roulette edit");
                return;
            }

            removing.add(player);
            player.sendMessage(ChatColor.GREEN + "Right-click to remove blocks. Left-click to stop removing blocks.");
        }
    }

    private class DeleteCommand extends RDCommand {

        private DeleteCommand() {
            super("delete");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /roulette edit");
                return;
            }

            manager.deleteGame(editing.get(player));
            manager.saveGames();

            player.sendMessage(ChatColor.RED + "You have successfully deleted the game.");
        }
    }
}
