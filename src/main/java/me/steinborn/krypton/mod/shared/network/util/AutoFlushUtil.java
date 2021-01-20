package me.steinborn.krypton.mod.shared.network.util;

import me.steinborn.krypton.mod.shared.network.ConfigurableAutoFlush;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayerEntity;

public class AutoFlushUtil {
    public static void setAutoFlush(ServerPlayerEntity player, boolean val) {
        if (player.getClass() == ServerPlayerEntity.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) player.networkHandler.getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    public static void setAutoFlush(ClientConnection conn, boolean val) {
        if (conn.getClass() == ClientConnection.class) {
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) conn);
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    private AutoFlushUtil() {}
}
