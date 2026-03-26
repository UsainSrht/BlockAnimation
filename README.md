# BlockAnimation

Async visible-block detection and packet-based color-shift animation API for **Spigot**, **Paper**, and **Folia**.

Scan nearby visible surface blocks around any location, then play smooth, client-side color animations on them — without touching the actual world.

---

## Features

- **Visibility Scanner** — Async BFS flood-fill that detects all visible surface blocks in a radius, respecting solidity and transparency. Results are immutable, serializable, and reusable.
- **Packet-Based Rendering** — Animations are strictly client-side (fake blocks via `Player.sendBlockChange`). Zero impact on world data or server state.
- **Shape Preservation** — Stairs stay stairs, slabs stay slabs. The material replacement system preserves block shapes, orientations, and waterlogged states.
- **3 Built-In Animations**
  - **Spread** — Radial wave expanding from one or more centers. Colors emanate outward like ripples.
  - **Pass-Through** — Directional sweep along any yaw angle, with optional trailing color wave.
  - **Noise Sparkle** — Random blocks shimmer through the palette each tick.
- **Loop Modes** — `RESTART` (jump back), `REVERSE` (ping-pong), `ONCE` (play and hold).
- **Folia Support** — All scheduling uses [MorePaperLib](https://github.com/A248/MorePaperLib) for region-aware, thread-safe execution.
- **Dual Build** — Ships as both a standalone plugin (with test commands) and a shadeable API library.

---

## As a Plugin

Drop the JAR into your `plugins/` folder. Use the built-in test command to try animations in-game.

### Commands

**Permission:** `blockanimation.test`

Colors are comma-separated hex values (e.g. `ff0000,00ff00,0000ff`).

```
/ba spread <radius> <duration> <colors> [loopMode] [sweepTicks] [cycleTicks]
/ba passthrough <radius> <duration> <colors> [loopMode] [trail] [yaw] [sweepTicks] [cycleTicks] [bandWidth]
/ba sparkle <radius> <duration> <colors> [loopMode] [density] [cycleTicks]
/ba stop
```

### Examples

```bash
# Red-to-blue radial wave, 16 block radius, 10 seconds, ping-pong
/ba spread 16 10 ff0000,0000ff reverse

# Green sweep from north (yaw=180), with trailing wave
/ba passthrough 12 15 00ff00,ffff00 restart true 180

# Rainbow sparkle, 20% density
/ba sparkle 10 8 ff0000,ff8800,ffff00,00ff00,0000ff,8800ff restart 0.2

# Stop all animations
/ba stop
```

All optional arguments show their **default values** as tab-complete hints.

---

## As an API

Add BlockAnimation as a dependency and shade it into your plugin.

### Maven

```xml
<dependency>
    <groupId>dev.blockanimation</groupId>
    <artifactId>BlockAnimation</artifactId>
    <version>1.0.1</version>
    <classifier>api</classifier>
    <scope>compile</scope>
</dependency>
```

> **Note:** You must also depend on and shade [MorePaperLib](https://github.com/A248/MorePaperLib) yourself when using the API JAR.

### Setup

```java
// In your plugin's onEnable()
MorePaperLib morePaperLib = new MorePaperLib(this);
AnimationEngine engine = new AnimationEngine(this, morePaperLib);
BlockAnimationAPI api = new BlockAnimationAPI(this, morePaperLib, engine);
```

### Scanning Visible Blocks

```java
CompletableFuture<VisibleBlocks> scan = api.calculateVisibleBlocks(player.getLocation(), 16);
```

Results can be saved/loaded for reuse:

```java
api.saveVisibleBlocks(blocks, new File(getDataFolder(), "scan.json"));
VisibleBlocks loaded = api.loadVisibleBlocks(new File(getDataFolder(), "scan.json"));
```

### Playing Animations

```java
// Define a gradient
ColorPalette palette = new GradientPalette(List.of(
    new RGBColor(255, 0, 0),    // Red
    new RGBColor(0, 255, 0),    // Green
    new RGBColor(0, 0, 255)     // Blue
));

// Option A: Play for a fixed duration
api.playAnimation(player, blocks,
    new SpreadAnimation(40, 60),  // 2s spread, 3s cycle
    palette,
    5, TimeUnit.SECONDS,
    LoopMode.REVERSE
);

// Option B: Play until a CompletableFuture completes
CompletableFuture<?> task = CompletableFuture.runAsync(() -> longRunningWork());
api.playAnimation(player, blocks,
    new SpreadAnimation(),
    palette,
    task  // animation runs until this completes
);
```

### Animation Types

```java
// Radial wave from player's location (or multiple centers)
new SpreadAnimation(sweepTicks, cycleTicks);
new SpreadAnimation(sweepTicks, cycleTicks, List.of(loc1, loc2));

// Directional sweep with optional trailing wave
new PassThroughAnimation(yawDegrees, sweepTicks, cycleTicks, bandWidth, trail);

// Random shimmer effect
new NoiseSparkleAnimation(density, cycleTicks);
```

### Stopping Animations

```java
api.stopAnimations(player);       // Stop all for a player
api.hasActiveAnimations(player);   // Check if any are running
```

### Custom Animations

Implement the `Animation` interface:

```java
public class MyAnimation implements Animation {
    @Override
    public String getName() { return "my_animation"; }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        // Called every tick — send fake block packets here
    }

    @Override
    public void reset(AnimationContext ctx) {
        // Called when animation ends — revert blocks
        FakeBlockSender.revert(ctx.player(), ctx.visibleBlocks().getBlocks());
    }
}
```

---

## Building

Requires **Java 17+** and **Maven**.

```bash
# Plugin JAR (default) — shades MorePaperLib, includes test command
mvn clean package

# API JAR — slim library for shading into other plugins
mvn clean package -Papi
```

---

## Requirements

- **Server:** Spigot, Paper, or Folia 1.21+
- **Java:** 17+
- **Test command:** Requires Paper (uses Brigadier command API)
- **Core API:** Works on Spigot, Paper, and Folia

---

## License

[MIT](LICENSE)
