# 🌌 Cosmos Launcher

A gravitational Android home screen where your apps orbit in a living galaxy, ranked by importance — like Obsidian's graph view, but as your launcher.

## Architecture

```
CosmosLauncher/
├── app/src/main/java/com/cosmos/launcher/
│   ├── AppNode.kt          — Data model: app + physics state + importance score
│   ├── AppRepository.kt    — Loads real apps, reads UsageStats, persists launch counts
│   ├── CosmosPhysics.kt    — Gravity/spring/repulsion simulation engine
│   ├── CosmosRenderer.kt   — Canvas drawing: stars, nebulae, tethers, nodes, tooltips
│   ├── CosmosView.kt       — SurfaceView with 60fps render thread + touch handling
│   ├── LauncherActivity.kt — Main activity (HOME intent)
│   ├── SettingsActivity.kt — Gravity/speed tuning panel
│   └── BootReceiver.kt     — Boot-complete receiver
```

## How Importance Works

Each app's importance score (0.0–1.0) is computed from:

| Signal         | Weight | Description                           |
|----------------|--------|---------------------------------------|
| Launch count   | 45%    | How many times you've opened it       |
| Usage time     | 35%    | Total foreground time (last 30 days)  |
| Recency        | 20%    | Exponential decay — used today = high |

Importance drives everything visual:
- **Size**: Core apps (>85%) are large, peripheral ones small
- **Orbit radius**: High importance → close to cluster center
- **Orbit speed**: High importance → faster orbit
- **Glow intensity**: More important = brighter aura
- **Tether opacity**: Strong tethers for high-importance apps
- **Label visibility**: Only shown above threshold

## Clusters

| Cluster   | Color     | Contents                          |
|-----------|-----------|-----------------------------------|
| WORK      | Red       | Gmail, Calendar, Slack, Notion…   |
| SOCIAL    | Green     | WhatsApp, Instagram, Telegram…    |
| CREATIVE  | Purple    | Spotify, YouTube, Figma…          |
| UTILITY   | Amber     | Maps, Health, Notes…              |
| FINANCE   | Teal      | Banking, Revolut, Crypto…         |

New apps auto-categorize by package name heuristics. Add to `clusterMap` in `AppRepository.kt` for precise control.

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- Android SDK 34
- JDK 17+

### Steps
1. Open `CosmosLauncher/` folder in Android Studio
2. Let Gradle sync (downloads ~200MB dependencies first time)
3. Connect an Android device or start an emulator (API 26+)
4. Run → select device → install
5. On device: Settings → Apps → Default apps → Home app → Cosmos Launcher

### Permissions Required
- `QUERY_ALL_PACKAGES` — to list installed apps
- `PACKAGE_USAGE_STATS` — for importance score computation
  - Must be granted manually: Settings → Apps → Special app access → Usage access → Cosmos Launcher → Allow

## Customization

### Add new cluster categories
In `AppRepository.kt`, add entries to `clusterMap`:
```kotlin
"com.your.app" to "work",  // or social, creative, utility, finance
```

### Tune physics
In `CosmosPhysics.kt`:
```kotlin
private val SPRING_STRENGTH = 0.018f   // orbital snap speed
private val GRAVITY_STRENGTH = 0.0006f // pull toward cluster center
private val REPULSION_STRENGTH = 0.35f // nodes push each other apart
private val DAMPING = 0.86f            // velocity decay (lower = floatier)
```

### Change importance weights
In `AppNode.kt → refreshImportance()`:
```kotlin
importance = launchScore * 0.45f + usageScore * 0.35f + recencyScore * 0.20f
```

## Physics Engine

Each frame tick applies 5 forces:
1. **Orbital spring** — pulls toward orbit target (cluster center + angle + radius)
2. **Cluster gravity** — gentle inward pull, scaled by importance
3. **Node repulsion** — short-range force prevents overlap
4. **Center mass** — keeps all apps on-screen
5. **Damping** — velocity decay for stable orbits

Apps dragged by finger get `isDragging = true`, bypassing physics until released, then spring back into orbit.

## Requirements
- Android 8.0+ (API 26)
- Permissions: QUERY_ALL_PACKAGES, PACKAGE_USAGE_STATS
