package me.usainsrht.blockanimation.animation;

/**
 * Base interface for all animation patterns.
 * <p>
 * Implementations define <i>what</i> happens on each tick (e.g., which blocks
 * change color) and how to cleanly reset when the animation ends.
 *
 * <h3>Custom animations</h3>
 * Developers can create their own by implementing this interface and registering
 * via the {@link AnimationEngine}:
 * <pre>{@code
 * public class MyCustomAnimation implements Animation {
 *     @Override public String getName() { return "my_custom"; }
 *     @Override public void tick(AnimationContext ctx, long elapsedTicks) { ... }
 *     @Override public void reset(AnimationContext ctx) { ... }
 * }
 * }</pre>
 */
public interface Animation {

    /**
     * @return a unique human-readable name for this animation type
     */
    String getName();

    /**
     * Called every tick to update the animation state and send fake block packets.
     *
     * @param ctx          the animation context (player, blocks, palette, etc.)
     * @param elapsedTicks number of ticks since the animation started
     */
    void tick(AnimationContext ctx, long elapsedTicks);

    /**
     * Called when the animation ends (future completed, cancelled, or player disconnected).
     * <p>
     * Implementations should revert all fake blocks back to their original state.
     *
     * @param ctx the animation context
     */
    void reset(AnimationContext ctx);
}
