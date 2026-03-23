package github.metalshark.cloudwatch.runnables;

import github.metalshark.cloudwatch.CloudWatch;
import github.metalshark.cloudwatch.listeners.*;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.util.ArrayList;
import java.util.List;

public class MinecraftStatisticsRunnable implements Runnable {

    @Override
    public void run() {
        final CloudWatch plugin = CloudWatch.getPlugin();
        final Dimension dimension = CloudWatch.getDimension();

        final ChunkLoadListener chunkLoadListener = plugin.getChunkLoadListener();
        final double chunksLoaded = chunkLoadListener.getMaxAndReset();

        final PlayerJoinListener playerJoinListener = plugin.getPlayerJoinListener();
        final double onlinePlayers = playerJoinListener.getMaxAndReset();

        final TickRunnable tickRunnable = plugin.getTickRunnable();
        final double maxTickTime = tickRunnable.getMaxElapsedMillisAndReset();
        final double ticksPerMinute = tickRunnable.getNumberOfTicksAndReset();
        final double ticksPerSecond = ticksPerMinute / 60;

        final CloudWatchClient cw = CloudWatch.getPlugin().getCloudWatchClient();
        try {

            final MetricDatum chunksLoadedMetric = MetricDatum
                .builder()
                .metricName("ChunksLoaded")
                .unit(StandardUnit.COUNT)
                .value(chunksLoaded)
                .dimensions(dimension)
                .build();
            final MetricDatum onlinePlayersMetric = MetricDatum
                .builder()
                .metricName("OnlinePlayers")
                .unit(StandardUnit.COUNT)
                .value(onlinePlayers)
                .dimensions(dimension)
                .build();
            final MetricDatum maxTickTimeMetric = MetricDatum
                .builder()
                .metricName("MaxTickTime")
                .unit(StandardUnit.MILLISECONDS)
                .value(maxTickTime)
                .dimensions(dimension)
                .build();
            final MetricDatum ticksPerSecondMetric = MetricDatum
                .builder()
                .metricName("TicksPerSecond")
                .unit(StandardUnit.COUNT)
                .value(ticksPerSecond)
                .dimensions(dimension)
                .build();

            final List<MetricDatum> metricDatumList = new ArrayList<>();
            metricDatumList.add(chunksLoadedMetric);
            metricDatumList.add(maxTickTimeMetric);
            metricDatumList.add(onlinePlayersMetric);
            metricDatumList.add(ticksPerSecondMetric);

            CloudWatch
                .getEventCountListeners()
                .forEach((name, listener) -> {
                    final double count = listener.getCountAndReset();
                    metricDatumList.add(MetricDatum
                        .builder()
                        .metricName(name)
                        .unit(StandardUnit.COUNT)
                        .value(count)
                        .dimensions(dimension)
                        .build());
                });

            final PutMetricDataRequest request = PutMetricDataRequest
                .builder()
                .namespace("Minecraft")
                .metricData(metricDatumList)
                .build();

            cw.putMetricData(request);
        } catch (CloudWatchException e) {
            CloudWatch.getPlugin().getLogger().severe(e.awsErrorDetails().errorMessage());
        }
    }

}
