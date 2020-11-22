package me.steinborn.krypton.mod;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Krypton implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LogManager.getLogger(Krypton.class);

    @Override
    public void onInitializeServer() {
        LOGGER.info("Krypton is now accelerating your Minecraft server's networking stack \uD83D\uDE80");
    }
}
