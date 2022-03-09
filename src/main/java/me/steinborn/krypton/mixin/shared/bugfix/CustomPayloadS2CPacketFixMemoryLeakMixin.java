package me.steinborn.krypton.mixin.shared.bugfix;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CustomPayloadS2CPacket.class)
public class CustomPayloadS2CPacketFixMemoryLeakMixin {

    @Redirect(method = "getData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;copy()Lio/netty/buffer/ByteBuf;"))
    private ByteBuf getData$copyShouldBeSlice(PacketByteBuf instance) {
        return instance.slice();
    }
}
