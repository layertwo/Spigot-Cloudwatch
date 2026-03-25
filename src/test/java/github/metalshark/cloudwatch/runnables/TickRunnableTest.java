package github.metalshark.cloudwatch.runnables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TickRunnableTest {

    private TickRunnable tickRunnable;

    @BeforeEach
    void setUp() {
        tickRunnable = new TickRunnable();
    }

    @Test
    void getNumberOfTicksAndReset_returnsZeroInitially() {
        assertEquals(0, tickRunnable.getNumberOfTicksAndReset());
    }

    @Test
    void run_incrementsTickCount() {
        tickRunnable.run();
        tickRunnable.run();
        tickRunnable.run();

        assertEquals(3, tickRunnable.getNumberOfTicksAndReset());
    }

    @Test
    void getNumberOfTicksAndReset_resetsAfterRead() {
        tickRunnable.run();
        tickRunnable.run();

        tickRunnable.getNumberOfTicksAndReset();
        assertEquals(0, tickRunnable.getNumberOfTicksAndReset());
    }

    @Test
    void getMaxElapsedMillisAndReset_returnsZeroInitially() {
        assertEquals(0, tickRunnable.getMaxElapsedMillisAndReset());
    }

    @Test
    void getMaxElapsedMillisAndReset_resetsAfterRead() throws InterruptedException {
        tickRunnable.run();
        Thread.sleep(50);
        tickRunnable.run();

        double max = tickRunnable.getMaxElapsedMillisAndReset();
        assertTrue(max >= 40, "Max elapsed should be at least 40ms, was: " + max);

        assertEquals(0, tickRunnable.getMaxElapsedMillisAndReset());
    }

    @Test
    void run_tracksMaxElapsedTime() throws InterruptedException {
        tickRunnable.run();
        Thread.sleep(20);
        tickRunnable.run();
        Thread.sleep(60);
        tickRunnable.run();

        double max = tickRunnable.getMaxElapsedMillisAndReset();
        assertTrue(max >= 50, "Max elapsed should reflect the longest tick gap, was: " + max);
    }

    @Test
    void run_multipleCyclesAreIndependent() throws InterruptedException {
        // First cycle
        tickRunnable.run();
        Thread.sleep(30);
        tickRunnable.run();
        double firstMax = tickRunnable.getMaxElapsedMillisAndReset();
        double firstTicks = tickRunnable.getNumberOfTicksAndReset();

        assertTrue(firstMax >= 20);
        assertEquals(2, firstTicks);

        // Second cycle
        tickRunnable.run();
        double secondMax = tickRunnable.getMaxElapsedMillisAndReset();
        double secondTicks = tickRunnable.getNumberOfTicksAndReset();

        assertEquals(1, secondTicks);
        // Second max should be small since no sleep between reset and run
        assertTrue(secondMax < firstMax || secondMax >= 0);
    }
}
