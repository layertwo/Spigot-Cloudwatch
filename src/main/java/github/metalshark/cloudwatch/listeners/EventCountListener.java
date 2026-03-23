package github.metalshark.cloudwatch.listeners;

import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicLong;

public class EventCountListener implements Listener {

    protected final AtomicLong count = new AtomicLong(0);

    public double getCountAndReset() {
        return count.getAndSet(0);
    }

}
