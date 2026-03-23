package github.metalshark.cloudwatch;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import github.metalshark.cloudwatch.listeners.*;
import github.metalshark.cloudwatch.runnables.JavaStatisticsRunnable;
import github.metalshark.cloudwatch.runnables.MinecraftStatisticsRunnable;
import github.metalshark.cloudwatch.runnables.TickRunnable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

public class CloudWatch extends JavaPlugin {

    @Getter
    private final ChunkLoadListener chunkLoadListener = new ChunkLoadListener();

    @Getter
    private final PlayerJoinListener playerJoinListener = new PlayerJoinListener();

    @Getter
    private final TickRunnable tickRunnable = new TickRunnable();

    @Getter
    private final static Map<String, EventCountListener> eventCountListeners = new ConcurrentHashMap<>();

    private final static ThreadFactory javaStatisticsThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("CloudWatch - Java Statistics")
        .build();
    private final static ThreadFactory minecraftStatisticsThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("CloudWatch - Minecraft Statistics")
        .build();

    private ScheduledExecutorService javaStatisticsExecutor;
    private ScheduledExecutorService minecraftStatisticsExecutor;

    @Getter
    private static Dimension dimension;

    @Getter
    private CloudWatchClient cloudWatchClient;

    private static String resolveInstanceId() {
        try {
            return EC2MetadataUtils.getInstanceId();
        } catch (SdkClientException ignored) {}

        final String metadataUri = System.getenv("ECS_CONTAINER_METADATA_URI_V4");
        if (metadataUri == null) return null;

        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metadataUri + "/task"))
                .timeout(Duration.ofSeconds(2))
                .build();
            final String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            @SuppressWarnings("unchecked")
            final Map<String, Object> metadata = new Yaml().load(body);
            final String taskArn = (String) metadata.get("TaskARN");
            if (taskArn == null) return null;
            return taskArn.substring(taskArn.lastIndexOf('/') + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void onEnable() {
        eventCountListeners.clear();

        final String instanceId = resolveInstanceId();
        if (instanceId == null) {
            getLogger().warning("The CloudWatch plugin requires EC2 or ECS/Fargate instance metadata.");
            this.setEnabled(false);
            return;
        }

        dimension = Dimension
            .builder()
            .name("Per-Instance Metrics")
            .value(instanceId)
            .build();

        cloudWatchClient = CloudWatchClient.builder().build();

        final PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(chunkLoadListener.init(), this);
        pluginManager.registerEvents(playerJoinListener.init(), this);

        eventCountListeners.put("ChunksPopulated", new ChunkPopulateListener());
        eventCountListeners.put("CreaturesSpawned", new CreatureSpawnListener());
        eventCountListeners.put("EntityDeaths", new EntityDeathListener());
        eventCountListeners.put("InventoriesClosed", new InventoryCloseListener());
        eventCountListeners.put("InventoriesOpened", new InventoryOpenListener());
        eventCountListeners.put("InventoryClicks", new InventoryClickListener());
        eventCountListeners.put("InventoryDrags", new InventoryDragListener());
        eventCountListeners.put("ItemsSpawned", new ItemSpawnListener());
        eventCountListeners.put("ItemsDespawned", new ItemDespawnListener());
        eventCountListeners.put("PlayerDropItems", new PlayerDropItemListener());
        eventCountListeners.put("PlayerExperienceChanges", new PlayerExpChangeListener());
        eventCountListeners.put("PlayerInteractions", new PlayerInteractListener());
        eventCountListeners.put("ProjectilesLaunched", new ProjectileLaunchListener());
        eventCountListeners.put("StructuresGrown", new StructureGrowListener());
        eventCountListeners.put("TradesSelected", new TradeSelectListener());

        for (Map.Entry<String, EventCountListener> entry : eventCountListeners.entrySet()) {
            final EventCountListener listener = entry.getValue();
            pluginManager.registerEvents(listener, this);
        }

        javaStatisticsExecutor = Executors.newSingleThreadScheduledExecutor(javaStatisticsThreadFactory);
        javaStatisticsExecutor.scheduleAtFixedRate(new JavaStatisticsRunnable(), 0, 1, TimeUnit.MINUTES);

        minecraftStatisticsExecutor = Executors.newSingleThreadScheduledExecutor(minecraftStatisticsThreadFactory);
        minecraftStatisticsExecutor.scheduleAtFixedRate(new MinecraftStatisticsRunnable(), 0, 1, TimeUnit.MINUTES);

        Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, tickRunnable, 1, 1);
    }

    @Override
    public void onDisable() {
        ChunkLoadEvent.getHandlerList().unregister(chunkLoadListener);
        ChunkUnloadEvent.getHandlerList().unregister(chunkLoadListener);
        PlayerJoinEvent.getHandlerList().unregister(playerJoinListener);
        PlayerQuitEvent.getHandlerList().unregister(playerJoinListener);

        for (Map.Entry<String, EventCountListener> entry : eventCountListeners.entrySet()) {
            final Listener listener = entry.getValue();
            HandlerList.unregisterAll(listener);
        }

        if (javaStatisticsExecutor != null) {
            javaStatisticsExecutor.shutdown();
            try { javaStatisticsExecutor.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        if (minecraftStatisticsExecutor != null) {
            minecraftStatisticsExecutor.shutdown();
            try { minecraftStatisticsExecutor.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        }
        if (cloudWatchClient != null) cloudWatchClient.close();
    }

    public static CloudWatch getPlugin() {
        return getPlugin(CloudWatch.class);
    }

}
