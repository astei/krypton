package me.steinborn.krypton.mixin.network.flushconsolidation;

import me.steinborn.krypton.mod.network.ConfigurableAutoFlush;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Inject(method = "sendChunkDataPackets", at = @At("HEAD"))
    public void prepareToSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo info) {
        // Guard against fake players
        if (player.getClass() == ServerPlayerEntity.class) {
            ((ConfigurableAutoFlush) player.networkHandler.getConnection()).setShouldAutoFlush(false);
        }
    }

    @Inject(method = "sendChunkDataPackets", at = @At("RETURN"))
    public void finishSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo info) {
        // Guard against fake players
        if (player.getClass() == ServerPlayerEntity.class) {
            ((ConfigurableAutoFlush) player.networkHandler.getConnection()).setShouldAutoFlush(true);
        }
    }
}
