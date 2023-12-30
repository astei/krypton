package me.steinborn.krypton.mixin.shared.bugfix;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CustomPayloadS2CPacket.class)
public class CustomPayloadS2CPacketFabricAPICompatMixin {

    @ModifyVariable(method = "readPayload", index = 1, at = @At(value = "HEAD"), argsOnly = true)
    private static PacketByteBuf readPayload$explicitCopy(PacketByteBuf buf) {
        PacketByteBuf copy = new PacketByteBuf(Unpooled.copiedBuffer(buf.copy()));
        // Pretend to consume everything in the buffer
        buf.skipBytes(buf.readableBytes());
        return copy;
    }
}
