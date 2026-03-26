package dev.blockanimation;

import dev.blockanimation.animation.AnimationEngine;
import dev.blockanimation.color.BlockColorRegistry;
import dev.blockanimation.command.BlockAnimationCommand;
import space.arim.morepaperlib.MorePaperLib;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BlockAnimation.
 * <p>
 * When used as a standalone plugin, this bootstraps the API, registers the
 * test command, and manages the lifecycle. When used as a shaded API,
 * external plugins create their own {@link BlockAnimationAPI} directly.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BlockAnimationPlugin extends JavaPlugin {

    private static BlockAnimationPlugin instance;

    private MorePaperLib morePaperLib;
    private AnimationEngine animationEngine;
    private BlockAnimationAPI api;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize MorePaperLib for Folia-safe scheduling
        morePaperLib = new MorePaperLib(this);

        // Build block color registry (static lookup table)
        BlockColorRegistry.init();

        // Create animation engine
        animationEngine = new AnimationEngine(this, morePaperLib);

        // Create public API
        api = new BlockAnimationAPI(this, morePaperLib, animationEngine);

        // Register Brigadier test command (Paper API)
        registerCommand();

        getLogger().info("BlockAnimation enabled — " + BlockColorRegistry.getAll().size() + " block colors registered.");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            api.shutdown();
        }
        instance = null;
    }

    private void registerCommand() {
        try {
            BlockAnimationCommand cmd = new BlockAnimationCommand(api);
            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                commands.registrar().register(cmd.buildCommand(),
                        "Test command for BlockAnimation. Usage: /blockanimation <spread|passthrough|sparkle|stop>");
            });
        } catch (Exception e) {
            getLogger().warning("Could not register Brigadier command (Paper API required): " + e.getMessage());
            getLogger().warning("The API is still fully functional — command registration is optional.");
        }
    }

    /** @return the singleton plugin instance */
    public static BlockAnimationPlugin getInstance() {
        return instance;
    }

    /** @return the public API */
    public BlockAnimationAPI getApi() {
        return api;
    }

}
