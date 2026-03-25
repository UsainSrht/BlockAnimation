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
import java.util.logging.Logger;

/**
 * Central manager for active animations.
 * <p>
 * Maintains a registry of running {@link AnimationTask} instances per player
 * and provides methods to start, stop, and query animations.
 *
 * <h3>Concurrency</h3>
 * All public methods are thread-safe. Animation tasks themselves run on
 * the appropriate region thread (Folia) or main thread (Spigot/Paper).
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
    // Play
    // ------------------------------------------------------------------

    /**
     * Start an animation for a player.
     *
     * @param player       target player
     * @param visibleBlocks the blocks to animate
     * @param animation    the animation pattern
     * @param palette      the color gradient
     * @param boundFuture  the controlling future — animation runs until this completes
     * @return the created {@link AnimationTask}
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture) {
        return play(player, visibleBlocks, animation, palette, boundFuture,
                50, 0); // default: 1 tick interval, indefinite duration
    }

    /**
     * Start an animation for a player with custom timing.
     *
     * @param player         target player
     * @param visibleBlocks  the blocks to animate
     * @param animation      the animation pattern
     * @param palette        the color gradient
     * @param boundFuture    the controlling future — animation runs until this completes
     * @param tickIntervalMs tick interval in milliseconds (minimum 50ms = 1 server tick)
     * @param durationMs     total duration in milliseconds (0 = indefinite)
     * @return the created {@link AnimationTask}
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture,
                              long tickIntervalMs, long durationMs) {
        return play(player, visibleBlocks, animation, palette, boundFuture,
                tickIntervalMs, durationMs, defaultReplacer);
    }

    /**
     * Start an animation for a player with full control over all parameters.
     *
     * @param player         target player
     * @param visibleBlocks  the blocks to animate
     * @param animation      the animation pattern
     * @param palette        the color gradient
     * @param boundFuture    the controlling future — animation runs until this completes
     * @param tickIntervalMs tick interval in milliseconds
     * @param durationMs     total duration in milliseconds (0 = indefinite)
     * @param replacer       custom shape-matching replacer
     * @return the created {@link AnimationTask}
     */
    public AnimationTask play(Player player, VisibleBlocks visibleBlocks,
                              Animation animation, ColorPalette palette,
                              CompletableFuture<?> boundFuture,
                              long tickIntervalMs, long durationMs,
                              ShapeMatchingReplacer replacer) {
        AnimationContext ctx = new AnimationContext(
                player, visibleBlocks, palette, replacer,
                Math.max(50, tickIntervalMs), durationMs
        );

        AnimationTask task = new AnimationTask(animation, ctx, boundFuture, plugin, morePaperLib);

        // Register
        activeTasks.computeIfAbsent(player.getUniqueId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(task);

        // Clean up registration when the task stops
        boundFuture.whenComplete((r, t) -> removeTask(player.getUniqueId(), task));

        task.start();

        LOGGER.fine(() -> "Started animation '" + animation.getName() + "' for " + player.getName()
                + " with " + visibleBlocks.getBlocks().size() + " blocks");

        return task;
    }

    // ------------------------------------------------------------------
    // Stop
    // ------------------------------------------------------------------

    /**
     * Stop all active animations for a player.
     */
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

    /**
     * Stop a specific animation task.
     */
    public void stop(AnimationTask task) {
        task.stop();
    }

    /**
     * Stop all animations across all players (called on plugin disable).
     */
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

    /**
     * @return true if the player has any active animations
     */
    public boolean hasActiveAnimations(Player player) {
        List<AnimationTask> tasks = activeTasks.get(player.getUniqueId());
        if (tasks == null) return false;
        synchronized (tasks) {
            return tasks.stream().anyMatch(t -> !t.isStopped());
        }
    }

    /**
     * @return unmodifiable list of active tasks for the player (may be empty)
     */
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
