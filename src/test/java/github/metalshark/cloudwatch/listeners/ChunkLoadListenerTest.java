package github.metalshark.cloudwatch.listeners;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChunkLoadListenerTest {

    private ChunkLoadListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChunkLoadListener();
    }

    @Test
    void getMaxAndReset_returnsZeroInitially() {
        assertEquals(0, listener.getMaxAndReset());
    }

    @Test
    void onChunkLoad_incrementsCountAndUpdatesMax() {
        ChunkLoadEvent event = mock(ChunkLoadEvent.class);

        listener.onChunkLoad(event);
        listener.onChunkLoad(event);
        listener.onChunkLoad(event);

        assertEquals(3, listener.getMaxAndReset());
    }

    @Test
    void onChunkUnload_decrementsCount() {
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);

        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);
        listener.onChunkUnload(unloadEvent);

        // Max was 3 (before the unload), current count is 2
        double max = listener.getMaxAndReset();
        assertEquals(3, max);

        // After reset, max should be current count (2)
        assertEquals(2, listener.getMaxAndReset());
    }

    @Test
    void getMaxAndReset_resetsMaxToCurrentCount() {
        ChunkLoadEvent event = mock(ChunkLoadEvent.class);

        listener.onChunkLoad(event);
        listener.onChunkLoad(event);
        listener.onChunkLoad(event);

        // First read returns max of 3
        assertEquals(3, listener.getMaxAndReset());

        // After reset, max should be current count (still 3 since no unloads)
        assertEquals(3, listener.getMaxAndReset());
    }

    @Test
    void maxTracksHighWaterMark() {
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);

        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);  // count=3, max=3
        listener.onChunkUnload(unloadEvent); // count=2, max still 3
        listener.onChunkLoad(loadEvent);  // count=3, max=3
        listener.onChunkUnload(unloadEvent); // count=2, max still 3

        assertEquals(3, listener.getMaxAndReset());
    }

    @Test
    void maxExceedsPreviousPeak() {
        ChunkLoadEvent loadEvent = mock(ChunkLoadEvent.class);
        ChunkUnloadEvent unloadEvent = mock(ChunkUnloadEvent.class);

        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);  // count=2, max=2

        assertEquals(2, listener.getMaxAndReset()); // resets max to count=2

        listener.onChunkLoad(loadEvent);
        listener.onChunkLoad(loadEvent);  // count=4, max=4

        assertEquals(4, listener.getMaxAndReset());
    }
}
