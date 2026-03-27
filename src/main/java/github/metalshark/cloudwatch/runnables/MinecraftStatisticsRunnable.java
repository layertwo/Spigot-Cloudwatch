package github.metalshark.cloudwatch.runnables;

import github.metalshark.cloudwatch.CloudWatch;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.util.ArrayList;
import java.util.List;

public class MinecraftStatisticsRunnable implements Runnable {

    private final CloudWatch plugin;
    private final Dimension dimension;
    private final CloudWatchClient cloudWatchClient;

    public MinecraftStatisticsRunnable(CloudWatch plugin, Dimension dimension, CloudWatchClient cloudWatchClient) {
        this.plugin = plugin;
        this.dimension = dimension;
        this.cloudWatchClient = cloudWatchClient;
    }

    @Override
    public void run() {
        final double chunksLoaded = plugin.getChunkLoadListener().getMaxAndReset();
        final double onlinePlayers = plugin.getPlayerJoinListener().getMaxAndReset();
        final double entityCount = plugin.getEntityCountSampler().getEntityCount();

        final TickRunnable tickRunnable = plugin.getTickRunnable();
        final double maxTickTime = tickRunnable.getMaxElapsedMillisAndReset();
        final double ticksPerSecond = tickRunnable.getNumberOfTicksAndReset() / 60.0;

        try {
            final List<MetricDatum> metrics = new ArrayList<>(5 + CloudWatch.getEventCountListeners().size());
            metrics.add(MetricDatum.builder().metricName("EntityCount").unit(StandardUnit.COUNT).value(entityCount).dimensions(dimension).build());
            metrics.add(MetricDatum.builder().metricName("ChunksLoaded").unit(StandardUnit.COUNT).value(chunksLoaded).dimensions(dimension).build());
            metrics.add(MetricDatum.builder().metricName("OnlinePlayers").unit(StandardUnit.COUNT).value(onlinePlayers).dimensions(dimension).build());
            metrics.add(MetricDatum.builder().metricName("MaxTickTime").unit(StandardUnit.MILLISECONDS).value(maxTickTime).dimensions(dimension).build());
            metrics.add(MetricDatum.builder().metricName("TicksPerSecond").unit(StandardUnit.COUNT).value(ticksPerSecond).dimensions(dimension).build());

            CloudWatch.getEventCountListeners().forEach((name, listener) ->
                metrics.add(MetricDatum.builder()
                    .metricName(name)
                    .unit(StandardUnit.COUNT)
                    .value(listener.getCountAndReset())
                    .dimensions(dimension)
                    .build()));

            cloudWatchClient.putMetricData(
                PutMetricDataRequest.builder().namespace("Minecraft").metricData(metrics).build());
        } catch (CloudWatchException e) {
            plugin.getLogger().warning("CloudWatch PutMetricData failed [Minecraft]: " + e.awsErrorDetails().errorCode());
        }
    }

}
