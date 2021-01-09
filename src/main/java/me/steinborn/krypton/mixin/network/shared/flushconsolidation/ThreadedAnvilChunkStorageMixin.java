package me.steinborn.krypton.mixin.network.shared.flushconsolidation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.steinborn.krypton.mod.network.ConfigurableAutoFlush;
import net.minecraft.network.Packet;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into various methods in {@code ThreadedAnvilChunkStorage} to utilize flush consolidation for sending chunks
 * all at once to the client. Helpful for heavy server activity or flying very quickly.
 */
@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final private Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;
    @Shadow @Final private PlayerChunkWatchingManager playerChunkWatchingManager;
    @Shadow @Final private ServerWorld world;
    @Shadow @Final private ThreadedAnvilChunkStorage.TicketManager ticketManager;
    @Shadow private int watchDistance;

    @Shadow protected abstract boolean doesNotGenerateChunks(ServerPlayerEntity player);

    @Shadow protected abstract ChunkSectionPos method_20726(ServerPlayerEntity serverPlayerEntity);

    @Shadow
    private static int getChebyshevDistance(ChunkPos pos, int x, int z) {
        throw new AssertionError("pedantic");
    }

    @Shadow public abstract void sendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk);

    @Shadow @Nullable protected abstract ChunkHolder getChunkHolder(long pos);

    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation
     */
    @Overwrite
    public void updateCameraPosition(ServerPlayerEntity player) {
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : this.entityTrackers.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updateCameraPosition(this.world.getPlayers());
            } else {
                entityTracker.updateCameraPosition(player);
            }
        }

        ChunkSectionPos oldPos = player.getCameraPosition();
        ChunkSectionPos newPos = ChunkSectionPos.from(player);
        boolean isWatchingWorld = this.playerChunkWatchingManager.isWatchDisabled(player);
        boolean noChunkGen = this.doesNotGenerateChunks(player);
        boolean movedSections = !oldPos.equals(newPos);

        if (movedSections || isWatchingWorld != noChunkGen) {
            this.method_20726(player);

            if (!isWatchingWorld) {
                this.ticketManager.handleChunkLeave(oldPos, player);
            }

            if (!noChunkGen) {
                this.ticketManager.handleChunkEnter(newPos, player);
            }

            if (!isWatchingWorld && noChunkGen) {
                this.playerChunkWatchingManager.disableWatch(player);
            }

            if (isWatchingWorld && !noChunkGen) {
                this.playerChunkWatchingManager.enableWatch(player);
            }

            long oldChunkPos = ChunkPos.toLong(oldPos.getX(), oldPos.getZ());
            long newChunkPos = ChunkPos.toLong(newPos.getX(), newPos.getZ());
            this.playerChunkWatchingManager.movePlayer(oldChunkPos, newChunkPos, player);

            // If the player is in the same world as this tracker, we should send them chunks.
            if (this.world == player.world) {
                this.sendChunks(oldPos, player);
            }
        }
    }

    private void sendChunks(ChunkSectionPos oldPos, ServerPlayerEntity player) {
        setAutoFlush(player, false);

        int oldChunkX = oldPos.getSectionX();
        int oldChunkZ = oldPos.getSectionZ();

        int newChunkX = MathHelper.floor(player.getX()) >> 4;
        int newChunkZ = MathHelper.floor(player.getZ()) >> 4;

        if (Math.abs(oldChunkX - newChunkX) <= this.watchDistance * 2 && Math.abs(oldChunkZ - newChunkZ) <= this.watchDistance * 2) {
            int minSendChunkX = Math.min(newChunkX, oldChunkX) - this.watchDistance;
            int maxSendChunkZ = Math.min(newChunkZ, oldChunkZ) - this.watchDistance;
            int q = Math.max(newChunkX, oldChunkX) + this.watchDistance;
            int r = Math.max(newChunkZ, oldChunkZ) + this.watchDistance;

            for(int curX = minSendChunkX; curX <= q; ++curX) {
                for(int curZ = maxSendChunkZ; curZ <= r; ++curZ) {
                    ChunkPos chunkPos = new ChunkPos(curX, curZ);
                    boolean inOld = getChebyshevDistance(chunkPos, oldChunkX, oldChunkZ) <= this.watchDistance;
                    boolean inNew = getChebyshevDistance(chunkPos, newChunkX, newChunkZ) <= this.watchDistance;
                    this.sendPacketsForChunk(player, chunkPos, new Packet[2], inOld, inNew);
                }
            }
        } else {
            for(int curX = oldChunkX - this.watchDistance; curX <= oldChunkX + this.watchDistance; ++curX) {
                for(int curZ = oldChunkZ - this.watchDistance; curZ <= oldChunkZ + this.watchDistance; ++curZ) {
                    ChunkPos pos = new ChunkPos(curX, curZ);
                    this.sendPacketsForChunk(player, pos, new Packet[2], true, false);
                }
            }

            for(int curX = newChunkX - this.watchDistance; curX <= newChunkX + this.watchDistance; ++curX) {
                for(int curZ = newChunkZ - this.watchDistance; curZ <= newChunkZ + this.watchDistance; ++curZ) {
                    ChunkPos pos = new ChunkPos(curX, curZ);
                    this.sendPacketsForChunk(player, pos, new Packet[2], false, true);
                }
            }
        }

        setAutoFlush(player, true);
    }

    protected void sendPacketsForChunk(ServerPlayerEntity player, ChunkPos pos, Packet<?>[] packets, boolean withinMaxWatchDistance, boolean withinViewDistance) {
        if (withinViewDistance && !withinMaxWatchDistance) {
            ChunkHolder chunkHolder = this.getChunkHolder(pos.toLong());
            if (chunkHolder != null) {
                WorldChunk worldChunk = chunkHolder.getWorldChunk();
                if (worldChunk != null) {
                    this.sendChunkDataPackets(player, packets, worldChunk);
                }

                DebugInfoSender.sendChunkWatchingChange(this.world, pos);
            }
        }

        if (!withinViewDistance && withinMaxWatchDistance) {
            player.sendUnloadChunkPacket(pos);
        }
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
            ConfigurableAutoFlush configurableAutoFlusher = ((ConfigurableAutoFlush) player.networkHandler.getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }
}
