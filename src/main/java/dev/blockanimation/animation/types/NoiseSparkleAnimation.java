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
import java.util.concurrent.ThreadLocalRandom;

/**
 * <b>Noise / Sparkle</b> animation — random blocks shimmer through the palette
 * on each tick, creating a sparkling or glimmering effect.
 * <p>
 * On each tick, a random subset of the visible blocks is selected and colored
 * at a random position in the palette. Blocks not selected in the current tick
 * are reverted to their original appearance, giving a twinkling effect.
 * <p>
 * The density of the sparkle can be configured. LoopMode is not directly
 * applicable but the palette base shifts over time for visual variety.
 */
public final class NoiseSparkleAnimation implements Animation {

    /** Fraction of blocks that "sparkle" each tick (0..1). */
    private final float density;

    /** Ticks for one full palette base shift cycle. */
    private final long cycleDurationTicks;

    /**
     * @param density          fraction of blocks to animate per tick (0.0–1.0)
     * @param cycleDurationTicks ticks for one full palette shift cycle
     */
    public NoiseSparkleAnimation(float density, long cycleDurationTicks) {
        this.density = Math.max(0.01f, Math.min(1.0f, density));
        this.cycleDurationTicks = Math.max(1, cycleDurationTicks);
    }

    /**
     * @param density fraction of blocks to animate per tick
     */
    public NoiseSparkleAnimation(float density) {
        this(density, 60);
    }

    /** Default: 15% density, 3s cycle. */
    public NoiseSparkleAnimation() {
        this(0.15f, 60);
    }

    @Override
    public String getName() {
        return "noise_sparkle";
    }

    @Override
    public void tick(AnimationContext ctx, long elapsedTicks) {
        List<BlockInfo> blocks = ctx.visibleBlocks().getBlocks();
        if (blocks.isEmpty()) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        ShapeMatchingReplacer replacer = ctx.replacer();
        Map<BlockInfo, BlockData> changes = new HashMap<>();

        // Palette base shift over time (adds visual variety)
        float baseShift = ctx.resolveProgress(elapsedTicks, cycleDurationTicks);

        for (BlockInfo block : blocks) {
            if (rng.nextFloat() < density) {
                // This block sparkles — pick a random palette color + base shift
                float palettePos = (rng.nextFloat() + baseShift) % 1.0f;
                RGBColor color = ctx.palette().getColorAt(palettePos);

                BlockData original = block.toBlockData();
                BlockData replacement = replacer.computeReplacement(original, color);
                changes.put(block, replacement);
            } else {
                // Not sparkling — show original
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
