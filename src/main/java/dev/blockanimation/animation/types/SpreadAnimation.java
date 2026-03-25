package dev.blockanimation.animation.types;

import dev.blockanimation.animation.Animation;
import dev.blockanimation.animation.AnimationContext;
import dev.blockanimation.color.RGBColor;
import dev.blockanimation.packet.FakeBlockSender;
import dev.blockanimation.shape.ShapeMatchingReplacer;
import dev.blockanimation.visibility.BlockInfo;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Spread (Radial)</b> animation — expands outward from the center of the
 * {@link dev.blockanimation.visibility.VisibleBlocks} like a shockwave.
 * <p>
 * On each tick the "active radius" grows; blocks whose distance ≤ active radius
 * are colored according to their distance-based position in the palette
 * (nearest → start of gradient, farthest → end).
 *
 * <h3>Looping</h3>
 * Once the wavefront reaches the maximum radius, the animation resets and loops.
 */
public final class SpreadAnimation implements Animation {

    /** Duration of one full sweep in ticks (blocks-per-tick growth speed) */
    private final long sweepDurationTicks;

    /**
     * @param sweepDurationTicks how many ticks one full radial sweep takes
     *                           (e.g., 40 = 2 seconds at 20 tps)
     */
    public SpreadAnimation(long sweepDurationTicks) {
        this.sweepDurationTicks = Math.max(1, sweepDurationTicks);
    }

    /** Default: 2-second sweep. */
    public SpreadAnimation() {
        this(40);
    }

    @Override
    public String getName() {
        return "spread";
    }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        List<BlockInfo> blocks = ctx.visibleBlocks().getBlocks();
        if (blocks.isEmpty()) return;

        double maxDistSq = ctx.visibleBlocks().getMaxDistanceSquared();
        if (maxDistSq <= 0) maxDistSq = 1;
        double maxDist = Math.sqrt(maxDistSq);

        // Progress within the current sweep (0..1)
        float sweepProgress = (float) ((elapsedTicks % sweepDurationTicks) / (double) sweepDurationTicks);

        double activeRadius = sweepProgress * maxDist;
        double activeRadiusSq = activeRadius * activeRadius;

        double cx = ctx.visibleBlocks().getCenterX();
        double cy = ctx.visibleBlocks().getCenterY();
        double cz = ctx.visibleBlocks().getCenterZ();

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (BlockInfo block : blocks) {
            double distSq = block.distanceSquared(cx, cy, cz);

            if (distSq <= activeRadiusSq) {
                // Color based on normalized distance from center
                float palettePos = (float) (Math.sqrt(distSq) / maxDist);
                RGBColor color = ctx.palette().getColorAt(palettePos);

                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);
            }
        }

        if (!changes.isEmpty()) {
            FakeBlockSender.sendBatch(ctx.player(), changes);
        }
    }

    @Override
    public void reset(AnimationContext ctx) {
        FakeBlockSender.revert(ctx.player(), ctx.visibleBlocks().getBlocks());
    }
}
