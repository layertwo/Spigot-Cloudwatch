package github.metalshark.cloudwatch.listeners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventCountListenerTest {

    private EventCountListener listener;

    @BeforeEach
    void setUp() {
        listener = new EventCountListener();
    }

    @Test
    void getCountAndReset_returnsZeroInitially() {
        assertEquals(0, listener.getCountAndReset());
    }

    @Test
    void getCountAndReset_returnsAccumulatedCount() {
        listener.count.incrementAndGet();
        listener.count.incrementAndGet();
        listener.count.incrementAndGet();

        assertEquals(3, listener.getCountAndReset());
    }

    @Test
    void getCountAndReset_resetsToZeroAfterRead() {
        listener.count.incrementAndGet();
        listener.count.incrementAndGet();

        listener.getCountAndReset();
        assertEquals(0, listener.getCountAndReset());
    }

    @Test
    void getCountAndReset_handlesMultipleCycles() {
        listener.count.addAndGet(5);
        assertEquals(5, listener.getCountAndReset());

        listener.count.addAndGet(3);
        assertEquals(3, listener.getCountAndReset());

        assertEquals(0, listener.getCountAndReset());
    }

    @Test
    void count_isThreadSafe() throws InterruptedException {
        final int iterations = 1000;
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) listener.count.incrementAndGet();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) listener.count.incrementAndGet();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(iterations * 2, listener.getCountAndReset());
    }
}
