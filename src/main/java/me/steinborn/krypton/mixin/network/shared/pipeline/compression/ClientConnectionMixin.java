package me.steinborn.krypton.mixin.network.shared.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.mod.network.compression.MinecraftCompressDecoder;
import me.steinborn.krypton.mod.network.compression.MinecraftCompressEncoder;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Shadow private Channel channel;

    @Overwrite
    public void setCompressionThreshold(int compressionThreshold) {
        if (compressionThreshold >= 0) {
            VelocityCompressor compressor = Natives.compress.get().create(6);
            MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
            MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(compressionThreshold, compressor);

            channel.pipeline().addBefore("decoder", "decompress", decoder);
            channel.pipeline().addBefore("encoder", "compress", encoder);
        } else {
            this.channel.pipeline().remove("decompress");
            this.channel.pipeline().remove("compress");
        }
    }
}
