package dev.blockanimation.animation.types;

import dev.blockanimation.animation.Animation;
import dev.blockanimation.animation.AnimationContext;
import dev.blockanimation.color.RGBColor;
import dev.blockanimation.packet.FakeBlockSender;
import dev.blockanimation.shape.ShapeMatchingReplacer;
import dev.blockanimation.visibility.BlockInfo;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * <b>Spread (Radial)</b> animation — expands outward from one or more centers
 * like a shockwave, then keeps all reached blocks colored with a continuously
 * cycling gradient.
 *
 * <h3>Behavior</h3>
 * <ol>
 *   <li>A wavefront expands outward at a configurable speed.</li>
 *   <li>Once a block is "reached" by the wavefront, it <b>stays colored</b>.</li>
 *   <li>The entire palette continuously shifts over time — all colored blocks
 *       cycle through the gradient together, offset by their distance from the center.</li>
 *   <li>When the wavefront has reached max radius, the palette simply keeps cycling
 *       on all blocks until the animation ends.</li>
 * </ol>
 *
 * <h3>Multi-center</h3>
 * Supports multiple spread origins. Each block uses the distance to the
 * <i>nearest</i> center for its wavefront check and palette position.
 */
public final class SpreadAnimation implements Animation {

    /** Ticks for the wavefront to reach the maximum radius. */
    private final long spreadDurationTicks;

    /** Ticks for one full palette cycle (how fast the gradient shifts). */
    private final long cycleDurationTicks;

    /**
     * Custom spread centers. If null/empty, uses the VisibleBlocks center.
     * Stored as [x, y, z] triples.
     */
    private final double[][] centers;

    /**
     * Full constructor.
     *
     * @param spreadDurationTicks ticks for wavefront to reach max radius (e.g., 40 = 2s)
     * @param cycleDurationTicks  ticks for one full palette color cycle (e.g., 60 = 3s)
     * @param centers             spread origins (may be empty for default center)
     */
    public SpreadAnimation(long spreadDurationTicks, long cycleDurationTicks, List<Location> centers) {
        this.spreadDurationTicks = Math.max(1, spreadDurationTicks);
        this.cycleDurationTicks = Math.max(1, cycleDurationTicks);
        if (centers != null && !centers.isEmpty()) {
            this.centers = new double[centers.size()][3];
            for (int i = 0; i < centers.size(); i++) {
                Location loc = centers.get(i);
                this.centers[i] = new double[]{loc.getX(), loc.getY(), loc.getZ()};
            }
        } else {
            this.centers = null; // will use VisibleBlocks center
        }
    }

    /**
     * Single-center convenience.
     *
     * @param spreadDurationTicks ticks for the wavefront
     * @param cycleDurationTicks  ticks for one palette cycle
     */
    public SpreadAnimation(long spreadDurationTicks, long cycleDurationTicks) {
        this(spreadDurationTicks, cycleDurationTicks, null);
    }

    /**
     * Simple constructor — 2s spread, 3s cycle.
     */
    public SpreadAnimation(long spreadDurationTicks) {
        this(spreadDurationTicks, 60);
    }

    /** Default: 2s spread, 3s cycle. */
    public SpreadAnimation() {
        this(40, 60);
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

        // Resolve centers
        double[][] activeCenters = this.centers;
        if (activeCenters == null || activeCenters.length == 0) {
            activeCenters = new double[][]{{
                    ctx.visibleBlocks().getCenterX(),
                    ctx.visibleBlocks().getCenterY(),
                    ctx.visibleBlocks().getCenterZ()
            }};
        }

        // Wavefront expansion progress (0..1, clamped at 1 once fully spread)
        float spreadProgress = Math.min(1.0f, (float) elapsedTicks / spreadDurationTicks);

        // Active radius (in blocks)
        double activeRadius = spreadProgress * maxDist;
        double activeRadiusSq = activeRadius * activeRadius;

        // Palette cycle phase — uses LoopMode
        float cyclePhase = ctx.resolveProgress(elapsedTicks, cycleDurationTicks);

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (BlockInfo block : blocks) {
            // Distance to nearest center
            double nearestDistSq = Double.MAX_VALUE;
            for (double[] center : activeCenters) {
                double dx = (block.x() + 0.5) - center[0];
                double dy = (block.y() + 0.5) - center[1];
                double dz = (block.z() + 0.5) - center[2];
                double dSq = dx * dx + dy * dy + dz * dz;
                if (dSq < nearestDistSq) nearestDistSq = dSq;
            }

            // Has the wavefront reached this block?
            if (nearestDistSq <= activeRadiusSq) {
                // Distance fraction for palette offset
                float distFraction = (float) (Math.sqrt(nearestDistSq) / maxDist);

                // Combine distance offset + time-based phase shift
                float palettePos = (distFraction + cyclePhase) % 1.0f;

                RGBColor color = ctx.palette().getColorAt(palettePos);
                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);
            }
            // Blocks not yet reached → leave unchanged (no revert spam)
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
