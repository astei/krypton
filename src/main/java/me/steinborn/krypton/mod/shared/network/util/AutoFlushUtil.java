package me.steinborn.krypton.mod.shared.network.util;

import me.steinborn.krypton.mixin.shared.network.util.ServerPlayNetworkHandlerAccessor;
import me.steinborn.krypton.mod.shared.network.ConfigurableAutoFlush;
import net.minecraft.server.network.ServerPlayerEntity;

public class AutoFlushUtil {
    public static void setAutoFlush(ServerPlayerEntity player, boolean val) {
        if (player.getClass() == ServerPlayerEntity.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) ((ServerPlayNetworkHandlerAccessor) player.networkHandler).getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    private AutoFlushUtil() {}
}
