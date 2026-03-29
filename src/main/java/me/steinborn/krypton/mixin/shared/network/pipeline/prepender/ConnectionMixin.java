package me.steinborn.krypton.mixin.shared.network.pipeline.prepender;

import io.netty.channel.ChannelOutboundHandler;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftVarintPrepender;
import net.minecraft.network.Connection;
import net.minecraft.network.LocalFrameEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Connection.class)
public class ConnectionMixin {
    /**
     * @author Andrew Steinborn
     * @reason replace Mojang prepender with a more efficient one
     */
    @Overwrite
    private static ChannelOutboundHandler createFrameEncoder(boolean local) {
        if (local) {
            return new LocalFrameEncoder();
        } else {
            return MinecraftVarintPrepender.INSTANCE;
        }
    }
}
