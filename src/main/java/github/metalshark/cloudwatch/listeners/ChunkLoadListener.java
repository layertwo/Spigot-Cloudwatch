package github.metalshark.cloudwatch.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.concurrent.atomic.AtomicLong;

public class ChunkLoadListener implements Listener {

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong max = new AtomicLong(0);

    public ChunkLoadListener init() {
        for (World world : Bukkit.getWorlds()) {
            count.addAndGet(world.getLoadedChunks().length);
        }
        max.set(count.get());
        return this;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void onChunkLoad(ChunkLoadEvent event) {
        long c = count.incrementAndGet();
        max.accumulateAndGet(c, Math::max);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void onChunkUnload(ChunkUnloadEvent event) {
        count.decrementAndGet();
    }

    public double getMaxAndReset() {
        return max.getAndSet(count.get());
    }

}
