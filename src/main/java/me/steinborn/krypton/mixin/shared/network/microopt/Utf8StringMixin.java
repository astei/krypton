package me.steinborn.krypton.mixin.shared.network.microopt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.charset.StandardCharsets;

@Mixin(Utf8String.class)
public class Utf8StringMixin {
    /**
     * @author Andrew Steinborn
     * @reason optimized version
     */
    @Overwrite
    public static void write(ByteBuf buf, CharSequence string, int length) {
        // Mojang _almost_ gets it right, but stumbles at the finish line...
        if (string.length() > length) {
            throw new EncoderException("String too big (was " + string.length() + " characters, max " + length + ")");
        }
        int utf8Bytes = ByteBufUtil.utf8Bytes(string);
        int maxBytesPermitted = ByteBufUtil.utf8MaxBytes(length);
        if (utf8Bytes > maxBytesPermitted) {
            throw new EncoderException("String too big (was " + utf8Bytes + " bytes encoded, max " + maxBytesPermitted + ")");
        } else {
            VarInt.write(buf, utf8Bytes);
            buf.writeCharSequence(string, StandardCharsets.UTF_8);
        }
    }
}
