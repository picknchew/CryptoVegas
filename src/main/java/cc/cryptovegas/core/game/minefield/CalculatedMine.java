package cc.cryptovegas.core.game.minefield;

import org.bukkit.block.Block;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CalculatedMine {
    private final ThreadLocal<SecureRandom> random = ThreadLocal.withInitial(SecureRandom::new);
    private final Set<Block> mines = new HashSet<>();

    public CalculatedMine(double chance, Set<Block> blocks) {
        blocks.forEach(block -> {
            if (shouldBeMine(chance)) {
                mines.add(block);
            }
        });
    }

    public Set<Block> getMines() {
        return mines;
    }

    public boolean isMine(Block block) {
        return mines.contains(block);
    }

    private boolean shouldBeMine(double chance) {
        double generated = random.get().nextDouble();

        return generated < chance;
    }

    @Override
    public String toString() {
        return mines.stream()
                .map(Block::getLocation)
                .map(location -> location.getX() + "," + location.getY() + "," + location.getZ())
                .collect(Collectors.joining("-"));
    }
}
