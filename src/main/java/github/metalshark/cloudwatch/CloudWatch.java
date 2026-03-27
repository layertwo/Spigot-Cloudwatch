package github.metalshark.cloudwatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ScheduledExecutorService javaStatisticsExecutor;
    private ScheduledExecutorService minecraftStatisticsExecutor;

    @Getter
    private Dimension dimension;

    @Getter
    private CloudWatchClient cloudWatchClient;

    private static String resolveInstanceId() {
        try {
            return EC2MetadataUtils.getInstanceId();
        } catch (SdkClientException ignored) {}

        final String metadataUri = System.getenv("ECS_CONTAINER_METADATA_URI_V4");
        if (metadataUri == null) return null;

        // Prevent SSRF: only allow the ECS task metadata endpoint
        try {
            final URI uri = URI.create(metadataUri);
            if (!"http".equals(uri.getScheme()) || !"169.254.170.2".equals(uri.getHost())) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }

        final ExecutorService httpExecutor = Executors.newSingleThreadExecutor();
        try {
            final HttpClient client = HttpClient.newBuilder().executor(httpExecutor).build();
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(metadataUri + "/task"))
                .timeout(Duration.ofSeconds(2))
                .build();
            final String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            final JsonNode metadata = OBJECT_MAPPER.readTree(body);
            final JsonNode taskArnNode = metadata.get("TaskARN");
            if (taskArnNode == null || taskArnNode.isNull()) return null;
            final String taskArn = taskArnNode.asText();
            return taskArn.substring(taskArn.lastIndexOf('/') + 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ignored) {
            return null;
        } finally {
            httpExecutor.shutdownNow();
        }
    }

    private String resolveServer() {
        String s = System.getenv("SPIGOT_CLOUDWATCH_SERVER");
        if (s != null && !s.isBlank()) return s.trim();
        s = getConfig().getString("server", "");
        return s != null ? s.trim() : "";
    }

    @Override
    public void onEnable() {
        eventCountListeners.clear();

        saveDefaultConfig();

        String server = resolveServer();
        if (server.isEmpty()) {
            server = resolveInstanceId();
        }
        if (server == null || server.isEmpty()) {
            getLogger().warning("Could not determine server identity. Disabling CloudWatch plugin.");
            this.setEnabled(false);
            return;
        }
        if (server.length() > 256 || !server.chars().allMatch(c -> c >= 0x20 && c <= 0x7E)) {
            getLogger().warning("Server identity is invalid (must be 1-256 printable ASCII characters). Disabling CloudWatch plugin.");
            this.setEnabled(false);
            return;
        }

        dimension = Dimension
            .builder()
            .name("Server")
            .value(server)
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
        javaStatisticsExecutor.scheduleAtFixedRate(
            new JavaStatisticsRunnable(dimension, cloudWatchClient), 0, 1, TimeUnit.MINUTES);

        minecraftStatisticsExecutor = Executors.newSingleThreadScheduledExecutor(minecraftStatisticsThreadFactory);
        minecraftStatisticsExecutor.scheduleAtFixedRate(
            new MinecraftStatisticsRunnable(this, dimension, cloudWatchClient), 0, 1, TimeUnit.MINUTES);

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
