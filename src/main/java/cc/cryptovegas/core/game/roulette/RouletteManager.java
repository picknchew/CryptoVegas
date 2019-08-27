package cc.cryptovegas.core.game.roulette;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.HashMap;
import java.util.Map;

public class RouletteManager {
    @ResourceFile(raw = true, filename = "internals/roulette.yml")
    private YAMLConfigurationFile file;

    private final Core core;
    private final Map<Block, RouletteGame> games = new HashMap<>();

    public RouletteManager(Core core) {
        this.core = core;

        core.getResourceFileGraph().addObject(this);

        if (file.getConfig().contains("games")) {
            for (String key : file.getConfig().getConfigurationSection("games").getKeys(false)) {
                Block sign = ((SerializableLocation) file.getConfig().get("games." + key + ".sign")).get().getBlock();
                Map<Block, Number> numbers = new HashMap<>();

                for (String numbersKey : file.getConfig().getConfigurationSection("games." + key + ".numbers").getKeys(false)) {
                    Location location = ((SerializableLocation) file.getConfig().get("games." + key + ".numbers." + numbersKey + ".block")).get();
                    Number number = (Number) file.getConfig().get("games." + key + ".numbers." + numbersKey + ".number");

                    numbers.put(location.getBlock(), number);
                }

                games.put(sign, new RouletteGame(core, sign, numbers));
            }
        }
    }

    public RouletteGame createGame(Block sign) {
        RouletteGame game = new RouletteGame(core, sign, new HashMap<>());
        games.put(sign, game);
        return game;
    }

    public void deleteGame(RouletteGame game) {
        games.remove(game.getSign());
        game.delete();
    }

    public RouletteGame getGameBySign(Block sign) {
        return games.get(sign);
    }

    public void saveGames() {
        int index = 0;

        for (Map.Entry<Block, RouletteGame> entry : games.entrySet()) {
            Block sign = entry.getKey();
            RouletteGame game = entry.getValue();
            int numberIndex = 0;

            file.getConfig().set("games." + index + ".sign", new SerializableLocation(sign.getLocation()));

            for (Map.Entry<Block, Number> numberEntry : game.getNumbers().entrySet()) {
                Block key = numberEntry.getKey();
                Number number = numberEntry.getValue();

                file.getConfig().set("games." + index + ".numbers." + numberIndex + ".block", new SerializableLocation(key.getLocation()));
                file.getConfig().set("games." + index + ".numbers." + numberIndex + ".number", number);

                numberIndex++;
            }

            index++;
        }

        file.saveConfig();
    }
}
