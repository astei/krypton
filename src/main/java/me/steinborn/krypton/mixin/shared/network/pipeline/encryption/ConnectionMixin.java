package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import me.steinborn.krypton.mod.shared.misc.KryptonPipelineEvent;
import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherDecoder;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherEncoder;
import net.minecraft.network.CipherDecoder;
import net.minecraft.network.CipherEncoder;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(Connection.class)
public class ConnectionMixin implements ClientConnectionEncryptionExtension {

    @Shadow private Channel channel;
    @Unique private boolean kryptonEncryptionEnabled = false;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        if (this.kryptonEncryptionEnabled) {
            return;
        }

        if (isMinecraftEncryption(this.channel.pipeline().get("decrypt"))) {
            this.channel.pipeline().remove("decrypt");
        }
        if (isMinecraftEncryption(this.channel.pipeline().get("encrypt"))) {
            this.channel.pipeline().remove("encrypt");
        }

        if (key == null) {
            // Normally this is something that shouldn't really happen.
            // However, some mods like to muck around in the encryption process
            // and disable it. The intended injection point here is to ensure
            // that `ServerboundKeyPacket.getSecretKey(PrivateKey)` returns `null`,
            // and Krypton will still "just work".
            return;
        }

        VelocityCipher decryption = Natives.cipher.get().forDecryption(key);
        VelocityCipher encryption = Natives.cipher.get().forEncryption(key);

        this.channel.pipeline().addBefore("splitter", "decrypt", new MinecraftCipherDecoder(decryption));
        this.channel.pipeline().addBefore("prepender", "encrypt", new MinecraftCipherEncoder(encryption));
        this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.ENCRYPTION_ENABLED);

        this.kryptonEncryptionEnabled = true;
    }

    @Unique
    private static boolean isMinecraftEncryption(ChannelHandler handler) {
        return handler instanceof CipherDecoder || handler instanceof CipherEncoder;
    }
}
