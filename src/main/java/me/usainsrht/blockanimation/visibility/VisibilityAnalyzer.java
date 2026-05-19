package me.usainsrht.blockanimation.visibility;

import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Asynchronous radial surface-block scanner.
 * <p>
 * Given a center {@link Location} and a radius, this class performs a
 * <b>Breadth-First Search (BFS) flood-fill</b> outward from the center,
 * detecting all visible surface blocks in every direction—i.e., blocks that
 * face air or transparent space and are therefore "seen" from the center.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Start at the block containing the center location.</li>
 *   <li>If the starting block can be propagated through (air / transparent / passable),
 *       expand to its 6-connected neighbours (±X, ±Y, ±Z).</li>
 *   <li>When hitting an <b>opaque</b> block, mark it as a visible surface
 *       and <i>do not</i> continue past it—it's a wall.</li>
 *   <li>When hitting a <b>transparent</b> block (glass, slab, stair, etc.),
 *       mark it as visible <i>and</i> continue propagating through it.</li>
 *   <li>Passable/air blocks are neither recorded nor blocking; the BFS just
 *       passes through them.</li>
 *   <li>Stop expanding when the radius limit is reached.</li>
 * </ol>
 *
 * <h3>Threading</h3>
 * Chunk block reads are scheduled via {@link MorePaperLib} to ensure correct
 * region ownership on Folia.  The heavy BFS bookkeeping runs off-thread; only
 * the actual {@link Block#getType()} calls are dispatched to the owning region.
 * The result is delivered as a {@link CompletableFuture}.
 */
public final class VisibilityAnalyzer {

    /** 6-connected neighbour offsets */
    private static final int[][] OFFSETS = {
            { 1, 0, 0}, {-1, 0, 0},
            { 0, 1, 0}, { 0,-1, 0},
            { 0, 0, 1}, { 0, 0,-1}
    };

    private final JavaPlugin plugin;
    private final MorePaperLib morePaperLib;

    public VisibilityAnalyzer(JavaPlugin plugin, MorePaperLib morePaperLib) {
        this.plugin = plugin;
        this.morePaperLib = morePaperLib;
    }

    /**
     * Asynchronously compute all visible surface blocks around the given center.
     *
     * @param center the scan origin (block coordinates are derived from this)
     * @param radius maximum scan radius in blocks (clamped to [1, 64])
     * @return a future that completes with the immutable {@link VisibleBlocks} result
     */
    public CompletableFuture<VisibleBlocks> analyze(Location center, int radius) {
        Objects.requireNonNull(center, "center");
        Objects.requireNonNull(center.getWorld(), "center.getWorld()");

        int clampedRadius = Math.max(1, Math.min(64, radius));
        CompletableFuture<VisibleBlocks> future = new CompletableFuture<>();

        // Schedule on the region that owns the center location (Folia-safe)
        try {
            morePaperLib.scheduling().regionSpecificScheduler(center).run(() -> {
                try {
                    VisibleBlocks result = runBFS(center, clampedRadius);
                    future.complete(result);
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "Visibility analysis failed", t);
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }

        return future;
    }

    // ------------------------------------------------------------------
    // BFS core (runs on the region thread)
    // ------------------------------------------------------------------

    private VisibleBlocks runBFS(Location center, int radius) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int radiusSq = radius * radius;

        // Visited set encoded as packed longs for memory efficiency
        Set<Long> visited = new HashSet<>(radius * radius * 4);
        List<BlockInfo> visibleBlocks = new ArrayList<>();

        // BFS queue — each entry is (x, y, z) packed into a long
        Queue<long[]> queue = new ArrayDeque<>();
        long startKey = packCoord(cx, cy, cz);
        visited.add(startKey);
        queue.add(new long[]{cx, cy, cz});

        while (!queue.isEmpty()) {
            long[] pos = queue.poll();
            int bx = (int) pos[0];
            int by = (int) pos[1];
            int bz = (int) pos[2];

            Block block = world.getBlockAt(bx, by, bz);
            Material mat = block.getType();

            // Determine block category
            boolean opaque = TransparencyHelper.isOpaque(mat);
            boolean transparent = TransparencyHelper.isTransparent(mat);

            // Record visible surface blocks
            if (opaque || transparent) {
                visibleBlocks.add(BlockInfo.of(bx, by, bz, mat, block.getBlockData()));
            }

            // Opaque blocks stop the BFS — they are walls
            if (opaque) continue;

            // Transparent and passable blocks allow further propagation
            for (int[] offset : OFFSETS) {
                int nx = bx + offset[0];
                int ny = by + offset[1];
                int nz = bz + offset[2];

                // Radius check (squared distance from center)
                int dx = nx - cx;
                int dy = ny - cy;
                int dz = nz - cz;
                if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                // World height bounds check
                if (ny < world.getMinHeight() || ny > world.getMaxHeight()) continue;

                long key = packCoord(nx, ny, nz);
                if (visited.add(key)) {
                    queue.add(new long[]{nx, ny, nz});
                }
            }
        }

        plugin.getLogger().fine(() -> "Visibility scan at (" + cx + "," + cy + "," + cz +
                ") r=" + radius + " found " + visibleBlocks.size() + " visible blocks");

        return new VisibleBlocks(
                world.getName(),
                center.getX(), center.getY(), center.getZ(),
                radius, visibleBlocks
        );
    }

    // ------------------------------------------------------------------
    // Coordinate packing
    // ------------------------------------------------------------------

    /**
     * Pack three ints into a single long for use as a HashMap key.
     * Supports block coordinates in the range [-2^20, 2^20) = [-1048576, 1048575].
     */
    private static long packCoord(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF)) | ((long) (y & 0x1FFFFF) << 21) | ((long) (z & 0x1FFFFF) << 42);
    }
}
