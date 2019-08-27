package cc.cryptovegas.core.util;

import cc.cryptovegas.core.Core;
import org.bukkit.scheduler.BukkitTask;
import tech.rayline.core.util.RunnableShorthand;

import java.util.function.Consumer;

public class CountdownTimer {
    private final Core core;
    private final long duration;
    private long durationRemaining;
    private Consumer<Long> consumer;
    private Runnable onFinish;

    private BukkitTask task;

    private CountdownTimer(Core core, long duration) {
        this.core = core;
        this.duration = this.durationRemaining = duration;
    }

    public CountdownTimer forEach(Consumer<Long> consumer) {
        this.consumer = consumer;
        return this;
    }

    public CountdownTimer onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    public void start() {
        task = RunnableShorthand.forPlugin(core).with(() -> {
            if (durationRemaining < 0) {
                cancel();
                return;
            }

            consumer.accept(durationRemaining--);
        }).repeat(20L);
    }

    public void reset() {
        durationRemaining = duration;
        task = null;
    }

    public void cancel() {
        task.cancel();

        if (onFinish != null) {
            onFinish.run();
        }
    }

    public static CountdownTimer create(Core core, long duration) {
        return new CountdownTimer(core, duration);
    }
}
