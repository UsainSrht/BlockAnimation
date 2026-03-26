package me.usainsrht.blockanimation.animation.types;

import me.usainsrht.blockanimation.animation.Animation;
import me.usainsrht.blockanimation.animation.AnimationContext;
import me.usainsrht.blockanimation.color.RGBColor;
import me.usainsrht.blockanimation.packet.FakeBlockSender;
import me.usainsrht.blockanimation.shape.ShapeMatchingReplacer;
import me.usainsrht.blockanimation.visibility.BlockInfo;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * <b>Spread (Radial Wave)</b> animation — a color wave emanates outward
 * from one or more centers like ripples in water.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>A wavefront expands outward from the center(s) over {@code spreadDurationTicks}.</li>
 *   <li>Each block is "reached" when the wavefront passes its distance.</li>
 *   <li>Once reached, the block starts cycling through the palette from the beginning.</li>
 *   <li>Center blocks were reached first, so they're further along in the palette.
 *       Edge blocks were reached later, so they're earlier in the palette.</li>
 *   <li>This creates a true propagating wave: R appears at center, expands outward
 *       as a ring, then G follows, then B follows, etc.</li>
 * </ol>
 *
 * <h3>Multi-center</h3>
 * Supports multiple spread origins. Each block uses the distance to the
 * nearest center for its wavefront timing.
 */
public final class SpreadAnimation implements Animation {

    /** Ticks for the wavefront to reach the maximum radius. */
    private final long spreadDurationTicks;

    /** Ticks for one full palette cycle on a single block. */
    private final long cycleDurationTicks;

    /** Custom spread centers (nullable → uses VisibleBlocks center). */
    private final double[][] centers;

    /**
     * Full constructor.
     *
     * @param spreadDurationTicks ticks for wavefront to reach max radius (e.g., 40 = 2s)
     * @param cycleDurationTicks  ticks for one full color cycle per block (e.g., 60 = 3s)
     * @param centers             spread origins (may be null/empty for default center)
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
            this.centers = null;
        }
    }

    public SpreadAnimation(long spreadDurationTicks, long cycleDurationTicks) {
        this(spreadDurationTicks, cycleDurationTicks, null);
    }

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

        // Current wavefront radius
        double activeRadius = ((double) elapsedTicks / spreadDurationTicks) * maxDist;
        double activeRadiusSq = activeRadius * activeRadius;

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (BlockInfo block : blocks) {
            // Distance to nearest center
            double nearestDist = Double.MAX_VALUE;
            for (double[] center : activeCenters) {
                double dx = (block.x() + 0.5) - center[0];
                double dy = (block.y() + 0.5) - center[1];
                double dz = (block.z() + 0.5) - center[2];
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d < nearestDist) nearestDist = d;
            }

            // Has the wavefront reached this block?
            if (nearestDist * nearestDist <= activeRadiusSq) {
                // When was this block first reached?
                // t_reach = (nearestDist / maxDist) * spreadDurationTicks
                long ticksReached = (long) ((nearestDist / maxDist) * spreadDurationTicks);

                // How long has this block been "colored"?
                long ticksSinceReached = elapsedTicks - ticksReached;

                // Palette position = how far this block has progressed through the cycle
                // Center blocks have large ticksSinceReached → further in palette
                // Edge blocks have small ticksSinceReached → earlier in palette
                float palettePos = ctx.resolveProgress(ticksSinceReached, cycleDurationTicks);

                RGBColor color = ctx.palette().getColorAt(palettePos);
                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);
            }
            // Blocks not yet reached → leave unchanged
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
