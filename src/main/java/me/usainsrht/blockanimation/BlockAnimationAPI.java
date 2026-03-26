package me.usainsrht.blockanimation;

import me.usainsrht.blockanimation.animation.Animation;
import me.usainsrht.blockanimation.animation.AnimationEngine;
import me.usainsrht.blockanimation.animation.AnimationTask;
import me.usainsrht.blockanimation.animation.LoopMode;
import me.usainsrht.blockanimation.color.ColorPalette;
import me.usainsrht.blockanimation.visibility.VisibilityAnalyzer;
import me.usainsrht.blockanimation.visibility.VisibleBlocks;
import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Public entry-point API for the BlockAnimation plugin.
 * <p>
 * When used as a plugin: {@code BlockAnimationPlugin.getInstance().getApi()}<br>
 * When shaded as an API: construct directly via the public constructor.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * BlockAnimationAPI api = BlockAnimationPlugin.getInstance().getApi();
 *
 * // Scan visible blocks
 * CompletableFuture<VisibleBlocks> scan = api.calculateVisibleBlocks(location, 16);
 *
 * // Define a gradient
 * ColorPalette palette = new GradientPalette(List.of(
 *     new RGBColor(0, 0, 255),
 *     new RGBColor(128, 0, 255)
 * ));
 *
 * // Option A: play for a fixed duration
 * scan.thenAccept(blocks ->
 *     api.playAnimation(player, blocks, new SpreadAnimation(40), palette,
 *                       5, TimeUnit.SECONDS, LoopMode.REVERSE)
 * );
 *
 * // Option B: play until a future completes
 * CompletableFuture<?> dbTask = CompletableFuture.runAsync(() -> heavyWork());
 * scan.thenAccept(blocks ->
 *     api.playAnimation(player, blocks, new SpreadAnimation(40), palette, dbTask)
 * );
 * }</pre>
 */
public final class BlockAnimationAPI {

    private final JavaPlugin plugin;
    private final MorePaperLib morePaperLib;
    private final AnimationEngine animationEngine;
    private final VisibilityAnalyzer visibilityAnalyzer;

    public BlockAnimationAPI(JavaPlugin plugin, MorePaperLib morePaperLib,
                             AnimationEngine animationEngine) {
        this.plugin = plugin;
        this.morePaperLib = morePaperLib;
        this.animationEngine = animationEngine;
        this.visibilityAnalyzer = new VisibilityAnalyzer(morePaperLib);
    }

    // ------------------------------------------------------------------
    // Visibility
    // ------------------------------------------------------------------

    /**
     * Asynchronously scan for all visible surface blocks around a location.
     *
     * @param center the scan origin
     * @param radius scan radius in blocks (1–64)
     * @return a future that completes with an immutable {@link VisibleBlocks}
     */
    public CompletableFuture<VisibleBlocks> calculateVisibleBlocks(Location center, int radius) {
        return visibilityAnalyzer.analyze(center, radius);
    }

    /** Load a previously saved {@link VisibleBlocks} from disk. */
    public VisibleBlocks loadVisibleBlocks(File file) throws IOException {
        return VisibleBlocks.load(file);
    }

    /** Save a {@link VisibleBlocks} to disk for later reuse. */
    public void saveVisibleBlocks(VisibleBlocks visibleBlocks, File file) throws IOException {
        visibleBlocks.save(file);
    }

    // ------------------------------------------------------------------
    // Animations — future-based
    // ------------------------------------------------------------------

    /**
     * Play animation until the given future completes.
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       CompletableFuture<?> boundFuture) {
        return animationEngine.play(player, visibleBlocks, animation, palette, boundFuture);
    }

    /**
     * Play animation until the given future completes, with custom timing and loop mode.
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       CompletableFuture<?> boundFuture,
                                       long tickIntervalMs, long durationMs,
                                       LoopMode loopMode) {
        return animationEngine.play(player, visibleBlocks, animation, palette,
                boundFuture, tickIntervalMs, durationMs, loopMode);
    }

    // ------------------------------------------------------------------
    // Animations — duration-based (no future)
    // ------------------------------------------------------------------

    /**
     * Play animation for a fixed duration. No {@link CompletableFuture} needed.
     *
     * @param player        target player
     * @param visibleBlocks blocks to animate
     * @param animation     animation pattern
     * @param palette       color gradient
     * @param duration      how long the animation plays
     * @param unit          time unit
     * @param loopMode      how the animation cycles (RESTART, REVERSE, ONCE)
     * @return the running task
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       long duration, TimeUnit unit, LoopMode loopMode) {
        return animationEngine.play(player, visibleBlocks, animation, palette,
                duration, unit, loopMode);
    }

    /**
     * Play animation for a fixed duration with custom tick interval.
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       long duration, TimeUnit unit, LoopMode loopMode,
                                       long tickIntervalMs) {
        return animationEngine.play(player, visibleBlocks, animation, palette,
                duration, unit, loopMode, tickIntervalMs);
    }

    // ------------------------------------------------------------------
    // Stop / Query
    // ------------------------------------------------------------------

    /** Stop all active animations for a player. */
    public void stopAnimations(Player player) {
        animationEngine.stopAll(player);
    }

    /** Check if a player has any running animations. */
    public boolean hasActiveAnimations(Player player) {
        return animationEngine.hasActiveAnimations(player);
    }

    /** @return the underlying animation engine for advanced use */
    public AnimationEngine getAnimationEngine() {
        return animationEngine;
    }

    /** @return the visibility analyzer for advanced use */
    public VisibilityAnalyzer getVisibilityAnalyzer() {
        return visibilityAnalyzer;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Gracefully shut down all animations. Called automatically on plugin disable. */
    public void shutdown() {
        animationEngine.stopAll();
        plugin.getLogger().info("BlockAnimation API shut down.");
    }
}
