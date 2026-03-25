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
 * and timing information.
 *
 * @param player         the player receiving fake block packets
 * @param visibleBlocks  the pre-calculated visible surface blocks
 * @param palette        the color gradient to animate through
 * @param replacer       shape-aware block replacement logic
 * @param tickIntervalMs interval between ticks in milliseconds
 * @param durationMs     total animation duration in milliseconds (0 = indefinite / until future completes)
 */
public record AnimationContext(
        Player player,
        VisibleBlocks visibleBlocks,
        ColorPalette palette,
        ShapeMatchingReplacer replacer,
        long tickIntervalMs,
        long durationMs
) {
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
}
