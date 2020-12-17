package me.steinborn.krypton.mixin.network.shared.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import me.steinborn.krypton.mod.network.VarintByteDecoder;
import net.minecraft.network.SplitterHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

/**
 * Overrides the SplitterHandler to use optimized packet splitting from Velocity 1.1.0. In addition this applies a
 * security fix to stop "nullping" attacks.
 */
@Mixin(SplitterHandler.class)
public class SplitterHandlerMixin {
    private final VarintByteDecoder reader = new VarintByteDecoder();

    /**
     * @author Andrew Steinborn
     */
    @Overwrite
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!ctx.channel().isActive()) {
            in.clear();
            return;
        }

        reader.reset();

        int varintEnd = in.forEachByte(reader);
        if (varintEnd == -1) {
            // We tried to go beyond the end of the buffer. This is probably a good sign that the
            // buffer was too short to hold a proper varint.
            return;
        }

        if (reader.getResult() == VarintByteDecoder.DecodeResult.SUCCESS) {
            int readLen = reader.readVarint();
            if (readLen < 0) {
                throw new DecoderException("Bad packet length");
            } else if (readLen == 0) {
                // skip over the empty packet and ignore it
                in.readerIndex(varintEnd + 1);
            } else {
                int minimumRead = reader.varintBytes() + readLen;
                if (in.isReadable(minimumRead)) {
                    out.add(in.retainedSlice(varintEnd + 1, readLen));
                    in.skipBytes(minimumRead);
                }
            }
        } else if (reader.getResult() == VarintByteDecoder.DecodeResult.TOO_BIG) {
            throw new DecoderException("Varint too big");
        }
    }
}
