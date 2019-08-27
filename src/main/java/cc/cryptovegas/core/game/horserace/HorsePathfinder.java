package cc.cryptovegas.core.game.horserace;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.inventivetalent.npclib.ai.AIAbstract;
import org.inventivetalent.npclib.npc.living.insentient.creature.ageable.animal.NPCHorse;

import java.util.List;

public class HorsePathfinder extends AIAbstract<NPCHorse> {
    private final double speed;
    private final List<Vector> points;
    private final PathFinishedListener listener;

    private boolean finished = false;

    private int currentIndex = 0;
    private int currentLength = 0;

    // speed in m/s
    public HorsePathfinder(List<Vector> points, double speed, PathFinishedListener listener) {
        this.points = points;
        // get speed per tick.
        this.speed = speed / 20;
        this.listener = listener;
    }

    @Override
    public void tick() {
        Vector entityVector = getNpc().getNpcEntity().getLocationVector().toBukkitVector();
        Vector vector = points.get(currentIndex).clone().subtract(entityVector);
        Entity entity = getNpc().getBukkitEntity();

        // magnitude
        double length = vector.length();

        vector.normalize();

        boolean shouldTick = false;

        if (currentLength + length >= speed ^ (currentIndex == points.size() - 1 && length < 1)) {
            vector.multiply(speed - currentLength);
            currentLength = 0;
        } else {
            currentLength += length;
            vector.multiply(length);
            this.currentIndex++;

            if (currentIndex == points.size()) {
                finished = true;

                if (listener != null) {
                    listener.onPathFinish();
                }
            } else {
                shouldTick = true;
            }
        }

        entity.setVelocity(vector);

        float yaw = (float) (Math.toDegrees(Math.atan2(vector.getZ(), vector.getX())) - 90.0F);

        getNpc().getNpcEntity().setYaw(yaw);

        if (shouldTick) {
            tick();
            getNpc().invokeNPCMethod("tickEntity");
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    public interface PathFinishedListener {
        void onPathFinish();
    }
}
