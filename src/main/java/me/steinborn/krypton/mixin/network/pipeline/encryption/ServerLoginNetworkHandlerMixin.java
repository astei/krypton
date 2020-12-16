package me.steinborn.krypton.mixin.network.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import me.steinborn.krypton.mod.network.ClientConnectionEncryptionExtension;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkEncryptionUtils;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {
    @Shadow private SecretKey secretKey;

    @Shadow @Final public ClientConnection connection;

    @Inject(method = "onKey", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;secretKey:Ljavax/crypto/SecretKey;", ordinal = 1))
    public void onKey$initializeVelocityCipher(LoginKeyC2SPacket packet, CallbackInfo info) throws GeneralSecurityException {
        ((ClientConnectionEncryptionExtension) this.connection).setupEncryption(this.secretKey);
    }

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkEncryptionUtils;cipherFromKey(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher onKey$ignoreJavaCipherInitialization(int ignored1, Key ignored2) {
        // Turn the operation into a no-op.
        return null;
    }

    @Redirect(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;setupEncryption(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(ClientConnection connection, Cipher ignored1, Cipher ignored2) {
        // Turn the operation into a no-op.
    }
}
