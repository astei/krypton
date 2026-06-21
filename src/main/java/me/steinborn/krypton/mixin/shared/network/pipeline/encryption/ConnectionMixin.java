package me.steinborn.krypton.mixin.shared.network.pipeline.encryption;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import me.steinborn.krypton.mod.shared.misc.KryptonPipelineEvent;
import me.steinborn.krypton.mod.shared.network.ClientConnectionEncryptionExtension;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherDecoder;
import me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherEncoder;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(Connection.class)
public class ConnectionMixin implements ClientConnectionEncryptionExtension {
    @Shadow
    private Channel channel;
    
    @Unique
    private boolean kryptonEncryptionActive = false;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        // Check if encryption is already active by looking at the pipeline
        if (!kryptonEncryptionActive && !isEncryptionActive()) {
            VelocityCipher decryption = Natives.cipher.get().forDecryption(key);
            VelocityCipher encryption = Natives.cipher.get().forEncryption(key);

            kryptonEncryptionActive = true;
            
            ChannelPipeline pipeline = this.channel.pipeline();
            
            // Replace Mojang's encryption with our optimized version
            // First, remove any existing encryption handlers
            if (pipeline.get("decrypt") != null) {
                pipeline.remove("decrypt");
            }
            if (pipeline.get("encrypt") != null) {
                pipeline.remove("encrypt");
            }
            
            // Add our optimized encryption handlers
            pipeline.addBefore("splitter", "decrypt", new MinecraftCipherDecoder(decryption));
            pipeline.addBefore("prepender", "encrypt", new MinecraftCipherEncoder(encryption));

            this.channel.pipeline().fireUserEventTriggered(KryptonPipelineEvent.ENCRYPTION_ENABLED);
        }
    }
    
    @Unique
    private boolean isEncryptionActive() {
        if (this.channel == null) {
            return false;
        }
        ChannelPipeline pipeline = this.channel.pipeline();
        // Check if our encryption is active or if Mojang's is active
        return pipeline.get("decrypt") instanceof MinecraftCipherDecoder 
            || pipeline.get("decrypt") instanceof net.minecraft.network.CipherDecoder;
    }
}