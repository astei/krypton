package me.steinborn.krypton.mixin.shared.network.pipeline.prepender;

import io.netty.channel.ChannelOutboundHandler;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftVarintPrepender;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.handler.LocalBufPacker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    /**
     * @author Andrew Steinborn
     * @reason replace Mojang prepender with a more efficient one
     */
    @Overwrite
    private static ChannelOutboundHandler getPrepender(boolean local) {
        if (local) {
            return new LocalBufPacker();
        } else {
            return MinecraftVarintPrepender.INSTANCE;
        }
    }
}
