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
 * <b>Pass-through (Directional)</b> animation — a color wave sweeps across the
 * visible blocks along a specific yaw angle, like a curtain of light.
 * <p>
 * Each block's position is projected onto the sweep axis. The "plane" of active
 * color moves along this axis over time, coloring blocks whose projection
 * falls within the sweep band.
 *
 * <h3>Looping</h3>
 * The sweep loops once it reaches the far end of the block set.
 */
public final class PassThroughAnimation implements Animation {

    /** Sweep direction yaw in degrees (0 = south, 90 = west, etc.) */
    private final float yawDegrees;

    /** Duration of one full sweep in ticks */
    private final long sweepDurationTicks;

    /** Width of the "color band" as a fraction of total extent (0..1) */
    private final float bandWidth;

    /**
     * @param yawDegrees        sweep direction in degrees
     * @param sweepDurationTicks ticks for one full directional pass
     * @param bandWidth         fraction (0..1) of the total extent covered by the color band
     */
    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks, float bandWidth) {
        this.yawDegrees = yawDegrees;
        this.sweepDurationTicks = Math.max(1, sweepDurationTicks);
        this.bandWidth = Math.max(0.05f, Math.min(1.0f, bandWidth));
    }

    /** Default: sweep south, 2 seconds, 30% band. */
    public PassThroughAnimation() {
        this(0f, 40, 0.3f);
    }

    @Override
    public String getName() {
        return "pass_through";
    }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        List<BlockInfo> blocks = ctx.visibleBlocks().getBlocks();
        if (blocks.isEmpty()) return;

        // Compute sweep axis from yaw (horizontal direction vector)
        double yawRad = Math.toRadians(yawDegrees);
        double axisX = -Math.sin(yawRad);
        double axisZ = Math.cos(yawRad);

        double cx = ctx.visibleBlocks().getCenterX();
        double cz = ctx.visibleBlocks().getCenterZ();

        // Project all blocks onto the sweep axis and find min/max
        double minProj = Double.MAX_VALUE;
        double maxProj = -Double.MAX_VALUE;
        double[] projections = new double[blocks.size()];

        for (int i = 0; i < blocks.size(); i++) {
            BlockInfo b = blocks.get(i);
            double dx = (b.x() + 0.5) - cx;
            double dz = (b.z() + 0.5) - cz;
            double proj = dx * axisX + dz * axisZ;
            projections[i] = proj;
            if (proj < minProj) minProj = proj;
            if (proj > maxProj) maxProj = proj;
        }

        double totalExtent = maxProj - minProj;
        if (totalExtent <= 0) totalExtent = 1;

        // Current sweep position (0..1 looping)
        float sweepPos = (float) ((elapsedTicks % sweepDurationTicks) / (double) sweepDurationTicks);

        // Band boundaries (allow the band to sweep from before to after the blocks)
        float bandStart = sweepPos - bandWidth;
        float bandEnd = sweepPos;

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            // Normalize projection to [0, 1]
            float normalizedProj = (float) ((projections[i] - minProj) / totalExtent);

            // Check if block falls within the color band
            if (normalizedProj >= bandStart && normalizedProj <= bandEnd) {
                // Position within the band → palette position
                float bandPos = (normalizedProj - bandStart) / bandWidth;
                RGBColor color = ctx.palette().getColorAt(bandPos);

                BlockInfo block = blocks.get(i);
                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);
            } else {
                // Outside the band — revert to original
                BlockInfo block = blocks.get(i);
                changes.put(block, block.toBlockData());
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
