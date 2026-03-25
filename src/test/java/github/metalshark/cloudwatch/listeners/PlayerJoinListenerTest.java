package github.metalshark.cloudwatch.listeners;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerJoinListenerTest {

    private PlayerJoinListener listener;

    @BeforeEach
    void setUp() {
        listener = new PlayerJoinListener();
    }

    @Test
    void getMaxAndReset_returnsZeroInitially() {
        assertEquals(0, listener.getMaxAndReset());
    }

    @Test
    void onPlayerJoin_incrementsCountAndUpdatesMax() {
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);

        listener.onPlayerJoin(event);
        listener.onPlayerJoin(event);

        assertEquals(2, listener.getMaxAndReset());
    }

    @Test
    void onPlayerLeave_decrementsCount() {
        PlayerJoinEvent joinEvent = mock(PlayerJoinEvent.class);
        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);

        listener.onPlayerJoin(joinEvent);
        listener.onPlayerJoin(joinEvent);
        listener.onPlayerJoin(joinEvent);
        listener.onPlayerLeave(quitEvent);

        // Max was 3, current count is 2
        assertEquals(3, listener.getMaxAndReset());
        // After reset, max tracks current count
        assertEquals(2, listener.getMaxAndReset());
    }

    @Test
    void maxTracksHighWaterMark() {
        PlayerJoinEvent joinEvent = mock(PlayerJoinEvent.class);
        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);

        listener.onPlayerJoin(joinEvent);
        listener.onPlayerJoin(joinEvent);
        listener.onPlayerJoin(joinEvent);   // count=3, max=3
        listener.onPlayerLeave(quitEvent);  // count=2
        listener.onPlayerLeave(quitEvent);  // count=1

        assertEquals(3, listener.getMaxAndReset());
    }

    @Test
    void multipleCyclesTrackIndependently() {
        PlayerJoinEvent joinEvent = mock(PlayerJoinEvent.class);
        PlayerQuitEvent quitEvent = mock(PlayerQuitEvent.class);

        // Cycle 1: 2 players peak
        listener.onPlayerJoin(joinEvent);
        listener.onPlayerJoin(joinEvent);
        assertEquals(2, listener.getMaxAndReset());

        // Cycle 2: 1 leaves, 2 join -> peak is 3
        listener.onPlayerLeave(quitEvent);   // count=1
        listener.onPlayerJoin(joinEvent);     // count=2
        listener.onPlayerJoin(joinEvent);     // count=3

        assertEquals(3, listener.getMaxAndReset());
    }
}
