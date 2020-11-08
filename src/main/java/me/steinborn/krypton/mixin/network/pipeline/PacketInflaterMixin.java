package me.steinborn.krypton.mixin.network.pipeline;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketInflater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.zip.Inflater;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;

@Mixin(PacketInflater.class)
public class PacketInflaterMixin {
    @Shadow private int compressionThreshold;
    @Shadow @Final private Inflater inflater;
    private VelocityCompressor compressor;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onConstructed(int compressionThreshold, CallbackInfo ci) {
        this.compressor = Natives.compress.get().create(-1);
        this.inflater.end();
    }

    /**
     * @reason Use libdeflate bindings.
     * @author Andrew Steinborn
     */
    @Overwrite
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() != 0) {
            PacketByteBuf packetByteBuf = new PacketByteBuf(in);
            int claimedUncompressedSize = packetByteBuf.readVarInt();
            if (claimedUncompressedSize == 0) {
                out.add(packetByteBuf.readBytes(packetByteBuf.readableBytes()));
            } else {
                 if (claimedUncompressedSize < this.compressionThreshold) {
                     throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is below server threshold of " + this.compressionThreshold);
                 }

                 if (claimedUncompressedSize > 2097152) {
                     throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than protocol maximum of " + 2097152);
                 }

                ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
                ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
                try {
                    compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
                    out.add(uncompressed);
                } catch (Exception e) {
                    uncompressed.release();
                    throw e;
                } finally {
                    compatibleIn.release();
                }
            }

        }
    }
}
