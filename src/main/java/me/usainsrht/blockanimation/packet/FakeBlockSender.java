package me.usainsrht.blockanimation.packet;

import me.usainsrht.blockanimation.visibility.BlockInfo;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Utility for sending per-player <b>fake</b> block updates using the standard
 * Bukkit API ({@link Player#sendBlockChange}).
 * <p>
 * The real world data is never modified.  Blocks are grouped by chunk section
 * for efficient batching where the API supports it.
 *
 * <h3>Thread safety</h3>
 * All send methods <b>must</b> be called from a thread that may interact with
 * the player (i.e., the player's region thread on Folia, or the main thread on
 * Spigot/Paper).
 */
public final class FakeBlockSender {

    private FakeBlockSender() {}

    /**
     * Send a single fake block change to a player.
     *
     * @param player   target player
     * @param info     block position
     * @param newData  the fake BlockData to display
     */
    public static void send(Player player, BlockInfo info, BlockData newData) {
        if (!player.isOnline()) return;
        player.sendBlockChange(new Location(player.getWorld(), info.x(), info.y(), info.z()), newData);
    }

    /**
     * Send multiple fake block changes to a player, grouped for efficiency.
     *
     * @param player  target player
     * @param changes map of BlockInfo → replacement BlockData
     */
    public static void sendBatch(Player player, Map<BlockInfo, BlockData> changes) {
        if (!player.isOnline() || changes.isEmpty()) return;

        // Use individual sendBlockChange calls — works on all server implementations.
        // Paper's sendMultiBlockChange is unstable across versions; individual calls
        // are reliable and still very performant for typical room-sized batches.
        World world = player.getWorld();
        for (Map.Entry<BlockInfo, BlockData> entry : changes.entrySet()) {
            BlockInfo info = entry.getKey();
            player.sendBlockChange(
                    new Location(world, info.x(), info.y(), info.z()),
                    entry.getValue()
            );
        }
    }

    /**
     * Revert a set of blocks back to their original appearance for a player.
     * <p>
     * Reads the original {@link BlockData} from the stored {@link BlockInfo#blockDataString()}.
     *
     * @param player target player
     * @param blocks blocks to revert
     */
    public static void revert(Player player, Collection<BlockInfo> blocks) {
        if (!player.isOnline() || blocks.isEmpty()) return;

        World world = player.getWorld();
        for (BlockInfo info : blocks) {
            BlockData original = info.toBlockData();
            player.sendBlockChange(
                    new Location(world, info.x(), info.y(), info.z()),
                    original
            );
        }
    }
}
