package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.encoding.StringEncoding;
import net.minecraft.network.encoding.VarInts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.charset.StandardCharsets;

@Mixin(StringEncoding.class)
public class StringEncodingMixin {
    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public static void encode(ByteBuf buf, CharSequence string, int length) {
        // Mojang _almost_ gets it right, but stumbles at the finish line...
        if (string.length() > length) {
            throw new EncoderException("String too big (was " + string.length() + " characters, max " + length + ")");
        }
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        int maxBytesPermitted = ByteBufUtil.utf8MaxBytes(length);
        if (utf8Bytes > maxBytesPermitted) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + maxBytesPermitted + ")");
        } else {
            VarInts.write(buf, utf8Bytes);
            buf.writeCharSequence(string, StandardCharsets.UTF_8);
        }
    }
}
