# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (produces fat JAR with all dependencies)
mvn clean package

# Output: target/CloudWatch-<version>.jar
```

Java 14 source/target compatibility; CI uses JDK 17. No tests exist in this project.

## Architecture

This is a Spigot (Bukkit) plugin that ships metrics to AWS CloudWatch every minute. On `onEnable()` it resolves a unique instance identifier to use as the CloudWatch `Dimension` value, trying two sources in order:

1. EC2 IMDS via `EC2MetadataUtils.getInstanceId()`
2. ECS/Fargate task metadata endpoint (`ECS_CONTAINER_METADATA_URI_V4` env var + `/task`) — extracts the task ID from the `TaskARN` field

If neither source is available the plugin disables itself.

### Three execution paths

| Class | Thread | Schedule | CloudWatch namespace |
|---|---|---|---|
| `JavaStatisticsRunnable` | Dedicated `ScheduledExecutorService` | Every 1 minute | `Java` |
| `MinecraftStatisticsRunnable` | Dedicated `ScheduledExecutorService` | Every 1 minute | `Minecraft` |
| `TickRunnable` | Bukkit async scheduler | Every server tick (1/20s) | — (feeds into Minecraft runnable) |

### Listener pattern

`EventCountListener` is the base class for all simple event counters. Subclasses register a single `@EventHandler` that increments `count`. Each minute `MinecraftStatisticsRunnable` calls `getCountAndReset()` on every entry in `CloudWatch.eventCountListeners` (a `ConcurrentHashMap<String, EventCountListener>`) and publishes one `MetricDatum` per entry.

Two listeners are **not** `EventCountListener` subclasses:
- `ChunkLoadListener` — tracks current loaded-chunk count across load/unload events and exposes `getMaxAndReset()` (max chunks loaded during the period)
- `PlayerJoinListener` — tracks peak online player count via join/quit events

### Deployment

The Maven assembly plugin produces a fat JAR (`jar-with-dependencies`) that includes the AWS SDK v2 (`software.amazon.awssdk:cloudwatch`). This JAR is dropped into the Spigot `plugins/` directory. The IAM role must have `cloudwatch:PutMetricData` permission.

Releases are published to GitHub Packages via the `release.yml` workflow when a GitHub release is created.
