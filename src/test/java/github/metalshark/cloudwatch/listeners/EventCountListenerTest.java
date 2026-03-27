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
        listener.increment();
        listener.increment();
        listener.increment();

        assertEquals(3, listener.getCountAndReset());
    }

    @Test
    void getCountAndReset_resetsToZeroAfterRead() {
        listener.increment();
        listener.increment();

        listener.getCountAndReset();
        assertEquals(0, listener.getCountAndReset());
    }

    @Test
    void getCountAndReset_handlesMultipleCycles() {
        for (int i = 0; i < 5; i++) listener.increment();
        assertEquals(5, listener.getCountAndReset());

        for (int i = 0; i < 3; i++) listener.increment();
        assertEquals(3, listener.getCountAndReset());

        assertEquals(0, listener.getCountAndReset());
    }

    @Test
    void increment_isThreadSafe() throws InterruptedException {
        final int iterations = 1000;
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) listener.increment();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < iterations; i++) listener.increment();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(iterations * 2, listener.getCountAndReset());
    }
}
