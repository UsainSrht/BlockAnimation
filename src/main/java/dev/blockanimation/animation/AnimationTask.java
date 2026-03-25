package dev.blockanimation.animation;

import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.ScheduledTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives an {@link Animation} on a repeating schedule and binds its lifecycle
 * to a {@link CompletableFuture}.
 * <p>
 * The task:
 * <ol>
 *   <li>Calls {@link Animation#tick(AnimationContext, long)} every N ticks.</li>
 *   <li>Monitors the bound future — when it completes (normally or exceptionally),
 *       calls {@link Animation#reset(AnimationContext)} and self-cancels.</li>
 *   <li>Also cancels if the player goes offline.</li>
 * </ol>
 *
 * Scheduling is done via {@link MorePaperLib} for Folia region safety.
 */
public final class AnimationTask {

    private static final Logger LOGGER = Logger.getLogger(AnimationTask.class.getName());

    private final Animation animation;
    private final AnimationContext context;
    private final CompletableFuture<?> boundFuture;
    private final MorePaperLib morePaperLib;
    private final JavaPlugin plugin;

    private final AtomicLong tickCounter = new AtomicLong(0);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private volatile ScheduledTask scheduledTask;

    /**
     * @param animation   the animation implementation
     * @param context     animation context (player, blocks, palette, etc.)
     * @param boundFuture the controlling future — animation stops when this completes
     * @param plugin      owning plugin
     * @param morePaperLib scheduling abstraction
     */
    public AnimationTask(Animation animation, AnimationContext context,
                         CompletableFuture<?> boundFuture,
                         JavaPlugin plugin, MorePaperLib morePaperLib) {
        this.animation = animation;
        this.context = context;
        this.boundFuture = boundFuture;
        this.plugin = plugin;
        this.morePaperLib = morePaperLib;
    }

    /**
     * Start the animation loop.
     */
    public void start() {
        // Watch the bound future — when it completes, stop the animation
        boundFuture.whenComplete((result, throwable) -> stop());

        long intervalTicks = context.tickIntervalTicks();

        // Schedule repeating task on the player's entity scheduler (Folia-safe).
        // AttachedScheduler.runAtFixedRate(command, alternateIfRemoved, initialDelay, period)
        // alternateIfRemoved is called if the entity is removed (player disconnects).
        scheduledTask = morePaperLib.scheduling()
                .entitySpecificScheduler(context.player())
                .runAtFixedRate(() -> {
                    try {
                        if (stopped.get()) return;

                        // Player disconnect check
                        if (!context.player().isOnline()) {
                            stop();
                            return;
                        }

                        long elapsed = tickCounter.getAndAdd(intervalTicks);
                        animation.tick(context, elapsed);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Animation tick error for " + animation.getName(), e);
                        stop();
                    }
                }, this::stop, // alternateIfRemoved – called when entity is removed
                intervalTicks, intervalTicks);
    }

    /**
     * Gracefully stop the animation: revert blocks and cancel the scheduled task.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) return; // already stopped

        try {
            // Revert blocks on the player's entity thread
            if (context.player().isOnline()) {
                morePaperLib.scheduling()
                        .entitySpecificScheduler(context.player())
                        .run(() -> {
                            try {
                                animation.reset(context);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Animation reset error for " + animation.getName(), e);
                            }
                        }, () -> { /* entity removed, nothing to revert */ });
            }
        } finally {
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }
    }

    /**
     * @return true if this task has been stopped
     */
    public boolean isStopped() {
        return stopped.get();
    }

    /**
     * @return the animation implementation being driven
     */
    public Animation getAnimation() {
        return animation;
    }
}
