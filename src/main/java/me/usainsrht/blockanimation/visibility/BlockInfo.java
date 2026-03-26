package me.usainsrht.blockanimation.visibility;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * Lightweight snapshot of a single block's position and state.
 * <p>
 * Intentionally avoids holding a reference to a live Bukkit {@link org.bukkit.block.Block}
 * so it can be safely used off the main thread and serialized to disk.
 *
 * @param x world X coordinate
 * @param y world Y coordinate
 * @param z world Z coordinate
 * @param material the block material
 * @param blockDataString serialized form of BlockData (e.g. {@code "minecraft:oak_stairs[facing=east,half=bottom]"})
 */
public record BlockInfo(int x, int y, int z, Material material, String blockDataString) {

    /**
     * Parse the stored {@code blockDataString} back to a live {@link BlockData}.
     * <p>
     * <b>Must be called on a thread that can access the Bukkit API.</b>
     */
    public BlockData toBlockData() {
        return org.bukkit.Bukkit.createBlockData(blockDataString);
    }

    /**
     * Create a {@link BlockInfo} from a live block.
     */
    public static BlockInfo of(int x, int y, int z, Material material, BlockData data) {
        return new BlockInfo(x, y, z, material, data.getAsString(true));
    }

    /**
     * Squared distance from this block's center to the given world coordinates.
     */
    public double distanceSquared(double cx, double cy, double cz) {
        double dx = x + 0.5 - cx;
        double dy = y + 0.5 - cy;
        double dz = z + 0.5 - cz;
        return dx * dx + dy * dy + dz * dz;
    }
}
