package cc.cryptovegas.core.commands;

import cc.cryptovegas.core.Core;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import rx.Subscription;
import tech.rayline.core.command.CommandException;
import tech.rayline.core.command.CommandPermission;
import tech.rayline.core.command.RDCommand;

@CommandPermission("cryptovegas.lockdown")
public class LockdownCommand extends RDCommand {
    private Core core;
    private Subscription listener;

    public LockdownCommand() {
        super("lockdown");
    }

    @Override
    public void onCommandRegister() {
        this.core = (Core) getPlugin();
    }

    @Override
    protected boolean shouldGenerateHelpCommand() {
        return false;
    }

    @Override
    protected void handleCommandUnspecific(CommandSender sender, String[] args) throws CommandException {
        if (!core.isLockdown()) {
            sender.sendMessage(ChatColor.RED + "The server is now in lockdown.");

            listener = core.observeEvent(EventPriority.HIGH, PlayerMoveEvent.class)
                    .filter(event -> !event.getPlayer().hasPermission("cryptovegas.admin"))
                    .doOnNext(event -> event.setCancelled(true))
                    .subscribe();
        } else {
            sender.sendMessage(ChatColor.RED + "The server is no longer in lockdown.");
            listener.unsubscribe();
        }

        core.setLockdown(!core.isLockdown());
    }
}
