package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.steinborn.krypton.mod.network.util.AutoFlushUtil.setAutoFlush;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(at = @At("HEAD"), method = "onPlayerConnect")
    public void onPlayerConnect$disableAutoFlush(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(connection, false);
    }

    @Inject(at = @At("RETURN"), method = "onPlayerConnect")
    public void onPlayerConnect$reenableAutoFlush(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        setAutoFlush(connection, true);
    }

    @Inject(at = @At("HEAD"), method = "respawnPlayer")
    public void onPlayerConnect$disableAutoFlush(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> ci) {
        setAutoFlush(player, false);
    }

    @Inject(at = @At("RETURN"), method = "respawnPlayer")
    public void onPlayerConnect$reenableAutoFlush(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> ci) {
        setAutoFlush(player, true);
    }
}
