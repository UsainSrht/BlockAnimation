package dev.blockanimation.animation;

import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives an {@link Animation} on a repeating schedule.
 * <p>
 * Lifecycle can be controlled by either:
 * <ul>
 *   <li>A {@link CompletableFuture} — animation runs until it completes.</li>
 *   <li>A duration — animation auto-stops after the configured time.</li>
 *   <li>Both — whichever completes first wins.</li>
 * </ul>
 *
 * Scheduling is done via {@link MorePaperLib} for Folia region safety.
 */
public final class AnimationTask {

    private static final Logger LOGGER = Logger.getLogger(AnimationTask.class.getName());

    private final Animation animation;
    private final AnimationContext context;
    private final CompletableFuture<?> boundFuture; // nullable — may be absent for duration-only
    private final MorePaperLib morePaperLib;

    private final AtomicLong tickCounter = new AtomicLong(0);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private volatile ScheduledTask scheduledTask;

    /**
     * @param animation    the animation implementation
     * @param context      animation context (player, blocks, palette, etc.)
     * @param boundFuture  the controlling future, or {@code null} for duration-only mode
     * @param morePaperLib scheduling abstraction
     */
    public AnimationTask(Animation animation, AnimationContext context,
                         CompletableFuture<?> boundFuture,
                         MorePaperLib morePaperLib) {
        this.animation = animation;
        this.context = context;
        this.boundFuture = boundFuture;
        this.morePaperLib = morePaperLib;
    }

    /**
     * Start the animation loop.
     */
    public void start() {
        // Watch the bound future — when it completes, stop the animation
        if (boundFuture != null) {
            boundFuture.whenComplete((result, throwable) -> stop());
        }

        long intervalTicks = context.tickIntervalTicks();

        // Schedule repeating task on the player's entity scheduler (Folia-safe).
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

                        // Duration auto-stop check
                        if (context.durationMs() > 0) {
                            long elapsedMs = elapsed * 50; // ticks → ms
                            if (elapsedMs >= context.durationMs()) {
                                stop();
                                return;
                            }
                        }

                        animation.tick(context, elapsed);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Animation tick error for " + animation.getName(), e);
                        stop();
                    }
                }, this::stop, // alternateIfRemoved – entity removed
                intervalTicks, intervalTicks);
    }

    /**
     * Gracefully stop the animation: revert blocks and cancel the scheduled task.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;

        try {
            if (context.player().isOnline()) {
                morePaperLib.scheduling()
                        .entitySpecificScheduler(context.player())
                        .run(() -> {
                            try {
                                animation.reset(context);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Animation reset error for " + animation.getName(), e);
                            }
                        }, () -> { /* entity removed */ });
            }
        } finally {
            if (scheduledTask != null) {
                scheduledTask.cancel();
            }
        }
    }

    /** @return true if this task has been stopped */
    public boolean isStopped() {
        return stopped.get();
    }

    /** @return the animation implementation being driven */
    public Animation getAnimation() {
        return animation;
    }

    /** @return the animation context */
    public AnimationContext getContext() {
        return context;
    }
}
