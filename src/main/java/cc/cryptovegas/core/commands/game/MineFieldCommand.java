package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.game.minefield.MineFieldGame;
import cc.cryptovegas.core.game.minefield.MineFieldManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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
public class MineFieldCommand extends RDCommand {
    private final MineFieldManager manager;
    private final Map<Player, InteractAction> actions = new HashMap<>();
    private final Map<Player, MineFieldGame> editing = new HashMap<>();
    private final Set<Player> interacting = new HashSet<>();

    public MineFieldCommand(MineFieldManager manager) {
        super("minefield");
        this.manager = manager;
    }

    @Override
    public void onCommandRegister() {
        Core core = (Core) getPlugin();

        core.observeEvent(PlayerQuitEvent.class).doOnNext(event -> {
            Player player = event.getPlayer();

            actions.remove(player);
            editing.remove(player);
            interacting.remove(player);
        }).subscribe();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR)
                .filter(event -> actions.remove(event.getPlayer()) != null)
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> event.getPlayer().sendMessage(ChatColor.RED + "You have stopped adding/removing blocks."))
                .subscribe();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> actions.containsKey(event.getPlayer()))
                .filter(event -> actions.get(event.getPlayer()) == InteractAction.ADD_BLOCK)
                .doOnNext(event -> {
                    MineFieldGame game = editing.get(event.getPlayer());
                    Player player = event.getPlayer();

                    if (game.containsBlock(event.getClickedBlock())) {
                        player.sendMessage(ChatColor.RED + "That block is already in use!");
                        return;
                    }

                    game.addBlock(event.getClickedBlock());
                    manager.saveGames();
                    player.sendMessage(ChatColor.GREEN + "You added that block to the game.");
                }).subscribe();

        core.observeEvent(PlayerInteractEvent.class)
                .filter(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK)
                .filter(event -> actions.containsKey(event.getPlayer()))
                .filter(event -> actions.get(event.getPlayer()) == InteractAction.REMOVE_BLOCK)
                .doOnNext(event -> {
                    MineFieldGame game = editing.get(event.getPlayer());
                    Player player = event.getPlayer();

                    if (!game.containsBlock(event.getClickedBlock())) {
                        player.sendMessage(ChatColor.RED + "That block is not in use!");
                        return;
                    }

                    game.removeBlock(event.getClickedBlock());
                    manager.saveGames();
                    player.sendMessage(ChatColor.GREEN + "You removed that block to the game.");
                }).subscribe();

        registerSubCommand(new CreateCommand(), new EditCommand(), new AddBlocksCommand(), new RemoveBlocksCommand(), new SetChanceCommand(), new DeleteCommand(), new ResetCommand());
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(ChatColor.RED + "/minefield create\n"
                + "/minefield edit\n"
                + "/minefield addblocks\n"
                + "/minefield removeblocks\n"
                + "/minefield setchance\n"
                + "/minefield delete\n"
                + "/minefield reset");
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

    private enum InteractAction {
        REMOVE_BLOCK, ADD_BLOCK
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

                manager.createGame(sign);
                player.sendMessage(ChatColor.GREEN + "You have successfully created a minefield game!");
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
                player.sendMessage(ChatColor.RED + "You are already using a minefield modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the game you wish to reset.");

            awaitInteraction(player).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                MineFieldGame game = manager.getGameBySign(sign);

                if (game == null) {
                    player.sendMessage(ChatColor.RED + "That sign is not associated with a game.");
                    return;
                }

                game.endGame(true);
            });
        }
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
                MineFieldGame game = manager.getGameBySign(sign);

                if (game == null) {
                    player.sendMessage(ChatColor.RED + "That sign is not a valid game.");
                    return;
                }

                editing.put(player, game);
                player.sendMessage(ChatColor.GREEN + "You have successfully selected this game.");
            });
        }
    }

    private class AddBlocksCommand extends RDCommand {

        private AddBlocksCommand() {
            super("addblocks");
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

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /minefield edit");
                return;
            }

            actions.put(player, InteractAction.ADD_BLOCK);
            player.sendMessage(ChatColor.RED + "Right-click to add blocks. Left-click to stop adding blocks.");
        }
    }

    private class RemoveBlocksCommand extends RDCommand {

        private RemoveBlocksCommand() {
            super("removeblocks");
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

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /minefield edit");
                return;
            }

            actions.put(player, InteractAction.REMOVE_BLOCK);
            player.sendMessage(ChatColor.RED + "Right-click to remove blocks. Left-click to stop removing blocks.");
        }
    }

    private class SetChanceCommand extends RDCommand {

        private SetChanceCommand() {
            super("setchance");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /minefield edit");
                return;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "/minefield setchance <decimal %>");
                return;
            }

            try {
                double chance = Double.parseDouble(args[0]);

                if (chance > 1 || chance < 0) {
                    player.sendMessage(ChatColor.RED + "The chance must be below 1 and above 0.");
                    return;
                }

                editing.get(player).setChance(chance);
                manager.saveGames();
                player.sendMessage(ChatColor.GREEN + "You have set the chance of the selected game.");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "That was not a valid number!");
            }
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
                player.sendMessage(ChatColor.RED + "Select a game to edit by using /minefield edit");
                return;
            }

            manager.deleteGame(editing.remove(player));
            player.sendMessage(ChatColor.GREEN + "You have successfully deleted the selected game.");
        }
    }
}
