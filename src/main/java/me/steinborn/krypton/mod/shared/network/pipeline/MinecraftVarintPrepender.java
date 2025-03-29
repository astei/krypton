package me.steinborn.krypton.mod.shared.network.pipeline;

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.minecraft.network.encoding.VarInts;

import java.util.List;

@ChannelHandler.Sharable
public class MinecraftVarintPrepender extends MessageToMessageEncoder<ByteBuf> {
    public static final MinecraftVarintPrepender INSTANCE = new MinecraftVarintPrepender();
    static final boolean IS_JAVA_CIPHER = Natives.cipher.get() == JavaVelocityCipher.FACTORY;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final int length = msg.readableBytes();
        final int varintLength = VarInts.getSizeInBytes(length);

        // this isn't optimal (ideally, we would use the trick Velocity uses and combine the prepender and
        // compressor into one), but to maximize mod compatibility, we do this instead
        final ByteBuf lenBuf = IS_JAVA_CIPHER
                ? ctx.alloc().heapBuffer(varintLength)
                : ctx.alloc().directBuffer(varintLength);

        VarInts.write(lenBuf, length);
        out.add(lenBuf);
        out.add(msg.retain());
    }
}
