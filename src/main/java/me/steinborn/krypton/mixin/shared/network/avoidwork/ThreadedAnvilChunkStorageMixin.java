package me.steinborn.krypton.mixin.shared.network.avoidwork;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.steinborn.krypton.mod.shared.WorldEntityByChunkAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {

    private static final Int2ObjectMap<Entity> DUMMY = Int2ObjectMaps.unmodifiable(new Int2ObjectOpenHashMap<>());

    @Shadow @Final private Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;

    @Inject(method = "sendChunkDataPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/DebugInfoSender;sendChunkWatchingChange(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/ChunkPos;)V", shift = At.Shift.AFTER, by = 1))
    public void sendChunkDataPackets$beSmart(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo ci) {
        // Synopsis: when sending chunk data to the player, sendChunkDataPackets iterates over EVERY tracked entity in
        // the world, when it doesn't have to do so - we only need entities in the current chunk. A similar optimization
        // is present in Paper.
        final Collection<Entity> entitiesInChunk = ((WorldEntityByChunkAccess) chunk.getWorld()).getEntitiesInChunk(chunk.getPos().x, chunk.getPos().z);
        final List<Entity> attachmentsToSend = new ArrayList<>();
        final List<Entity> passengersToSend = new ArrayList<>();
        for (Entity entity : entitiesInChunk) {
            final ThreadedAnvilChunkStorage.EntityTracker entityTracker = this.entityTrackers.get(entity.getId());
            if (entityTracker != null) {
                entityTracker.updateTrackedStatus(player);
                if (entity instanceof MobEntity && ((MobEntity)entity).getHoldingEntity() != null) {
                    attachmentsToSend.add(entity);
                }

                if (!entity.getPassengerList().isEmpty()) {
                    passengersToSend.add(entity);
                }
            }
        }

        if (!attachmentsToSend.isEmpty()) {
            for (Entity entity : attachmentsToSend) {
                player.networkHandler.sendPacket(new EntityAttachS2CPacket(entity, ((MobEntity) entity).getHoldingEntity()));
            }
        }

        if (!passengersToSend.isEmpty()) {
            for (Entity entity : passengersToSend) {
                player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
            }
        }
    }

    @Redirect(method = "sendChunkDataPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;entityTrackers:Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;", opcode = Opcodes.GETFIELD))
    public Int2ObjectMap<Entity> sendChunkDataPackets$nullifyRest(ThreadedAnvilChunkStorage tacs) {
        return DUMMY;
    }
}
