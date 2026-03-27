package github.metalshark.cloudwatch.runnables;

import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Samples total live entity count across all worlds on the main server thread.
 * Must be scheduled as a sync task so Bukkit world state is safe to read.
 * The async stats runnable reads the cached value via getEntityCount().
 */
public class EntityCountSampler implements Runnable {

    private final AtomicLong entityCount = new AtomicLong(0);

    @Override
    public void run() {
        entityCount.set(Bukkit.getWorlds().stream()
            .mapToLong(w -> w.getEntities().size())
            .sum());
    }

    public long getEntityCount() {
        return entityCount.get();
    }

}
