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
 * <b>Pass-through (Directional Wave)</b> animation — a color wave sweeps across
 * the visible blocks along a specific yaw angle, like a curtain of light.
 *
 * <h3>Continuous mode (default)</h3>
 * Blocks behind the sweep front keep their color and continue cycling.
 * Each block's palette position = how long ago the sweep front passed it.
 * This creates a true directional wave: the first color appears at the leading
 * edge and older colors trail behind, cycling through the palette.
 *
 * <h3>Band-only mode</h3>
 * When {@code continuous = false}, only the band is colored; blocks outside revert.
 */
public final class PassThroughAnimation implements Animation {

    /** Sweep direction yaw in degrees (0 = south, 90 = west). */
    private final float yawDegrees;

    /** Ticks for one full directional pass. */
    private final long sweepDurationTicks;

    /** Ticks for one full palette cycle on a colored block. */
    private final long cycleDurationTicks;

    /** Width of the color band as a fraction of total extent (0..1). */
    private final float bandWidth;

    /** When true, blocks retain color after the band passes. */
    private final boolean continuous;

    /**
     * Full constructor.
     *
     * @param yawDegrees         sweep direction in degrees
     * @param sweepDurationTicks ticks for one directional pass
     * @param cycleDurationTicks ticks for one full palette cycle per block
     * @param bandWidth          fraction (0..1) of extent covered by the band
     * @param continuous         if true, blocks keep color after the band passes
     */
    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks,
                                long cycleDurationTicks, float bandWidth,
                                boolean continuous) {
        this.yawDegrees = yawDegrees;
        this.sweepDurationTicks = Math.max(1, sweepDurationTicks);
        this.cycleDurationTicks = Math.max(1, cycleDurationTicks);
        this.bandWidth = Math.max(0.05f, Math.min(1.0f, bandWidth));
        this.continuous = continuous;
    }

    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks,
                                float bandWidth, boolean continuous) {
        this(yawDegrees, sweepDurationTicks, 60, bandWidth, continuous);
    }

    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks, float bandWidth) {
        this(yawDegrees, sweepDurationTicks, 60, bandWidth, true);
    }

    /** Default: sweep south, 2s, 3s cycle, 30% band, continuous. */
    public PassThroughAnimation() {
        this(0f, 40, 60, 0.3f, true);
    }

    @Override
    public String getName() {
        return "pass_through";
    }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        List<BlockInfo> blocks = ctx.visibleBlocks().getBlocks();
        if (blocks.isEmpty()) return;

        // Compute sweep axis from yaw
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

        // Sweep front position using loop mode (0..1)
        float sweepPos = ctx.resolveProgress(elapsedTicks, sweepDurationTicks);

        // Band boundaries
        float bandLeading = sweepPos;
        float bandTrailing = sweepPos - bandWidth;

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            float normalizedProj = (float) ((projections[i] - minProj) / totalExtent);

            boolean inBand = normalizedProj >= bandTrailing && normalizedProj <= bandLeading;
            boolean behindBand = normalizedProj < bandTrailing;

            if (inBand) {
                // In the active band — color based on position within band
                float bandPos = (normalizedProj - bandTrailing) / bandWidth;
                RGBColor color = ctx.palette().getColorAt(bandPos);

                BlockInfo block = blocks.get(i);
                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);

            } else if (continuous && behindBand) {
                // Behind the band: this block was first reached when the sweep
                // front passed it. Compute time since reached.
                // The sweep front reached normalizedProj at tick:
                //   t_reach = normalizedProj * sweepDurationTicks  (for RESTART/first pass)
                // For loop modes, we use a simplified approach: the distance
                // from the current sweep front to this block = how long ago it was passed.
                float distBehindFront = bandTrailing - normalizedProj;
                long ticksSinceReached = (long) (distBehindFront * sweepDurationTicks);

                float palettePos = ctx.resolveProgress(ticksSinceReached, cycleDurationTicks);
                RGBColor color = ctx.palette().getColorAt(palettePos);

                BlockInfo block = blocks.get(i);
                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);

            } else if (!continuous) {
                // Not in band and not continuous — revert to original
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
