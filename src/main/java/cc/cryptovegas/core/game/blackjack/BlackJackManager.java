package cc.cryptovegas.core.game.blackjack;

import cc.cryptovegas.core.Core;
import cc.cryptovegas.core.util.SerializableLocation;
import org.bukkit.Location;
import org.bukkit.block.Block;
import tech.rayline.core.inject.Injectable;
import tech.rayline.core.inject.InjectionProvider;
import tech.rayline.core.parse.ResourceFile;
import tech.rayline.core.plugin.YAMLConfigurationFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Injectable
public class BlackJackManager {
    private final Core core;
    private final Set<BlackJackGame> games = new HashSet<>();

    @ResourceFile(raw = true, filename = "internals/blackjack.yml")
    private YAMLConfigurationFile file;

    @InjectionProvider
    public BlackJackManager(Core core) {
        this.core = core;
        core.getResourceFileGraph().addObject(this);

        if (file.getConfig().contains("games")) {
            for (String key : file.getConfig().getConfigurationSection("games").getKeys(false)) {
                Block dealerSign = ((SerializableLocation) file.getConfig().get("games." + key + ".dealer")).get().getBlock();
                Set<Block> signs = ((List<?>) file.getConfig().getList("games." + key + ".signs")).stream()
                        .map(object -> (SerializableLocation) object)
                        .map(SerializableLocation::get)
                        .map(Location::getBlock)
                        .collect(Collectors.toSet());

                games.add(new BlackJackGame(core, dealerSign, signs));
            }
        }
    }

    public BlackJackGame createGame(Block sign) {
        BlackJackGame game = new BlackJackGame(core, sign);
        games.add(game);
        saveGames();
        return game;
    }

    public void deleteGame(BlackJackGame game) {
        games.remove(game);
        game.delete();
        saveGames();
    }

    public BlackJackGame getGameBySign(Block sign) {
        for (BlackJackGame game : games) {
            if (game.isSignUsed(sign)) {
                return game;
            }
        }

        return null;
    }

    public boolean isSignUsed(Block sign) {
        for (BlackJackGame game : games) {
            if (game.isSignUsed(sign)) {
                return true;
            }
        }

        return false;
    }

    public void saveGames() {
        int index = 0;

        for (BlackJackGame game : games) {
            List<SerializableLocation> signs = game.getSigns().stream()
                    .map(Block::getLocation).map(SerializableLocation::new)
                    .collect(Collectors.toList());

            file.getConfig().set("games." + index + ".dealer", new SerializableLocation(game.getDealerSign().getLocation()));
            file.getConfig().set("games." + index + ".signs", signs);
            index++;
        }

        file.saveConfig();
    }
}
