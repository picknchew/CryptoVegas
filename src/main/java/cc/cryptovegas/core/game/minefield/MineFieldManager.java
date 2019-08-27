package cc.cryptovegas.core.game.minefield;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MineFieldManager {
    private final Core core;

    @ResourceFile(raw = true, filename = "internals/minefield.yml")
    private YAMLConfigurationFile file;

    private final Set<MineFieldGame> games = new HashSet<>();

    public MineFieldManager(Core core) {
        this.core = core;
        core.getResourceFileGraph().addObject(this);

        if (file.getConfig().contains("games")) {
            for (String key : file.getConfig().getConfigurationSection("games").getKeys(false)) {
                Block dealerSign = ((SerializableLocation) file.getConfig().get("games." + key + ".sign")).get().getBlock();
                Set<Block> blocks = ((List<?>) file.getConfig().getList("games." + key + ".blocks")).stream()
                        .map(object -> (SerializableLocation) object)
                        .map(SerializableLocation::get)
                        .map(Location::getBlock)
                        .collect(Collectors.toSet());

                games.add(new MineFieldGame(core, dealerSign, blocks, file.getConfig().getDouble("games." + key + ".chance")));
            }
        }
    }

    public void createGame(Block sign) {
        games.add(new MineFieldGame(core, sign));
        saveGames();
    }

    public void deleteGame(MineFieldGame game) {
        games.remove(game);
        game.delete();
        saveGames();
    }

    public Optional<MineFieldGame> getGameByPlayer(Player player) {
        return games.stream().filter(game -> game.getCurrentlyPlaying() == player).findFirst();
    }

    public MineFieldGame getGameBySign(Block sign) {
        for (MineFieldGame game : games) {
            if (sign.equals(game.getDealerSign())) {
                return game;
            }
        }

        return null;
    }

    public void saveGames() {
        int index = 0;

        for (MineFieldGame game : games) {
            SerializableLocation sign = new SerializableLocation(game.getDealerSign().getLocation());
            List<SerializableLocation> blocks = game.getBlocks().stream()
                    .map(Block::getLocation).map(SerializableLocation::new)
                    .collect(Collectors.toList());

            file.getConfig().set("games." + index + ".sign", sign);
            file.getConfig().set("games." + index + ".blocks", blocks);
            file.getConfig().set("games." + index + ".chance", game.getChance());
            index++;
        }

        file.saveConfig();
    }
}
