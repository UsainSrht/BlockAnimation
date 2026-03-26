package dev.blockanimation.animation;

import dev.blockanimation.color.ColorPalette;

import dev.blockanimation.shape.ShapeMatchingReplacer;
import dev.blockanimation.visibility.VisibleBlocks;
import org.bukkit.entity.Player;

/**
 * Immutable context passed to every {@link Animation#tick} call.
 * <p>
 * Contains all the resources an animation implementation needs:
 * the target player, visible blocks, color palette, shape replacer,
 * timing information, and loop mode.
 *
 * @param player         the player receiving fake block packets
 * @param visibleBlocks  the pre-calculated visible surface blocks
 * @param palette        the color gradient to animate through
 * @param replacer       shape-aware block replacement logic
 * @param tickIntervalMs interval between ticks in milliseconds
 * @param durationMs     total animation duration in milliseconds (0 = indefinite / until future completes)
 * @param loopMode       how the animation cycles when it reaches the end of a sweep
 */
public record AnimationContext(
        Player player,
        VisibleBlocks visibleBlocks,
        ColorPalette palette,
        ShapeMatchingReplacer replacer,
        long tickIntervalMs,
        long durationMs,
        LoopMode loopMode
) {

    /**
     * Backwards-compatible constructor defaulting to {@link LoopMode#RESTART}.
     */
    public AnimationContext(Player player, VisibleBlocks visibleBlocks, ColorPalette palette,
                            ShapeMatchingReplacer replacer, long tickIntervalMs, long durationMs) {
        this(player, visibleBlocks, palette, replacer, tickIntervalMs, durationMs, LoopMode.RESTART);
    }

    /**
     * Convenience: duration in ticks (20 tps).
     */
    public long durationTicks() {
        return durationMs / 50;
    }

    /**
     * Convenience: tick interval in server ticks.
     */
    public long tickIntervalTicks() {
        return Math.max(1, tickIntervalMs / 50);
    }

    /**
     * Compute the current progress (0..1) within a cycle, accounting for the loop mode.
     *
     * @param elapsedTicks ticks since animation started
     * @param cycleTicks   ticks for one full cycle
     * @return progress in [0, 1]
     */
    public float resolveProgress(long elapsedTicks, long cycleTicks) {
        return loopMode.resolveProgress(elapsedTicks, cycleTicks);
    }
}
