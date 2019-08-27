package cc.cryptovegas.core.gui;

import cc.cryptovegas.core.Core;
import org.bukkit.entity.Player;
import tech.rayline.core.gui.InventoryGUI;

public abstract class GUI {
    protected final Core core;

    private final String title;
    private final int size;

    final InventoryGUI gui;

    public GUI(Core core, String title, int size) {
        this.core = core;
        this.title = title;
        this.size = size;
        this.gui = new InventoryGUI(core, size, title);

        populateInventory();
        gui.updateInventory();
    }

    public void open(Player player) {
        gui.openFor(player);
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public InventoryGUI getInventoryGUI() {
        return gui;
    }

    protected abstract void populateInventory();
}
