package me.steinborn.krypton.mixin.network.shared.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.mod.shared.network.compression.MinecraftCompressDecoder;
import me.steinborn.krypton.mod.shared.network.compression.MinecraftCompressEncoder;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    private static Constructor<?> krypton_viaEventConstructor;

    static {
        krypton_findViaEvent();
    }

    @Shadow
    private Channel channel;

    private static void krypton_findViaEvent() {
        // ViaFabric compatibility
        try {
            krypton_viaEventConstructor =
                    Class.forName("com.viaversion.fabric.common.handler.PipelineReorderEvent").getConstructor();
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
    }

    @Inject(method = "setCompressionThreshold", at = @At("HEAD"), cancellable = true)
    public void setCompressionThreshold(int compressionThreshold, CallbackInfo ci) {
        if (compressionThreshold >= 0) {
            VelocityCompressor compressor = Natives.compress.get().create(4);
            MinecraftCompressEncoder encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
            MinecraftCompressDecoder decoder = new MinecraftCompressDecoder(compressionThreshold, compressor);

            channel.pipeline().addBefore("decoder", "decompress", decoder);
            channel.pipeline().addBefore("encoder", "compress", encoder);
        } else {
            this.channel.pipeline().remove("decompress");
            this.channel.pipeline().remove("compress");
        }

        this.handleViaCompression();

        ci.cancel();
    }

    private void handleViaCompression() {
        if (krypton_viaEventConstructor == null) return;
        try {
            this.channel.pipeline().fireUserEventTriggered(krypton_viaEventConstructor.newInstance());
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
