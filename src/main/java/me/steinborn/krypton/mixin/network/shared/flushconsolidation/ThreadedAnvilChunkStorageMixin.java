package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import me.steinborn.krypton.mod.network.ConfigurableAutoFlush;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final private ServerWorld world;

    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"))
    public void prepareToSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo info) {
        setAutoFlush(player, false);
    }

    @Inject(method = "sendChunkDataPackets", at = @At("RETURN"))
    public void finishSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo info) {
        setAutoFlush(player, true);
    }

    @Inject(method = "tickPlayerMovement", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            setAutoFlush(player, false);
        }
    }

    @Inject(method = "tickPlayerMovement", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            setAutoFlush(player, true);
        }
    }

    private static void setAutoFlush(ServerPlayerEntity player, boolean val) {
        if (player.getClass() == ServerPlayerEntity.class) {
            ((ConfigurableAutoFlush) player.networkHandler.getConnection()).setShouldAutoFlush(val);
        }
    }
}
