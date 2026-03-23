package github.metalshark.cloudwatch.runnables;

public class TickRunnable implements Runnable {

    private long lastTimeMillis = System.currentTimeMillis();
    private long maxElapsedMillis = 0;
    private long numberOfTicks = 0;

    @Override
    public synchronized void run() {
        final long timeMillis = System.currentTimeMillis();
        final long elapsedMillis = timeMillis - lastTimeMillis;
        if (elapsedMillis > maxElapsedMillis) maxElapsedMillis = elapsedMillis;
        lastTimeMillis = timeMillis;
        numberOfTicks++;
    }

    public synchronized double getMaxElapsedMillisAndReset() {
        final long old = maxElapsedMillis;
        maxElapsedMillis = 0;
        return old;
    }

    public synchronized double getNumberOfTicksAndReset() {
        final long old = numberOfTicks;
        numberOfTicks = 0;
        return old;
    }

}
