package dev.blockanimation.shape;

import dev.blockanimation.color.BlockColorRegistry;
import dev.blockanimation.color.RGBColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;

/**
 * Produces a replacement {@link BlockData} that visually matches a target
 * {@link RGBColor} while preserving the original block's <b>shape</b>.
 * <p>
 * When {@code replaceWithSame} is enabled (the default):
 * <ul>
 *   <li>Stairs → another Stair material, keeping facing/half/shape/waterlogged.</li>
 *   <li>Slabs → another Slab material, keeping type (top/bottom/double)/waterlogged.</li>
 *   <li>Glass panes → another Pane, keeping connected faces.</li>
 *   <li>Fences → another Fence, keeping connected faces/waterlogged.</li>
 *   <li>Full blocks → any full block of matching color.</li>
 * </ul>
 */
public final class ShapeMatchingReplacer {

    private final boolean replaceWithSame;

    /**
     * @param replaceWithSame {@code true} to enforce shape-group matching (recommended)
     */
    public ShapeMatchingReplacer(boolean replaceWithSame) {
        this.replaceWithSame = replaceWithSame;
    }

    /**
     * Compute a replacement {@link BlockData} for the supplied original, tinted towards
     * the given target color while preserving shape properties.
     *
     * @param original the real block's data (shape source)
     * @param targetColor the desired display color
     * @return a new {@link BlockData} ready to be sent via packets
     */
    public BlockData computeReplacement(BlockData original, RGBColor targetColor) {
        Material originalMat = original.getMaterial();
        ShapeGroup group = ShapeGroup.classify(originalMat);

        // Choose the closest material within the same shape group (or globally)
        Material replacement;
        if (replaceWithSame && group.isAnimatable()) {
            replacement = BlockColorRegistry.findClosest(targetColor, group);
        } else {
            replacement = BlockColorRegistry.findClosest(targetColor);
        }

        if (replacement == null) replacement = originalMat; // fallback

        // Build the new BlockData and transfer shape properties from the original
        BlockData newData = Bukkit.createBlockData(replacement);
        transferProperties(original, newData);
        return newData;
    }

    // ------------------------------------------------------------------
    // Property transfer helpers
    // ------------------------------------------------------------------

    /**
     * Copies directional, orientable, bisected, waterlogged, and other
     * shape-critical properties from {@code source} to {@code target}
     * where both blocks share the same data interface.
     */
    private void transferProperties(BlockData source, BlockData target) {
        // Stairs ─ facing, half, shape, waterlogged
        if (source instanceof Stairs srcStairs && target instanceof Stairs tgtStairs) {
            tgtStairs.setFacing(srcStairs.getFacing());
            tgtStairs.setHalf(srcStairs.getHalf());
            tgtStairs.setShape(srcStairs.getShape());
            if (source instanceof Waterlogged srcWl && target instanceof Waterlogged tgtWl) {
                tgtWl.setWaterlogged(srcWl.isWaterlogged());
            }
            return;
        }

        // Slabs ─ type, waterlogged
        if (source instanceof Slab srcSlab && target instanceof Slab tgtSlab) {
            tgtSlab.setType(srcSlab.getType());
            if (source instanceof Waterlogged srcWl && target instanceof Waterlogged tgtWl) {
                tgtWl.setWaterlogged(srcWl.isWaterlogged());
            }
            return;
        }

        // Fences ─ connected faces, waterlogged
        if (source instanceof Fence srcFence && target instanceof Fence tgtFence) {
            for (var face : srcFence.getAllowedFaces()) {
                if (tgtFence.getAllowedFaces().contains(face)) {
                    tgtFence.setFace(face, srcFence.hasFace(face));
                }
            }
            if (source instanceof Waterlogged srcWl && target instanceof Waterlogged tgtWl) {
                tgtWl.setWaterlogged(srcWl.isWaterlogged());
            }
            return;
        }

        // MultipleFacing (panes, iron bars)
        if (source instanceof MultipleFacing srcMf && target instanceof MultipleFacing tgtMf) {
            for (var face : srcMf.getAllowedFaces()) {
                if (tgtMf.getAllowedFaces().contains(face)) {
                    tgtMf.setFace(face, srcMf.hasFace(face));
                }
            }
        }

        // Directional (general fallback for facing)
        if (source instanceof Directional srcDir && target instanceof Directional tgtDir) {
            if (tgtDir.getFaces().contains(srcDir.getFacing())) {
                tgtDir.setFacing(srcDir.getFacing());
            }
        }

        // Orientable (logs, basalt pillar axis)
        if (source instanceof Orientable srcO && target instanceof Orientable tgtO) {
            if (tgtO.getAxes().contains(srcO.getAxis())) {
                tgtO.setAxis(srcO.getAxis());
            }
        }

        // Bisected (half for generic bisected blocks)
        if (source instanceof Bisected srcB && target instanceof Bisected tgtB) {
            tgtB.setHalf(srcB.getHalf());
        }

        // Waterlogged (catch-all)
        if (source instanceof Waterlogged srcWl && target instanceof Waterlogged tgtWl) {
            tgtWl.setWaterlogged(srcWl.isWaterlogged());
        }
    }
}
