package me.steinborn.krypton.mod.shared;

import com.velocitypowered.natives.util.Natives;
import io.netty.util.ResourceLeakDetector;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KryptonSharedInitializer implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger(KryptonSharedInitializer.class);

    static {
        /**
         * By default, Netty allocates 16MiB arenas for the PooledByteBufAllocator. This is too much
         * memory for Minecraft, which imposes a maximum packet size of 2MiB! We'll use 4MiB as a more
         * sane default.
         *
         * Note: io.netty.allocator.pageSize << io.netty.allocator.maxOrder is the formula used to
         * compute the chunk size. We lower maxOrder from its default of 11 to 9. (We also use a null
         * check, so that the user is free to choose another setting if need be.)
         */
        if (System.getProperty("io.netty.allocator.maxOrder") == null) {
            System.setProperty("io.netty.allocator.maxOrder", "9");
        }

        /**
         * Disable the resource leak detector by default as it reduces performance. Allow the user to
         * override this if desired.
         */
        if (System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Compression will use " + Natives.compress.getLoadedVariant() + ", encryption will use " + Natives.cipher.getLoadedVariant());
    }
}
