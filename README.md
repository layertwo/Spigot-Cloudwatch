# CloudWatch

[AWS CloudWatch](https://aws.amazon.com/cloudwatch/) monitoring plugin for Minecraft Spigot servers running on AWS.

Requires an IAM role with `cloudwatch:PutMetricData` [permission](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/permissions-reference-cw.html). Unix/Linux only — some statistics (file descriptors, CPU load) are unavailable on other operating systems.

## Installation

Build the fat JAR and drop it into your Spigot `plugins/` directory:

```bash
mvn clean package
# Output: target/CloudWatch-<version>.jar
```

Requires Spigot 1.13+.

## Configuration

The plugin needs a server name to use as the CloudWatch dimension value. It resolves the name in this order:

1. `SPIGOT_CLOUDWATCH_SERVER` environment variable
2. `server` key in `plugins/CloudWatch/config.yml`
3. EC2 instance ID (via EC2 instance metadata service)
4. ECS task ID (via `ECS_CONTAINER_METADATA_URI_V4` metadata endpoint)

The plugin self-disables on startup if no server identity can be determined from any of these sources.

**config.yml** (auto-generated on first run):

```yaml
server: ""
```

Set `server` to a stable name for your instance if you are not running on EC2 or ECS, or want to override the auto-detected value.

## Development

### Prerequisites

- Java 17+
- Maven 3.6+

### Running Tests

```bash
mvn test
```

Tests cover the core metric-tracking components:

- **TickRunnable** — tick counting and max elapsed time tracking
- **EventCountListener** — atomic event counter with reset semantics
- **ChunkLoadListener** — chunk load/unload tracking with high-water mark
- **PlayerJoinListener** — player count tracking with high-water mark

## Metrics

Metrics are published every minute to two CloudWatch namespaces, dimensioned by `Server = <server name>`.

## Java Statistics

CloudWatch namespace: `Java`. All values represent the current value or the count/total time during that minute.

- Number of Garbage Collections
- Time spent performing Garbage Collection
- Heap Size
- Heap Max Size
- Heap Free Size
- Heap Used Size
- Number of Threads
- Number of Open File Descriptors
- Maximum File Descriptors
- Total Physical Memory Size
- Free Physical Memory Size
- Used Physical Memory Size
- Process CPU Load
- System CPU Load

## Minecraft Statistics

CloudWatch namespace: `Minecraft`. All values represent the maximum value, count, or total time during that minute.

- Number of Online Players
- Maximum Tick Time
- Ticks per Second
- Number of Entities (live count across all worlds)
- Number of Chunks Loaded
- Number of Chunks Populated
- Number of Creatures Spawned
- Number of Entity Deaths
- Number of Inventories Closed
- Number of Inventories Opened
- Number of Inventory Clicks
- Number of Inventory Drags
- Number of Items Despawned
- Number of Items Spawned
- Number of Items Players Dropped
- Number of Player Experience Changes
- Number of Player Interactions
- Number of Projectiles Launched
- Number of Structures Grown
- Number of Trades Selected
