package me.steinborn.krypton.mod.client;

import me.steinborn.krypton.mod.Krypton;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class KryptonClient implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger(Krypton.class);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Krypton is now accelerating your Minecraft client's networking stack \uD83D\uDE80");
        LOGGER.info("Note that Krypton is most effective on servers, not the client.");
    }
}
