package github.metalshark.cloudwatch.runnables;

import com.sun.management.UnixOperatingSystemMXBean;
import github.metalshark.cloudwatch.CloudWatch;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.lang.management.*;
import java.util.List;

public class JavaStatisticsRunnable implements Runnable {

    private final Dimension dimension;
    private final CloudWatchClient cloudWatchClient;

    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    private double prevTotalGarbageCollections = 0;
    private double prevTotalGarbageCollectionTime = 0;
    private boolean firstRun = true;

    public JavaStatisticsRunnable(Dimension dimension, CloudWatchClient cloudWatchClient) {
        this.dimension = dimension;
        this.cloudWatchClient = cloudWatchClient;
    }

    public void run() {

        double totalGarbageCollections = 0;
        double totalGarbageCollectionTime = 0;

        for (GarbageCollectorMXBean gc : gcBeans) {
            final long count = gc.getCollectionCount();
            if (count >= 0) totalGarbageCollections += count;

            final long time = gc.getCollectionTime();
            if (time >= 0) totalGarbageCollectionTime += time;
        }

        final double garbageCollections = totalGarbageCollections - prevTotalGarbageCollections;
        final double garbageCollectionTime = totalGarbageCollectionTime - prevTotalGarbageCollectionTime;

        prevTotalGarbageCollections = totalGarbageCollections;
        prevTotalGarbageCollectionTime = totalGarbageCollectionTime;

        if (firstRun) {
            firstRun = false;
            return;
        }

        // MemoryMXBean.getHeapMemoryUsage() returns a consistent snapshot, avoiding
        // the race between separate Runtime.totalMemory()/freeMemory() calls.
        final MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        final double heapSize = heap.getCommitted();
        final double heapMaxSize = heap.getMax();
        final double heapUsedSize = heap.getUsed();
        final double heapFreeSize = heapSize - heapUsedSize;

        final double threadCount = threadBean.getThreadCount();

        double openFileDescriptors = 0;
        double maxFileDescriptors = 0;
        double totalPhysicalMemorySize = 0;
        double freePhysicalMemorySize = 0;
        double usedPhysicalMemorySize = 0;
        double processCpuLoad = 0;
        double systemCpuLoad = 0;
        if (osBean instanceof UnixOperatingSystemMXBean) {
            final UnixOperatingSystemMXBean unixOs = (UnixOperatingSystemMXBean) osBean;
            openFileDescriptors = unixOs.getOpenFileDescriptorCount();
            maxFileDescriptors = unixOs.getMaxFileDescriptorCount();
            totalPhysicalMemorySize = unixOs.getTotalMemorySize();
            freePhysicalMemorySize = unixOs.getFreeMemorySize();
            usedPhysicalMemorySize = totalPhysicalMemorySize - freePhysicalMemorySize;
            processCpuLoad = unixOs.getProcessCpuLoad() * 100;
            systemCpuLoad = unixOs.getCpuLoad() * 100;
        }

        try {
            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                .namespace("Java")
                .metricData(List.of(
                    MetricDatum.builder().metricName("GarbageCollections").unit(StandardUnit.COUNT).value(garbageCollections).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("GarbageCollectionTime").unit(StandardUnit.MILLISECONDS).value(garbageCollectionTime).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("HeapSize").unit(StandardUnit.BYTES).value(heapSize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("HeapMaxSize").unit(StandardUnit.BYTES).value(heapMaxSize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("HeapFreeSize").unit(StandardUnit.BYTES).value(heapFreeSize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("HeapUsedSize").unit(StandardUnit.BYTES).value(heapUsedSize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("Threads").unit(StandardUnit.COUNT).value(threadCount).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("OpenFileDescriptors").unit(StandardUnit.COUNT).value(openFileDescriptors).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("MaxFileDescriptors").unit(StandardUnit.COUNT).value(maxFileDescriptors).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("TotalPhysicalMemorySize").unit(StandardUnit.BYTES).value(totalPhysicalMemorySize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("FreePhysicalMemorySize").unit(StandardUnit.BYTES).value(freePhysicalMemorySize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("UsedPhysicalMemorySize").unit(StandardUnit.BYTES).value(usedPhysicalMemorySize).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("ProcessCpuLoad").unit(StandardUnit.PERCENT).value(processCpuLoad).dimensions(dimension).build(),
                    MetricDatum.builder().metricName("SystemCpuLoad").unit(StandardUnit.PERCENT).value(systemCpuLoad).dimensions(dimension).build()
                ))
                .build());
        } catch (CloudWatchException e) {
            CloudWatch.getPlugin().getLogger().warning("CloudWatch PutMetricData failed [Java]: " + e.awsErrorDetails().errorCode());
        }
    }

}
