package dev.blockanimation.animation;

import dev.blockanimation.color.ColorPalette;
import dev.blockanimation.shape.ShapeMatchingReplacer;
import dev.blockanimation.visibility.VisibleBlocks;
import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Central manager for active animations.
 * <p>
 * Maintains a registry of running {@link AnimationTask} instances per player
 * and provides methods to start, stop, and query animations.
 * <p>
 * Animations can be driven by a {@link CompletableFuture} (runs until it completes),
 * by a fixed duration, or both (whichever ends first).
 *
 * <h3>Concurrency</h3>
 * All public methods are thread-safe.
 */
public final class AnimationEngine {

    private static final Logger LOGGER = Logger.getLogger(AnimationEngine.class.getName());

    private final JavaPlugin plugin;
    private final MorePaperLib morePaperLib;
    private final ShapeMatchingReplacer defaultReplacer;

    /** Player UUID → list of active animation tasks */
    private final Map<UUID, List<AnimationTask>> activeTasks = new ConcurrentHashMap<>();

    public AnimationEngine(JavaPlugin plugin, MorePaperLib morePaperLib) {
        this.plugin = plugin;
        this.morePaperLib = morePaperLib;
        this.defaultReplacer = new ShapeMatchingReplacer(true);
    }

    // ------------------------------------------------------------------
    // Play (future-based)
    // ------------------------------------------------------------------

    /**
     * Start an animation driven by a {@link CompletableFuture}.
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture) {
        return play(player, visibleBlocks, animation, palette, boundFuture,
                100, 0, LoopMode.RESTART);
    }

    /**
     * Start an animation driven by a future, with custom timing and loop mode.
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture,
                              long tickIntervalMs, long durationMs) {
        return play(player, visibleBlocks, animation, palette, boundFuture,
                tickIntervalMs, durationMs, LoopMode.RESTART);
    }

    /**
     * Start an animation driven by a future, with full control.
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture,
                              long tickIntervalMs, long durationMs,
                              LoopMode loopMode) {
        return play(player, visibleBlocks, animation, palette, boundFuture,
                tickIntervalMs, durationMs, loopMode, defaultReplacer);
    }

    // ------------------------------------------------------------------
    // Play (duration-based — no future required)
    // ------------------------------------------------------------------

    /**
     * Start an animation for a fixed duration. No {@link CompletableFuture} needed.
     *
     * @param player        target player
     * @param visibleBlocks blocks to animate
     * @param animation     animation pattern
     * @param palette       color gradient
     * @param duration      how long the animation plays
     * @param unit          time unit for the duration
     * @param loopMode      how the animation cycles
     * @return the running task
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              long duration, TimeUnit unit, LoopMode loopMode) {
        return play(player, visibleBlocks, animation, palette, null,
                100, unit.toMillis(duration), loopMode, defaultReplacer);
    }

    /**
     * Start an animation for a fixed duration with custom tick interval.
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              long duration, TimeUnit unit, LoopMode loopMode,
                              long tickIntervalMs) {
        return play(player, visibleBlocks, animation, palette, null,
                tickIntervalMs, unit.toMillis(duration), loopMode, defaultReplacer);
    }

    // ------------------------------------------------------------------
    // Core play (all roads lead here)
    // ------------------------------------------------------------------

    /**
     * Start an animation with full control over all parameters.
     *
     * @param player         target player
     * @param visibleBlocks  blocks to animate
     * @param animation      animation pattern
     * @param palette        color gradient
     * @param boundFuture    controlling future (nullable — use duration if null)
     * @param tickIntervalMs tick interval in ms
     * @param durationMs     total duration in ms (0 = indefinite, requires future)
     * @param loopMode       how the animation cycles
     * @param replacer       shape-matching replacer
     * @return the running task
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture,
                              long tickIntervalMs, long durationMs,
                              LoopMode loopMode, ShapeMatchingReplacer replacer) {
        AnimationContext ctx = new AnimationContext(
                player, visibleBlocks, palette, replacer,
                Math.max(50, tickIntervalMs), durationMs, loopMode
        );

        AnimationTask task = new AnimationTask(animation, ctx, boundFuture, morePaperLib);

        // Register
        activeTasks.computeIfAbsent(player.getUniqueId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(task);

        // Clean up registration when the task stops
        Runnable cleanup = () -> removeTask(player.getUniqueId(), task);
        if (boundFuture != null) {
            boundFuture.whenComplete((r, t) -> cleanup.run());
        }

        task.start();

        LOGGER.fine(() -> "Started animation '" + animation.getName() + "' for " + player.getName()
                + " with " + visibleBlocks.getBlocks().size() + " blocks, loopMode=" + loopMode);

        return task;
    }

    // ------------------------------------------------------------------
    // Stop
    // ------------------------------------------------------------------

    /** Stop all active animations for a player. */
    public void stopAll(Player player) {
        List<AnimationTask> tasks = activeTasks.remove(player.getUniqueId());
        if (tasks != null) {
            synchronized (tasks) {
                for (AnimationTask task : tasks) {
                    task.stop();
                }
            }
        }
    }

    /** Stop a specific animation task. */
    public void stop(AnimationTask task) {
        task.stop();
    }

    /** Stop all animations across all players (called on plugin disable). */
    public void stopAll() {
        for (Map.Entry<UUID, List<AnimationTask>> entry : activeTasks.entrySet()) {
            List<AnimationTask> tasks = entry.getValue();
            synchronized (tasks) {
                for (AnimationTask task : tasks) {
                    task.stop();
                }
            }
        }
        activeTasks.clear();
    }

    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    /** @return true if the player has any active animations */
    public boolean hasActiveAnimations(Player player) {
        List<AnimationTask> tasks = activeTasks.get(player.getUniqueId());
        if (tasks == null) return false;
        synchronized (tasks) {
            return tasks.stream().anyMatch(t -> !t.isStopped());
        }
    }

    /** @return unmodifiable list of active tasks for the player */
    public List<AnimationTask> getActiveTasks(Player player) {
        List<AnimationTask> tasks = activeTasks.get(player.getUniqueId());
        if (tasks == null) return Collections.emptyList();
        synchronized (tasks) {
            return List.copyOf(tasks);
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void removeTask(UUID playerId, AnimationTask task) {
        List<AnimationTask> tasks = activeTasks.get(playerId);
        if (tasks != null) {
            synchronized (tasks) {
                tasks.remove(task);
                if (tasks.isEmpty()) {
                    activeTasks.remove(playerId);
                }
            }
        }
    }
}
