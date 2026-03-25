package dev.blockanimation;

import dev.blockanimation.animation.AnimationEngine;
import dev.blockanimation.color.BlockColorRegistry;
import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for the BlockAnimation plugin.
 * <p>
 * Initializes MorePaperLib for Folia-safe scheduling, bootstraps the
 * {@link BlockColorRegistry}, and exposes the public {@link BlockAnimationAPI}.
 */
public final class BlockAnimationPlugin extends JavaPlugin {

    private static BlockAnimationPlugin instance;
    private MorePaperLib morePaperLib;
    private BlockAnimationAPI api;

    @Override
    public void onEnable() {
        instance = this;
        morePaperLib = new MorePaperLib(this);

        // Warm up the color registry (loads the hardcoded palette)
        BlockColorRegistry.init();

        // Create animation engine and public API
        AnimationEngine animationEngine = new AnimationEngine(this, morePaperLib);
        api = new BlockAnimationAPI(this, morePaperLib, animationEngine);

        getLogger().info("BlockAnimation enabled – Folia-safe scheduling active.");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            api.shutdown();
        }
        getLogger().info("BlockAnimation disabled.");
        instance = null;
    }

    /**
     * @return the singleton plugin instance
     */
    public static BlockAnimationPlugin getInstance() {
        return instance;
    }

    /**
     * @return the public API for external plugin use
     */
    public BlockAnimationAPI getApi() {
        return api;
    }

    /**
     * @return the MorePaperLib instance for scheduling
     */
    public MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }
}
