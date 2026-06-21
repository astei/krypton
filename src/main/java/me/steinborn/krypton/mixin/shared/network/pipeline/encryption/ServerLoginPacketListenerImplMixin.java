package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Shadow 
    @Final 
    Connection connection;

    @Redirect(
        method = "handleKey", 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"
        )
    )
    public void onKey$redirectEncryptionSetup(Connection connection, Cipher decryptCipher, Cipher encryptCipher) 
            throws GeneralSecurityException {
        // We need to get the SecretKey from somewhere - the original method
        // used the Cipher objects. We'll need to extract the key.
        // Alternatively, we can still call the original method but with our 
        // optimized pipeline already in place.
        
        // Let's use a different approach - we'll still call the vanilla method
        // but our ConnectionMixin will replace the handlers
        connection.setEncryptionKey(decryptCipher, encryptCipher);
    }
}