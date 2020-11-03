package me.steinborn.cardigan.mixin.network.pipeline;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketDeflater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.zip.Deflater;

/**
 * Replaces the deflate library used by Minecraft with Velocity natives.
 */
@Mixin(PacketDeflater.class)
public class PacketDeflaterMixin extends MessageToByteEncoder<ByteBuf> {
    private static final int COMPRESSION_LEVEL = 6;

    @Shadow private int compressionThreshold;
    @Final @Shadow private Deflater deflater;
    @Shadow private byte[] deflateBuffer;
    private VelocityCompressor compressor;

    {
        deflateBuffer = null;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onConstructed(int compressionThreshold, CallbackInfo ci) {
        this.compressor = Natives.compress.get().create(COMPRESSION_LEVEL);
        this.deflater.end();
    }

    /**
     * @author Andrew Steinborn
     */
    @Overwrite
    public void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        int packetBytes = in.readableBytes();
        PacketByteBuf packetByteBuf = new PacketByteBuf(out);
        if (packetBytes < this.compressionThreshold) {
            packetByteBuf.writeVarInt(0);
            packetByteBuf.writeBytes(in);
        } else {
            packetByteBuf.writeVarInt(packetBytes);
            compressor.deflate(in, out);
        }
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
        return ctx.alloc().directBuffer(msg.readableBytes() + 1);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.compressor.close();
    }
}
