package me.usainsrht.blockanimation.shape;

import me.usainsrht.blockanimation.color.BlockColorRegistry;
import me.usainsrht.blockanimation.color.RGBColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;

/**
 * Produces a replacement {@link BlockData} that visually matches a target
 * {@link RGBColor} while preserving the original block's <b>shape</b>.
 * <p>
 * Supported shape transfers:
 * <ul>
 *   <li>Stairs → facing, half, shape, waterlogged</li>
 *   <li>Slabs → type, waterlogged</li>
 *   <li>Fences → connected faces, waterlogged</li>
 *   <li>Walls → height per face, up, waterlogged</li>
 *   <li>Glass panes / iron bars → connected faces</li>
 *   <li>Doors → facing, half, hinge, open, powered</li>
 *   <li>Trapdoors → facing, half, open, powered, waterlogged</li>
 *   <li>Buttons → attached face, facing, powered</li>
 *   <li>Pressure plates → powered</li>
 *   <li>Carpets → no special data (just material swap)</li>
 *   <li>Directional / Orientable / Bisected / Waterlogged → generic fallbacks</li>
 * </ul>
 */
public final class ShapeMatchingReplacer {

    private final boolean replaceWithSame;

    public ShapeMatchingReplacer(boolean replaceWithSame) {
        this.replaceWithSame = replaceWithSame;
    }

    /**
     * Compute a replacement {@link BlockData} for the supplied original, tinted towards
     * the given target color while preserving shape properties.
     */
    public BlockData computeReplacement(BlockData original, RGBColor targetColor) {
        Material originalMat = original.getMaterial();
        ShapeGroup group = ShapeGroup.classify(originalMat);

        Material replacement;
        if (replaceWithSame && group.isAnimatable()) {
            replacement = BlockColorRegistry.findClosest(targetColor, group);
        } else {
            replacement = BlockColorRegistry.findClosest(targetColor);
        }

        if (replacement == null) replacement = originalMat;

        BlockData newData = Bukkit.createBlockData(replacement);
        transferProperties(original, newData);
        return newData;
    }

    // ------------------------------------------------------------------
    // Property transfer
    // ------------------------------------------------------------------

    private void transferProperties(BlockData source, BlockData target) {
        // === Stairs → facing, half, shape, waterlogged ===
        if (source instanceof Stairs srcStairs && target instanceof Stairs tgtStairs) {
            tgtStairs.setFacing(srcStairs.getFacing());
            tgtStairs.setHalf(srcStairs.getHalf());
            tgtStairs.setShape(srcStairs.getShape());
            transferWaterlogged(source, target);
            return;
        }

        // === Slabs → type, waterlogged ===
        if (source instanceof Slab srcSlab && target instanceof Slab tgtSlab) {
            tgtSlab.setType(srcSlab.getType());
            transferWaterlogged(source, target);
            return;
        }

        // === Walls → height per face, up, waterlogged ===
        if (source instanceof Wall srcWall && target instanceof Wall tgtWall) {
            for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                    org.bukkit.block.BlockFace.NORTH,
                    org.bukkit.block.BlockFace.EAST,
                    org.bukkit.block.BlockFace.SOUTH,
                    org.bukkit.block.BlockFace.WEST}) {
                tgtWall.setHeight(face, srcWall.getHeight(face));
            }
            tgtWall.setUp(srcWall.isUp());
            transferWaterlogged(source, target);
            return;
        }

        // === Doors → facing, half, hinge, open, powered ===
        if (source instanceof Door srcDoor && target instanceof Door tgtDoor) {
            tgtDoor.setFacing(srcDoor.getFacing());
            tgtDoor.setHalf(srcDoor.getHalf());
            tgtDoor.setHinge(srcDoor.getHinge());
            tgtDoor.setOpen(srcDoor.isOpen());
            if (source instanceof Powerable srcP && target instanceof Powerable tgtP) {
                tgtP.setPowered(srcP.isPowered());
            }
            return;
        }

        // === Trapdoors → facing, half, open, powered, waterlogged ===
        if (source instanceof TrapDoor srcTrap && target instanceof TrapDoor tgtTrap) {
            tgtTrap.setFacing(srcTrap.getFacing());
            tgtTrap.setHalf(srcTrap.getHalf());
            tgtTrap.setOpen(srcTrap.isOpen());
            if (source instanceof Powerable srcP && target instanceof Powerable tgtP) {
                tgtP.setPowered(srcP.isPowered());
            }
            transferWaterlogged(source, target);
            return;
        }

        // === Fences → connected faces, waterlogged ===
        if (source instanceof Fence srcFence && target instanceof Fence tgtFence) {
            for (var face : srcFence.getAllowedFaces()) {
                if (tgtFence.getAllowedFaces().contains(face)) {
                    tgtFence.setFace(face, srcFence.hasFace(face));
                }
            }
            transferWaterlogged(source, target);
            return;
        }

        // === Buttons (Switch) → attached face, facing, powered ===
        if (source instanceof FaceAttachable srcFa && target instanceof FaceAttachable tgtFa
                && source instanceof Switch && target instanceof Switch) {
            tgtFa.setAttachedFace(srcFa.getAttachedFace());
            if (source instanceof Directional srcDir && target instanceof Directional tgtDir) {
                if (tgtDir.getFaces().contains(srcDir.getFacing())) {
                    tgtDir.setFacing(srcDir.getFacing());
                }
            }
            if (source instanceof Powerable srcP && target instanceof Powerable tgtP) {
                tgtP.setPowered(srcP.isPowered());
            }
            return;
        }

        // === Pressure plates → powered ===
        if (source instanceof Powerable srcP && target instanceof Powerable tgtP
                && ShapeGroup.classify(source.getMaterial()) == ShapeGroup.PRESSURE_PLATE) {
            tgtP.setPowered(srcP.isPowered());
            return;
        }

        // === MultipleFacing (panes, iron bars) ===
        if (source instanceof MultipleFacing srcMf && target instanceof MultipleFacing tgtMf) {
            for (var face : srcMf.getAllowedFaces()) {
                if (tgtMf.getAllowedFaces().contains(face)) {
                    tgtMf.setFace(face, srcMf.hasFace(face));
                }
            }
        }

        // === Generic fallbacks (cascading, not returning) ===

        // Directional
        if (source instanceof Directional srcDir && target instanceof Directional tgtDir) {
            if (tgtDir.getFaces().contains(srcDir.getFacing())) {
                tgtDir.setFacing(srcDir.getFacing());
            }
        }

        // Orientable (logs, basalt, pillar axis)
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
        transferWaterlogged(source, target);
    }

    private void transferWaterlogged(BlockData source, BlockData target) {
        if (source instanceof Waterlogged srcWl && target instanceof Waterlogged tgtWl) {
            tgtWl.setWaterlogged(srcWl.isWaterlogged());
        }
    }
}
