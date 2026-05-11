# 🌌 Cosmos Launcher

A gravitational Android home screen where your apps orbit in a living galaxy, ranked by importance — like Obsidian's graph view, but as your launcher.

## 🎯 Overview

Cosmos transforms your launcher into an interactive physics simulation. Apps are displayed as cosmic bodies orbiting around cluster centers based on their **category** (Work, Social, Creative, Utility, Finance) and **usage patterns** (launch frequency, screen time, recency). The result is a unique, visually stunning way to access your apps while naturally surfacing the ones you use most.

## ✨ Core Features

- **Physics-Based Visualization**: Apps orbit using real orbital mechanics with gravity, springs, and repulsion forces
- **Smart App Ranking**: Dynamic importance scoring based on launch count, usage time, and recency
- **Gesture Support**: Drag apps freely around the screen; tap to launch
- **Real-time Analytics**: Live time/date display and usage stat integration
- **Category Clustering**: 5 predefined clusters with color coding and auto-categorization
- **Smooth 60 FPS Animation**: Dedicated render thread on SurfaceView
- **Immersive UI**: Full-screen launcher with top time display and dock
- **Boot Persistence**: Restores launcher state on device restart

## 🏗️ Architecture

```
CosmosLauncher/
├── app/src/main/java/com/cosmos/launcher/
│   ├── AppNode.kt              — Data model: app + physics state + importance score
│   ├── AppRepository.kt        — Loads real apps, reads UsageStats, persists analytics
│   ├── CosmosPhysics.kt        — Gravity/spring/repulsion simulation engine
│   ├── CosmosRenderer.kt       — Canvas drawing: stars, orbits, tethers, nodes, labels
│   ├── CosmosView.kt           — SurfaceView with 60fps render thread + touch handling
│   ├── LauncherActivity.kt     — Main activity (HOME intent)
│   ├── SettingsActivity.kt     — Physics parameter tuning panel
│   └── BootReceiver.kt         — Boot-complete receiver
├── app/src/main/res/
│   ├── layout/                 — XML layouts (launcher, settings, legend)
│   ├── drawable/               — Drawables (profile orb, search bar, dock backgrounds)
│   ├── values/                 — Colors, strings, themes
│   └── xml/                    — Preferences
└── build.gradle, settings.gradle
```

## 📊 How Importance Works

Each app's importance score (0.0–1.0) is computed from three weighted signals, updated in real-time:

| Signal         | Weight | Description                           |
|----------------|--------|---------------------------------------|
| Launch count   | 45%    | How many times you've opened it       |
| Usage time     | 35%    | Total foreground time (last 30 days)  |
| Recency        | 20%    | Exponential decay — used today = high |

**Calculation**:
```kotlin
launchScore = (launchCount / 100).coerceIn(0f, 1f)
usageScore = (usageTimeMs / 2_hours).coerceIn(0f, 1f)
recencyScore = 1 / (1 + daysSinceUsed * 0.5)

importance = launchScore * 0.45 + usageScore * 0.35 + recencyScore * 0.20
```

### Visual Impact

Importance drives everything visual:

| Property | Effect |
|----------|--------|
| **Size** | Low importance (0.1) = 15px radius; High importance (1.0) = 40px radius |
| **Glow Intensity** | Low = 0.3; High = 1.0 (affects brightness/alpha) |
| **Orbit Speed** | Low = 0.005 rad/frame; High = 0.03 rad/frame |
| **Tether Opacity** | Low = 0.2; High = 1.0 (connection lines fade based on distance/importance) |
| **Label Visibility** | Only shown if importance > 0.5 |

## 🎨 Clusters

Apps are grouped into **5 predefined clusters**, each with a distinct color and orbit radius:

| Cluster   | Color   | Hex     | Apps                                    |
|-----------|---------|---------|------------------------------------------|
| WORK      | Red     | #FF6B6B | Gmail, Calendar, Slack, Notion…         |
| SOCIAL    | Green   | #51CF66 | WhatsApp, Instagram, Telegram, Twitter… |
| CREATIVE  | Purple  | #A78BFA | Spotify, YouTube, Figma, Adobe apps…    |
| UTILITY   | Amber   | #FBBF24 | Maps, Settings, Google Docs, Sheets…    |
| FINANCE   | Teal    | #06B6D4 | Revolut, PayPal, Coinbase, Banking…     |

### Orbit Assignment

Within each cluster, apps are positioned in a circle based on importance:
- Base radius varies per cluster (80–280 px)
- Apps distributed evenly around the circle
- Angle incremented by `2π / clusterSize`

### Auto-Categorization

Apps without predefined mappings are guessed by package name patterns:

```kotlin
when {
    packageName.contains("mail") || contains("calendar") || contains("slack") → "work"
    packageName.contains("whatsapp") || contains("messenger") || contains("instagram") → "social"
    packageName.contains("spotify") || contains("music") || contains("youtube") → "creative"
    packageName.contains("bank") || contains("finance") || contains("crypto") → "finance"
    else → "utility"
}
```

## 🎮 Physics Engine

Each frame applies **5 forces** in sequence:

### 1. Orbital Spring
Pulls apps toward their target orbit position (determined by cluster and importance).

```kotlin
val targetX = targetOrbitRadius * cos(targetOrbitAngle)
val targetY = targetOrbitRadius * sin(targetOrbitAngle)
force = SPRING_STRENGTH * speedMultiplier * distance
```

**Parameter**: `SPRING_STRENGTH = 0.018f`

### 2. Cluster Gravity
Gentle inward pull toward the cluster center (origin), scaled by app importance.

```kotlin
force = GRAVITY_STRENGTH * gravityMultiplier * importance
```

**Parameter**: `GRAVITY_STRENGTH = 0.0006f`

### 3. Node Repulsion
Short-range repulsive force prevents app icons from overlapping.

```kotlin
if (distance < minDist && distance > 0.1f) {
    overlap = minDist - distance
    force = REPULSION_STRENGTH * overlap
}
```

**Parameters**:
- `REPULSION_STRENGTH = 0.35f`
- `MIN_DISTANCE = 25f`

### 4. Center Mass
Soft boundary keeps all apps within viewport bounds.

```kotlin
if (x < minX) fx += (minX - x) * CENTER_MASS_STRENGTH
```

**Parameter**: `CENTER_MASS_STRENGTH = 0.0003f`

### 5. Damping & Velocity Limits
Velocity decay and speed capping for stable, smooth motion.

```kotlin
vx *= DAMPING           // vx *= 0.86
vy *= DAMPING
if (speed > MAX_VELOCITY) {
    vx = (vx / speed) * MAX_VELOCITY
    vy = (vy / speed) * MAX_VELOCITY
}
```

**Parameters**:
- `DAMPING = 0.86f`
- `MAX_VELOCITY = 12f`

### Dragging Override

When `isDragging = true`, physics is bypassed and the app follows the finger touch point directly with offset compensation.

## 🖱️ Touch Interaction

| Action | Behavior |
|--------|----------|
| **Press (ACTION_DOWN)** | Find closest app within `radius + 20dp`; set `isDragging = true` |
| **Move (ACTION_MOVE)** | Update app position with touch offset; skip physics |
| **Release (ACTION_UP)** | Measure movement distance; if < 30dp → launch app; if > 30dp → return to orbit (physics resumes) |

## 🎨 Rendering

### Render Pipeline

Each frame (60 FPS target):

1. **Clear canvas** with background (#04060f)
2. **Draw stars** (100 deterministic pseudo-random stars using seed 12345L)
3. **Draw orbits** (circle paths + segmented lines for each orbit)
4. **Draw tethers** (connection lines between apps in same cluster or high importance)
5. **Draw nodes** (sorted by importance; draw glow, core circle, icon, label)
6. **Draw touch indicator** (concentric circles if touching)

### Node Rendering

For each app node:

```kotlin
// 1. Glow layer
drawCircle(x, y, radius * 1.3f, glowPaint) // color: cluster color, alpha: importance * 150

// 2. Core
drawCircle(x, y, radius, clusterPaint)     // solid color circle

// 3. Icon (if available)
node.icon.draw(canvas)                     // scaled to radius * 1.5f

// 4. Label (if importance > 0.5)
drawText(label, x, y + radius + 20f)       // alpha: importance * 255
```

## 📱 UI Layout

### Main Launcher Layout (`activity_launcher.xml`)

```
┌─────────────────────────────────────┐
│ 00:00                            [E]│  ← Top Bar (Time, Date, Profile)
│ MON · 1 JAN 2025                    │
├─────────────────────────────────────┤
│                                     │
│                                     │
│           CosmosView                │  ← Full-screen rendering surface
│       (Physics + Orbits)            │
│                                     │
│                                     │
├─────────────────────────────────────┤
│ [🔍 Search apps...] ⌕              │  ← Search Bar
├─────────────────────────────────────┤
│ [☎] [💬] [🌐] [📷] [⚙]             │  ← Dock (5 quick access buttons)
└─────────────────────────────────────┘
```

### Color Scheme

| Element | Color | Hex |
|---------|-------|-----|
| Background | Deep Blue | #04060f |
| Primary Blue | Indigo | #7b8fff |
| Text | Light Purple | #e8eaf6 |
| Dim Blue | Blue Gray | #447b8fff |

## 🛠️ Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- Android SDK 34
- JDK 17+

### Steps

1. **Clone and open project**
   ```bash
   git clone https://github.com/THREEEMAN/Cosmic.git
   cd Cosmic
   ```

2. **Open in Android Studio** and let Gradle sync

3. **Connect device or start emulator** (API 26+)

4. **Run** → Select device → Install

5. **Set as default launcher**
   - Device: Settings → Apps → Default apps → Home app → Cosmos Launcher

6. **Grant usage access permission** (required)
   - Device: Settings → Apps → Special app access → Usage access → Cosmos Launcher → Allow

## ⚙️ Customization

### Add New Apps to Clusters

Edit `AppRepository.kt`:

```kotlin
private val clusterMap = mapOf(
    // Add your app here:
    "com.example.myapp" to "work",  // or social, creative, utility, finance
    // ...
)
```

### Tune Physics Parameters

Edit `CosmosPhysics.kt`:

```kotlin
private val SPRING_STRENGTH = 0.018f       // ↑ faster orbit snap
private val GRAVITY_STRENGTH = 0.0006f     // ↑ stronger pull inward
private val REPULSION_STRENGTH = 0.35f     // ↑ apps push harder
private val DAMPING = 0.86f                // ↓ floatier; ↑ more stable
private val MAX_VELOCITY = 12f             // ↑ faster max speed
private val CENTER_MASS_STRENGTH = 0.0003f // ↑ stronger on-screen pull
```

### Adjust Importance Weights

Edit `AppNode.kt` in `refreshImportance()`:

```kotlin
importance = (launchScore * 0.45f + usageScore * 0.35f + recencyScore * 0.20f)
             .coerceIn(0.05f, 1f)
```

### Change Orbit Radii

Edit `CosmosPhysics.kt` in `assignOrbits()`:

```kotlin
val baseRadius = 80f + (cluster.hashCode() % 5) * 40f  // adjust base radius
```

## 📋 Permissions Required

| Permission | Purpose |
|-----------|---------|
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `PACKAGE_USAGE_STATS` | Access usage stats for importance scoring |
| `EXPAND_STATUS_BAR` | Expand status bar (optional) |
| `RECEIVE_BOOT_COMPLETED` | Restore launcher on boot |

**Note**: `PACKAGE_USAGE_STATS` must be manually granted in Settings after installation.

## 🔧 Build Configuration

- **Target SDK**: 34
- **Min SDK**: 26
- **Language**: Kotlin
- **Java Version**: 17
- **Namespace**: `com.cosmos.launcher`

### Core Dependencies

```gradle
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.11.0
androidx.constraintlayout:constraintlayout:2.1.4
```

## 🚀 Future Enhancements

- [ ] **Gesture Support**: Swipe between clusters, pinch to zoom
- [ ] **Settings UI**: Live physics tuning sliders
- [ ] **App Shortcuts**: Long-press for app action menu
- [ ] **Widget Support**: Embed widgets in clusters
- [ ] **Search Implementation**: Functional search bar with app filtering
- [ ] **Notification Badges**: Unread count on app nodes
- [ ] **Folder Support**: Organize apps into groups
- [ ] **Custom Wallpapers**: Background variations
- [ ] **Night Mode Variations**: Dynamic theme adjustments
- [ ] **Export/Import Config**: Share custom cluster arrangements

## 📝 Key Classes Reference

| Class | Responsibility |
|-------|-----------------|
| `LauncherActivity` | Entry point, UI init, app launching |
| `CosmosView` | Custom SurfaceView, 60 FPS render thread, touch input |
| `CosmosPhysics` | Physics simulation, force calculations, orbit assignment |
| `CosmosRenderer` | Canvas drawing, visual rendering |
| `AppNode` | Data model, physics state, importance scoring |
| `AppRepository` | App loading, usage stats, categorization |
| `BootReceiver` | Boot completion handler |
| `SettingsActivity` | Settings UI |

## 🎬 Performance

- **Target FPS**: 60 (16ms per frame)
- **Render Thread**: Dedicated, non-blocking
- **Physics Updates**: Per-frame with adaptive timestep (0.016s)
- **Memory**: Optimized for ~100-200 apps per device
- **Battery**: Efficient force calculations; reduced when screen off

## 📄 License

[Add your license here]

## 👨‍💻 Author

**THREEEMAN**

---

**Made with ❤️ and physics** — Where productivity meets the cosmos.
