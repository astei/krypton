package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Shadow @Final Connection connection;

    @Inject(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(ServerboundKeyPacket packet, CallbackInfo ci, String _digest, PrivateKey _serverPrivateKey, SecretKey secretKey) throws GeneralSecurityException {
        // The `ClientConnectionEncryptionExtension` implementation in `ConnectionMixin` will replace
        // the pipeline handler vanilla just installed with Velocity's own.
        ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) secretKey);
    }
}
