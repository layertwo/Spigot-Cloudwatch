package github.metalshark.cloudwatch.listeners;

import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicLong;

public class EventCountListener implements Listener {

    private final AtomicLong count = new AtomicLong(0);

    protected void increment() {
        count.incrementAndGet();
    }

    public double getCountAndReset() {
        return count.getAndSet(0);
    }

}
