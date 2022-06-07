package me.steinborn.krypton.mixin.shared.network.pipeline.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import me.steinborn.krypton.mod.shared.network.compression.MinecraftCompressDecoder;
import me.steinborn.krypton.mod.shared.network.compression.MinecraftCompressEncoder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketDeflater;
import net.minecraft.network.PacketInflater;
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
    public void setCompressionThreshold(int compressionThreshold, boolean validate, CallbackInfo ci) {
        if (compressionThreshold == -1) {
            if (this.channel.pipeline().get("decompress") instanceof PacketInflater) {
                this.channel.pipeline().remove("decompress");
            }
            if (this.channel.pipeline().get("compress") instanceof PacketDeflater) {
                this.channel.pipeline().remove("compress");
            }
        } else {
            MinecraftCompressDecoder decoder = (MinecraftCompressDecoder) channel.pipeline()
                    .get("decompress");
            MinecraftCompressEncoder encoder = (MinecraftCompressEncoder) channel.pipeline()
                    .get("compress");
            if (decoder != null && encoder != null) {
                decoder.setThreshold(compressionThreshold);
                encoder.setThreshold(compressionThreshold);
            } else {
                VelocityCompressor compressor = Natives.compress.get().create(4);

                encoder = new MinecraftCompressEncoder(compressionThreshold, compressor);
                decoder = new MinecraftCompressDecoder(compressionThreshold, validate, compressor);

                channel.pipeline().addBefore("decoder", "decompress", decoder);
                channel.pipeline().addBefore("encoder", "compress", encoder);
            }
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
