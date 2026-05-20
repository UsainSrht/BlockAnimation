package me.usainsrht.blockanimation.animation;

import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

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
 * Scheduling bypasses MorePaperLib to avoid silent scheduling failures on Folia 26.x.
 * On Folia the Folia EntityScheduler is called directly via reflection; on non-Folia
 * the standard Bukkit scheduler is used.
 */
public final class AnimationTask {

    /**
     * True when running inside a Folia server.
     * Detected once at class-load time by probing a Folia-only class.
     */
    private static final boolean IS_FOLIA = detectFolia();

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private final Animation animation;
    private final AnimationContext context;
    private final CompletableFuture<?> boundFuture;
    private final JavaPlugin plugin;
    // morePaperLib kept for API / constructor compatibility but no longer used for scheduling
    @SuppressWarnings("unused")
    private final MorePaperLib morePaperLib;

    private final AtomicLong tickCounter = new AtomicLong(0);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /** Cancels the running repeating task; set once after successful scheduling. */
    private volatile Runnable cancelHandle;

    /**
     * @param animation    the animation implementation
     * @param context      animation context (player, blocks, palette, etc.)
     * @param boundFuture  the controlling future, or {@code null} for duration-only mode
     * @param plugin       the plugin instance
     * @param morePaperLib scheduling abstraction (kept for API compatibility)
     */
    public AnimationTask(Animation animation, AnimationContext context,
                         CompletableFuture<?> boundFuture,
                         JavaPlugin plugin,
                         MorePaperLib morePaperLib) {
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
        if (boundFuture != null) {
            boundFuture.whenComplete((result, throwable) -> stop());
        }

        long intervalTicks = context.tickIntervalTicks();

        Runnable tickRunnable = () -> {
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

            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "[BlockAnimation] Animation tick error for " + animation.getName(), t);
                stop();
            }
        };

        if (IS_FOLIA) {
            startFolia(tickRunnable, intervalTicks);
        } else {
            startBukkit(tickRunnable, intervalTicks);
        }
    }

    /** Schedule on Folia via direct EntityScheduler reflection. */
    private void startFolia(Runnable tickRunnable, long intervalTicks) {
        try {
            // player.getScheduler() → EntityScheduler
            Method getScheduler = context.player().getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(context.player());

            // EntityScheduler.runAtFixedRate(Plugin, Consumer<ScheduledTask>, Runnable, long, long)
            Method runAtFixedRate = entityScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class, Consumer.class, Runnable.class, long.class, long.class);

            // retired callback: called by Folia when the entity is removed (player quits)
            Runnable retired = this::stop;

            Object taskObj = runAtFixedRate.invoke(
                    entityScheduler,
                    plugin,
                    (Consumer<Object>) st -> tickRunnable.run(),
                    retired,
                    intervalTicks,
                    intervalTicks);

            if (taskObj != null) {
                Method cancelMethod = taskObj.getClass().getMethod("cancel");
                cancelHandle = () -> {
                    try {
                        cancelMethod.invoke(taskObj);
                    } catch (Exception ex) {
                        // best-effort cancel
                    }
                };
            } else {
                plugin.getLogger().warning(
                        "[BlockAnimation] Folia EntityScheduler.runAtFixedRate returned null for player "
                                + context.player().getName() + ". Animation will not run.");
            }

        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE,
                    "[BlockAnimation] Failed to schedule animation task on Folia", t);
        }
    }

    /** Schedule on non-Folia via Bukkit scheduler. */
    private void startBukkit(Runnable tickRunnable, long intervalTicks) {
        try {
            org.bukkit.scheduler.BukkitTask task =
                    Bukkit.getScheduler().runTaskTimer(plugin, tickRunnable, intervalTicks, intervalTicks);
            cancelHandle = task::cancel;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE,
                    "[BlockAnimation] Failed to schedule animation task", t);
        }
    }

    /**
     * Gracefully stop the animation: cancel the repeating task and revert blocks.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;

        // Cancel the tick loop first
        Runnable handle = cancelHandle;
        if (handle != null) {
            try {
                handle.run();
            } catch (Throwable t) {
                // best-effort cancel
            }
        }

        // Schedule the block-revert on the correct thread
        try {
            if (context.player().isOnline()) {
                Runnable resetTask = () -> {
                    try {
                        animation.reset(context);
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.WARNING,
                                "[BlockAnimation] Animation reset error for " + animation.getName(), t);
                    }
                };

                if (IS_FOLIA) {
                    scheduleResetFolia(resetTask);
                } else {
                    Bukkit.getScheduler().runTask(plugin, resetTask);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE,
                    "[BlockAnimation] Unexpected error stopping animation", t);
        }
    }

    /** Schedule the reset runnable on Folia's EntityScheduler. */
    private void scheduleResetFolia(Runnable resetTask) {
        try {
            Method getScheduler = context.player().getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(context.player());

            // EntityScheduler.run(Plugin, Consumer<ScheduledTask>, Runnable)
            Method run = entityScheduler.getClass().getMethod(
                    "run", Plugin.class, Consumer.class, Runnable.class);

            run.invoke(entityScheduler, plugin,
                    (Consumer<Object>) st -> resetTask.run(),
                    (Runnable) () -> { /* entity removed — skip reset */ });

        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "[BlockAnimation] Failed to schedule animation reset on Folia", t);
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
