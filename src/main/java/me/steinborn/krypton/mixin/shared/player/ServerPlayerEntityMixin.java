package me.steinborn.krypton.mixin.shared.player;


import me.steinborn.krypton.mod.shared.player.KryptonServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayerEntity.class)
@Implements(@Interface(iface = KryptonServerPlayerEntity.class, prefix = "krypton$", unique = true))
public class ServerPlayerEntityMixin implements KryptonServerPlayerEntity {
    @Unique
    private int playerViewDistance = -1;

    @Unique
    private boolean updatedViewDistance = false;

    @Inject(method = "setClientSettings", at = @At("HEAD"))
    public void setClientSettings(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        updatedViewDistance = (playerViewDistance != packet.viewDistance());
        playerViewDistance = packet.viewDistance();
    }

    @Override
    public boolean isUpdatedViewDistance() {
        return updatedViewDistance;
    }

    @Override
    public void setUpdatedViewDistance(boolean updatedViewDistance) {
        this.updatedViewDistance = updatedViewDistance;
    }

    @Override
    public int getPlayerViewDistance() {
        return playerViewDistance;
    }
}
