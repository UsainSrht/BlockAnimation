package dev.blockanimation;

import dev.blockanimation.animation.Animation;
import dev.blockanimation.animation.AnimationEngine;
import dev.blockanimation.animation.AnimationTask;
import dev.blockanimation.color.ColorPalette;
import dev.blockanimation.visibility.VisibilityAnalyzer;
import dev.blockanimation.visibility.VisibleBlocks;
import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Public entry-point API for the BlockAnimation plugin.
 * <p>
 * External plugins obtain this via {@code BlockAnimationPlugin.getInstance().getApi()}.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * BlockAnimationAPI api = BlockAnimationPlugin.getInstance().getApi();
 *
 * // 1. Calculate (or load) visible blocks
 * CompletableFuture<VisibleBlocks> scan = api.calculateVisibleBlocks(location, 16);
 *
 * // 2. Define a palette
 * ColorPalette palette = new GradientPalette(List.of(
 *     new RGBColor(0, 0, 255),
 *     new RGBColor(128, 0, 255)
 * ));
 *
 * // 3. Start a long-running task
 * CompletableFuture<?> dbTask = CompletableFuture.runAsync(() -> heavyDatabaseWork());
 *
 * // 4. Play an animation that runs until the task finishes
 * scan.thenAccept(visibleBlocks ->
 *     api.playAnimation(player, visibleBlocks, new SpreadAnimation(), palette, dbTask)
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

    /**
     * Load a previously saved {@link VisibleBlocks} from disk.
     *
     * @param file the JSON file
     * @return the deserialized data
     * @throws IOException on read failure
     */
    public VisibleBlocks loadVisibleBlocks(File file) throws IOException {
        return VisibleBlocks.load(file);
    }

    /**
     * Save a {@link VisibleBlocks} to disk for later reuse.
     *
     * @param visibleBlocks the data to persist
     * @param file          target file
     * @throws IOException on write failure
     */
    public void saveVisibleBlocks(VisibleBlocks visibleBlocks, File file) throws IOException {
        visibleBlocks.save(file);
    }

    // ------------------------------------------------------------------
    // Animations
    // ------------------------------------------------------------------

    /**
     * Start an animation for a player, driven until the given future completes.
     *
     * @param player        target player
     * @param visibleBlocks blocks to animate
     * @param animation     animation pattern (e.g., {@code new SpreadAnimation()})
     * @param palette       color gradient
     * @param boundFuture   the animation runs until this future completes
     * @return the running {@link AnimationTask}
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       CompletableFuture<?> boundFuture) {
        return animationEngine.play(player, visibleBlocks, animation, palette, boundFuture);
    }

    /**
     * Start an animation with custom timing.
     *
     * @param player         target player
     * @param visibleBlocks  blocks to animate
     * @param animation      animation pattern
     * @param palette        color gradient
     * @param boundFuture    controlling future
     * @param tickIntervalMs tick interval in ms (min 50)
     * @param durationMs     total duration in ms (0 = indefinite)
     * @return the running {@link AnimationTask}
     */
    public AnimationTask playAnimation(Player player, VisibleBlocks visibleBlocks,
                                       Animation animation, ColorPalette palette,
                                       CompletableFuture<?> boundFuture,
                                       long tickIntervalMs, long durationMs) {
        return animationEngine.play(player, visibleBlocks, animation, palette,
                boundFuture, tickIntervalMs, durationMs);
    }

    /**
     * Stop all active animations for a player.
     */
    public void stopAnimations(Player player) {
        animationEngine.stopAll(player);
    }

    /**
     * Check if a player has any running animations.
     */
    public boolean hasActiveAnimations(Player player) {
        return animationEngine.hasActiveAnimations(player);
    }

    /**
     * @return the underlying animation engine for advanced use
     */
    public AnimationEngine getAnimationEngine() {
        return animationEngine;
    }

    /**
     * @return the visibility analyzer for advanced use
     */
    public VisibilityAnalyzer getVisibilityAnalyzer() {
        return visibilityAnalyzer;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Gracefully shut down all animations and release resources.
     * Called automatically on plugin disable.
     */
    public void shutdown() {
        animationEngine.stopAll();
        plugin.getLogger().info("BlockAnimation API shut down.");
    }
}
