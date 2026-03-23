package github.metalshark.cloudwatch.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.atomic.AtomicLong;

public class PlayerJoinListener implements Listener {

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong max = new AtomicLong(0);

    public PlayerJoinListener init() {
        for (World world : Bukkit.getWorlds()) {
            count.addAndGet(world.getPlayers().size());
        }
        max.set(count.get());
        return this;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void onPlayerJoin(PlayerJoinEvent event) {
        long c = count.incrementAndGet();
        max.accumulateAndGet(c, Math::max);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void onPlayerLeave(PlayerQuitEvent event) {
        count.decrementAndGet();
    }

    public double getMaxAndReset() {
        return max.getAndSet(count.get());
    }

}
