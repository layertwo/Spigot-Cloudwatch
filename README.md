# CloudWatch

[AWS CloudWatch](https://aws.amazon.com/cloudwatch/) monitoring plugin for Minecraft Spigot servers running on AWS.

Requires an IAM role with `cloudwatch:PutMetricData` [permission](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/permissions-reference-cw.html). Unix/Linux only — some statistics (file descriptors, CPU load) are unavailable on other operating systems.

## Installation

Build the fat JAR and drop it into your Spigot `plugins/` directory:

```bash
mvn clean package
# Output: target/CloudWatch-<version>.jar
```

The plugin self-disables on startup if instance metadata is unreachable.

 ## Java Statistics
 All Java statistics collected are per minute and represent the current value or the count/total time during that period.

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
 All Minecraft statistics collected are per minute and represent the maximum value, count or total time during that period.

- Number of Online Players
- Maximum Tick Time
- Ticks per Second
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
