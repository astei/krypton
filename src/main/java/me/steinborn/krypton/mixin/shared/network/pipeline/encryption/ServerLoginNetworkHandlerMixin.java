package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {
    @Shadow @Final ClientConnection connection;

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/encryption/NetworkEncryptionUtils;cipherFromKey(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher onKey$initializeVelocityCipher(int ignored1, Key secretKey) throws GeneralSecurityException {
        // Hijack this portion of the cipher initialization and set up our own encryption handler.
        ((ClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) secretKey);

        // Turn the operation into a no-op.
        return null;
    }

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;setupEncryption(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(ClientConnection connection, Cipher ignored1, Cipher ignored2) {
        // Turn the operation into a no-op.
    }
}
