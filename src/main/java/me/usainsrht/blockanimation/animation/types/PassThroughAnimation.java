package me.usainsrht.blockanimation.animation.types;

import me.usainsrht.blockanimation.animation.Animation;
import me.usainsrht.blockanimation.animation.AnimationContext;
import me.usainsrht.blockanimation.animation.LoopMode;
import me.usainsrht.blockanimation.color.RGBColor;
import me.usainsrht.blockanimation.packet.FakeBlockSender;
import me.usainsrht.blockanimation.shape.ShapeMatchingReplacer;
import me.usainsrht.blockanimation.visibility.BlockInfo;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>Pass-through (Directional Wave)</b> animation.
 *
 * <h3>Styles (LoopMode × trail flag)</h3>
 * <ul>
 *   <li>{@code trail=false, RESTART} — band sweeps, blocks revert behind it, loops cleanly.</li>
 *   <li>{@code trail=true,  RESTART} — band sweeps, blocks behind keep cycling, loops forever.</li>
 *   <li>{@code trail=false, REVERSE} — band ping-pongs, blocks revert behind it.</li>
 *   <li>{@code trail=true,  REVERSE} — band ping-pongs, blocks behind keep cycling.</li>
 * </ul>
 *
 * <h3>Key behaviors</h3>
 * <ul>
 *   <li>The sweep range is extended by {@code bandWidth} so the <i>entire</i> band
 *       exits before the loop restarts — no cutoff.</li>
 *   <li>In trail mode, once the band has completed at least one full pass, all blocks
 *       are considered "touched" and stay colored permanently (cycling).</li>
 * </ul>
 */
public final class PassThroughAnimation implements Animation {

    private final float yawDegrees;
    private final long sweepDurationTicks;
    private final long cycleDurationTicks;
    private final float bandWidth;
    private final boolean trail;

    /**
     * @param yawDegrees         sweep direction (0 = south, 90 = west)
     * @param sweepDurationTicks ticks for one full pass
     * @param cycleDurationTicks ticks for one palette cycle on trailing blocks
     * @param bandWidth          fraction (0..1) of extent covered by the band
     * @param trail              if true, blocks keep cycling color after band passes
     */
    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks,
                                long cycleDurationTicks, float bandWidth,
                                boolean trail) {
        this.yawDegrees = yawDegrees;
        this.sweepDurationTicks = Math.max(1, sweepDurationTicks);
        this.cycleDurationTicks = Math.max(1, cycleDurationTicks);
        this.bandWidth = Math.max(0.05f, Math.min(1.0f, bandWidth));
        this.trail = trail;
    }

    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks,
                                float bandWidth, boolean trail) {
        this(yawDegrees, sweepDurationTicks, 60, bandWidth, trail);
    }

    public PassThroughAnimation(float yawDegrees, long sweepDurationTicks, float bandWidth) {
        this(yawDegrees, sweepDurationTicks, 60, bandWidth, false);
    }

    /** Default: south, 2s sweep, 3s cycle, 30% band, no trail. */
    public PassThroughAnimation() {
        this(0f, 40, 60, 0.3f, false);
    }

    @Override
    public String getName() {
        return "pass_through";
    }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        List<BlockInfo> blocks = ctx.visibleBlocks().getBlocks();
        if (blocks.isEmpty()) return;

        // --- Projection ---
        double yawRad = Math.toRadians(yawDegrees);
        double axisX = -Math.sin(yawRad);
        double axisZ = Math.cos(yawRad);
        double cx = ctx.visibleBlocks().getCenterX();
        double cz = ctx.visibleBlocks().getCenterZ();

        double minProj = Double.MAX_VALUE, maxProj = -Double.MAX_VALUE;
        double[] projections = new double[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BlockInfo b = blocks.get(i);
            projections[i] = ((b.x() + 0.5) - cx) * axisX + ((b.z() + 0.5) - cz) * axisZ;
            if (projections[i] < minProj) minProj = projections[i];
            if (projections[i] > maxProj) maxProj = projections[i];
        }
        double totalExtent = maxProj - minProj;
        if (totalExtent <= 0) totalExtent = 1;

        // --- Sweep position ---
        // Extended range: band fully enters AND exits before the loop resets.
        // Sweep maps resolvedProgress 0..1 → bandLeading 0..(1+bandWidth).
        // At progress=0 the band is fully off-screen left (trailing = -bandWidth).
        // At progress=1 the band is fully off-screen right (trailing = 1.0).
        float sweepRange = 1.0f + bandWidth;
        float resolvedProgress = ctx.resolveProgress(elapsedTicks, sweepDurationTicks);
        float bandLeading = resolvedProgress * sweepRange;
        float bandTrailing = bandLeading - bandWidth;

        // Direction (for REVERSE ping-pong)
        boolean movingForward = true;
        if (ctx.loopMode() == LoopMode.REVERSE) {
            long fullCycle = sweepDurationTicks * 2;
            movingForward = (elapsedTicks % fullCycle) < sweepDurationTicks;
        }

        // Has the band completed at least one full forward pass?
        // After one pass, every block position [0,1] has been swept past.
        boolean allBlocksTouched = ((float) elapsedTicks / sweepDurationTicks) >= 1.0f;

        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            float norm = (float) ((projections[i] - minProj) / totalExtent);
            BlockInfo block = blocks.get(i);

            boolean inBand = norm >= bandTrailing && norm <= bandLeading;

            if (inBand) {
                // ---- Inside the active band ----
                float bandPos = (norm - bandTrailing) / bandWidth;
                if (!movingForward) bandPos = 1.0f - bandPos; // flip gradient direction
                bandPos = Math.max(0f, Math.min(1f, bandPos));

                RGBColor color = ctx.palette().getColorAt(bandPos);
                changes.put(block, replacer.computeReplacement(block.toBlockData(), color));

            } else if (trail) {
                // ---- Trail mode ----
                boolean wasTouched;
                if (allBlocksTouched) {
                    // After one full forward pass, ALL blocks stay colored.
                    wasTouched = true;
                } else if (movingForward) {
                    wasTouched = norm < bandTrailing;
                } else {
                    wasTouched = norm > bandLeading;
                }

                if (wasTouched) {
                    float palettePos;
                    if (ctx.loopMode() == LoopMode.REVERSE) {
                        // REVERSE: color based on when the band MOST RECENTLY passed
                        // this block (could be forward or backward pass).
                        int halfCycle = (int) (elapsedTicks / sweepDurationTicks);
                        long ticksIntoHalf = elapsedTicks % sweepDurationTicks;
                        boolean halfForward = (halfCycle % 2) == 0;

                        // When does the band's trailing edge pass this block in each direction?
                        float fwdTouchFrac = (norm + bandWidth) / sweepRange;
                        float bwdTouchFrac = ((1.0f - norm) + bandWidth) / sweepRange;
                        float currentHalfTouchFrac = halfForward ? fwdTouchFrac : bwdTouchFrac;
                        float progressInHalf = (float) ticksIntoHalf / sweepDurationTicks;

                        long tickLastTouched;
                        if (progressInHalf >= currentHalfTouchFrac) {
                            // Band already passed this block in the current half-cycle
                            tickLastTouched = (long) (halfCycle * sweepDurationTicks
                                    + currentHalfTouchFrac * sweepDurationTicks);
                        } else if (halfCycle > 0) {
                            // Not yet reached in current half — use previous half-cycle
                            boolean prevForward = !halfForward;
                            float prevTouchFrac = prevForward ? fwdTouchFrac : bwdTouchFrac;
                            tickLastTouched = (long) ((halfCycle - 1) * sweepDurationTicks
                                    + prevTouchFrac * sweepDurationTicks);
                        } else {
                            tickLastTouched = 0;
                        }
                        long ticksSinceLastTouched = Math.max(0, elapsedTicks - tickLastTouched);
                        palettePos = ctx.resolveProgress(ticksSinceLastTouched, cycleDurationTicks);
                    } else {
                        // RESTART/ONCE: color based on when this block was first touched
                        long tickFirstTouched = (long) ((norm + bandWidth) / sweepRange * sweepDurationTicks);
                        long ticksSinceTouched = Math.max(0, elapsedTicks - tickFirstTouched);
                        palettePos = ctx.resolveProgress(ticksSinceTouched, cycleDurationTicks);
                    }
                    RGBColor color = ctx.palette().getColorAt(palettePos);
                    changes.put(block, replacer.computeReplacement(block.toBlockData(), color));
                } else {
                    // Not yet touched — keep original
                    changes.put(block, block.toBlockData());
                }

            } else {
                // ---- No trail, not in band — revert ----
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
