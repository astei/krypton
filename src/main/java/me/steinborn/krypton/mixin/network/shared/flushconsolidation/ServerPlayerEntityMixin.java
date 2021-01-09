package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.steinborn.krypton.mod.network.util.AutoFlushUtil.setAutoFlush;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Inject(at = @At("HEAD"), method = "playerTick")
    public void playerTick$disableAutoFlush(CallbackInfo ci) {
        setAutoFlush(networkHandler.getConnection(), false);
    }

    @Inject(at = @At("RETURN"), method = "playerTick")
    public void playerTick$reenableAutoFlush(CallbackInfo ci) {
        setAutoFlush(networkHandler.getConnection(), true);
    }
}
