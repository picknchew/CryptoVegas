package cc.cryptovegas.core.commands.game;

import cc.cryptovegas.core.AdminIPStore;
import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.game.blackjack.BlackJackGame;
import cc.cryptovegas.core.game.blackjack.BlackJackManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
public class BlackJackCommand extends RDCommand {
    private final BlackJackManager manager;
    private final Set<Player> interacting = new HashSet<>();
    private final Map<Player, BlackJackGame> editing = new HashMap<>();

    public BlackJackCommand(BlackJackManager manager) {
        super("blackjack");
        this.manager = manager;
    }

    private Single<Block> awaitInteraction(Player player, boolean checkEditing) {
        interacting.add(player);

        Observable<Block> observable = getPlugin().observeEvent(PlayerInteractEvent.class)
                .filter(event -> player == event.getPlayer())
                .doOnNext(event -> event.setCancelled(true))
                .doOnNext(event -> interacting.remove(event.getPlayer()))
                .map(event -> {
                    // just in case the game was removed while editing the game.
                    if (checkEditing && !editing.containsKey(player)) {
                        return null;
                    }

                    Block block = event.getClickedBlock();

                    if (block.getType() != Material.SIGN_POST && block.getType() != Material.WALL_SIGN) {
                        player.sendMessage(ChatColor.RED + "That was not a sign!");
                        return null;
                    }

                    return block;
                });

        return observable.take(1).toSingle();
    }

    @Override
    public void onCommandRegister() {
        Core core = (Core) getPlugin();

        core.observeEvent(PlayerQuitEvent.class)
                .map(PlayerEvent::getPlayer)
                .doOnNext(interacting::remove)
                .doOnNext(editing::remove)
                .subscribe();

        registerSubCommand(new CreateCommand(), new EditCommand(), new AddSignCommand(), new RemoveSignCommand(), new DeleteCommand(), new ResetCommand());
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommand(Player player, String[] args) throws CommandException {
        player.sendMessage(ChatColor.RED + "/blackjack create\n"
                + "/blackjack edit\n"
                + "/blackjack addsign\n"
                + "/blackjack removesign\n"
                + "/blackjack delete\n"
                + "/blackjack reset");
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
                player.sendMessage(ChatColor.RED + "You are already using a blackjack modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the sign you want to be the dealer sign.");

            awaitInteraction(player, false).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                if (manager.isSignUsed(sign)) {
                    player.sendMessage(ChatColor.RED + "That sign is already associated with a blackjack game!");
                    return;
                }

                BlackJackGame game = manager.createGame(sign);

                player.sendMessage(ChatColor.GREEN + "Successfully created a game!");
                editing.put(player, game);
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
                player.sendMessage(ChatColor.RED + "You are already using a blackjack modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the game you wish to reset.");

            awaitInteraction(player, false).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                BlackJackGame game = manager.getGameBySign(sign);

                if (game == null) {
                    player.sendMessage(ChatColor.RED + "That sign is not associated with a game.");
                    return;
                }

                game.resetGame();
                player.sendMessage(ChatColor.GREEN + "You have selected this game to be edited.");
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
                player.sendMessage(ChatColor.RED + "You are already using a blackjack modification command.");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on a sign associated with a game.");

            awaitInteraction(player, false).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                BlackJackGame game = manager.getGameBySign(sign);

                if (game == null) {
                    player.sendMessage(ChatColor.RED + "That sign is not associated with a game.");
                    return;
                }

                editing.put(player, game);
                player.sendMessage(ChatColor.GREEN + "You have selected this game to be edited.");
            });
        }
    }

    private class AddSignCommand extends RDCommand {

        private AddSignCommand() {
            super("addsign");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a blackjack modification command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "You must be selecting a game to edit it. Use /blackjack edit");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the sign you wish to be added.");

            awaitInteraction(player, true).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                BlackJackGame game = editing.get(player);

                if (manager.getGameBySign(sign) != null) {
                    player.sendMessage(ChatColor.RED + "That sign is already in use!");
                    return;
                }

                game.addSign(sign);
                manager.saveGames();
                player.sendMessage(ChatColor.GREEN + "You have added that sign to the game!");
            });
        }
    }

    private class RemoveSignCommand extends RDCommand {

        private RemoveSignCommand() {
            super("removesign");
        }

        @Override
        protected void handleCommand(Player player, String[] args) throws CommandException {
            if (!AdminIPStore.contains(player.getAddress().getHostString())) {
                player.sendMessage(ChatColor.RED + "You are unauthorized to use this command.");
                return;
            }

            if (interacting.contains(player)) {
                player.sendMessage(ChatColor.RED + "You are already using a blackjack modification command.");
                return;
            }

            if (!editing.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "You must be selecting a game to edit it. Use /blackjack edit");
                return;
            }

            player.sendMessage(ChatColor.AQUA + "Right-click on the sign you wish to be removed.");

            awaitInteraction(player, true).subscribe(sign -> {
                if (sign == null) {
                    return;
                }

                BlackJackGame game = editing.get(player);

                if (game.isDealerSign(sign)) {
                    player.sendMessage(ChatColor.GREEN + "You cannot remove the dealer sign from the game!");
                    return;
                }

                if (!game.isSignUsed(sign)) {
                    player.sendMessage(ChatColor.GREEN + "That sign is not used by the game that you are editing");
                    return;
                }

                game.removeSign(sign);
                manager.saveGames();
                player.sendMessage(ChatColor.GREEN + "You have removed that sign from the game!");
            });
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
                player.sendMessage(ChatColor.RED + "You must be selecting a game to edit it. Use /blackjack edit");
                return;
            }

            BlackJackGame game = editing.get(player);

            manager.deleteGame(game);
            manager.saveGames();

            for (Map.Entry<Player, BlackJackGame> entry : editing.entrySet()) {
                if (entry.getValue() == game) {
                    editing.remove(entry.getKey());
                }
            }

            player.sendMessage(ChatColor.GREEN + "You have successfully deleted the selected game.");
        }
    }
}
