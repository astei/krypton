package me.steinborn.krypton.mixin.shared.network.flushconsolidation;


import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.steinborn.krypton.mod.shared.network.util.AutoFlushUtil;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * Mixes into various methods in {@code ThreadedAnvilChunkStorage} to utilize flush consolidation for sending chunks all at once to the
 * client. Helpful for heavy server activity or flying very quickly.
 * <p>
 * Note for anyone attempting to modify this class in the future: for some reason, mojang includes both the chunk loading & chunk unloading
 * packets in the <i>same</i> method. This is why chunks must <i>always</i> be sent to the player when they leave an area.
 */
@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {
    private static final Logger LOGGER = LogManager.getLogger(ThreadedAnvilChunkStorageMixin.class);
    
    @Shadow
    @Final
    private Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;
    
    @Shadow
    @Final
    private PlayerChunkWatchingManager playerChunkWatchingManager;
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Shadow
    @Final
    private ThreadedAnvilChunkStorage.TicketManager ticketManager;
    
    @Shadow
    private int watchDistance;
    
    @Shadow
    public static boolean isWithinDistance(int x1, int y1, int x2, int y2, int maxDistance) {
        // PAIL: isWithinEuclideanDistance(x1, y1, x2, y2, maxDistance)
        throw new AssertionError("pedantic");
    }
    
    /**
     * This is run on login. This method is overwritten to avoid sending duplicate chunks (which mc does by default)
     *
     * @author solonovamax
     */
    @Overwrite
    public void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added) {
        boolean isWatchingWorld = this.playerChunkWatchingManager.isWatchInactive(player);
        boolean doesChunkGen = !this.doesNotGenerateChunks(player);
        
        int chunkPosX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        int chunkPosZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());
        
        if (added) {
            this.playerChunkWatchingManager.add(ChunkPos.toLong(chunkPosX, chunkPosZ), player, isWatchingWorld);
            this.updateWatchedSection(player);
            if (!isWatchingWorld) {
                this.ticketManager.handleChunkEnter(ChunkSectionPos.from(player), player);
            }
        } else {
            ChunkSectionPos chunkSectionPos = player.getWatchedSection();
            this.playerChunkWatchingManager.remove(chunkSectionPos.toChunkPos().toLong(), player);
            
            if (doesChunkGen) {
                this.ticketManager.handleChunkLeave(chunkSectionPos, player);
            }
        }
        
        // Send chunk watch packets even if the player has been removed, as this also send chunk unload packets
        sendSpiralChunkWatchPackets(player);
    }
    
    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation
     */
    @Overwrite
    public void updatePosition(ServerPlayerEntity player) {
        // TODO: optimize this further by only considering entities that the player is close to.
        //       use the FastChunkEntityAccess magic to do this.
        for (ThreadedAnvilChunkStorage.EntityTracker entityTracker : this.entityTrackers.values()) {
            if (entityTracker.entity == player) {
                entityTracker.updateTrackedStatus(this.world.getPlayers());
            } else {
                entityTracker.updateTrackedStatus(player);
            }
        }
        
        ChunkSectionPos oldPos = player.getWatchedSection();
        ChunkSectionPos newPos = ChunkSectionPos.from(player);
        boolean isWatchingWorld = this.playerChunkWatchingManager.isWatchDisabled(player);
        boolean noChunkGen = this.doesNotGenerateChunks(player);
        boolean movedSections = !oldPos.equals(newPos);
        
        
        if (movedSections || isWatchingWorld != noChunkGen) {
            this.updateWatchedSection(player);
            
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
        }
        
        // The player *always* needs to be send chunks, as for some reason both chunk loading & unloading packets are handled
        // by the same method (why mojang)
        if (player.world == this.world) {
            this.sendChunkWatchPackets(oldPos, player);
        }
    }
    
    @Inject(method = "tickEntityMovement", at = @At("HEAD"))
    public void disableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            AutoFlushUtil.setAutoFlush(player, false);
        }
    }
    
    @Inject(method = "tickEntityMovement", at = @At("RETURN"))
    public void enableAutoFlushForEntityTracking(CallbackInfo info) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }
    
    @Shadow
    public abstract void sendWatchPackets(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> mutableObject,
                                          boolean withinMaxWatchDistance, boolean withinViewDistance);
    
    @Shadow
    protected abstract boolean doesNotGenerateChunks(ServerPlayerEntity player);
    
    @Shadow
    protected abstract ChunkSectionPos updateWatchedSection(ServerPlayerEntity serverPlayerEntity);
    
    /**
     * Sends watch packets to the client in a spiral for a player which has *no* chunks loaded in the area.
     *
     * @author solonovamax
     */
    private void sendSpiralChunkWatchPackets(ServerPlayerEntity player) {
        int chunkPosX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        int chunkPosZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());
        
        int x = 0, z = 0, dx = 0, dz = -1;
        int t = this.watchDistance * 2;
        int maxI = t * t * 2;
        for (int i = 0; i < maxI; i++) {
            if ((-this.watchDistance <= x) && (x <= this.watchDistance) && (-this.watchDistance <= z) && (z <= this.watchDistance)) {
                LOGGER.info("Sending chunk at pos [{}, {}]", chunkPosX + x, chunkPosZ + z);
                
                this.sendWatchPackets(player,
                                      new ChunkPos(chunkPosX + x, chunkPosZ + z),
                                      new MutableObject<>(), false, true);
            }
            if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
                t = dx;
                dx = -dz;
                dz = t;
            }
            x += dx;
            z += dz;
        }
    }
    
    private void sendChunkWatchPackets(ChunkSectionPos oldPos, ServerPlayerEntity player) {
        AutoFlushUtil.setAutoFlush(player, false);
        
        try {
            int oldChunkX = oldPos.getSectionX();
            int oldChunkZ = oldPos.getSectionZ();
            
            int newChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
            int newChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());
            
            // TODO: Track chunks the server has sent the player
            if (Math.abs(oldChunkX - newChunkX) <= this.watchDistance * 2 && Math.abs(oldChunkZ - newChunkZ) <= this.watchDistance * 2) {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - this.watchDistance - 1;
                int minSendChunkZ = Math.min(newChunkZ, oldChunkZ) - this.watchDistance - 1;
                int maxSendChunkX = Math.max(newChunkX, oldChunkX) + this.watchDistance + 1;
                int maxSendChunkZ = Math.max(newChunkZ, oldChunkZ) + this.watchDistance + 1;
                
                // We're sending *all* chunks in the range of where the player was, to where the player currently is.
                // This is because the #sendWatchPackets method will also unload chunks.
                // For chunks outside of the view distance, it does nothing.
                for (int curX = minSendChunkX; curX <= maxSendChunkX; ++curX) {
                    for (int curZ = minSendChunkZ; curZ <= maxSendChunkZ; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = isWithinDistance(curX, curZ, oldChunkX, oldChunkZ, this.watchDistance);
                        boolean inNew = isWithinDistance(curX, curZ, newChunkX, newChunkZ, this.watchDistance);
                        this.sendWatchPackets(player, chunkPos, new MutableObject<>(), inOld, inNew);
                    }
                }
            } else { // If the player is not near the old chunks, send all new chunks & unload old chunks
                
                // Unload previous chunks
                // Chunk unload packets are very light, so we can just do it like this
                for (int curX = oldChunkX - watchDistance - 1; curX <= oldChunkX + watchDistance + 1; ++curX) {
                    for (int curZ = oldChunkZ - watchDistance - 1; curZ <= oldChunkZ + watchDistance + 1; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        
                        this.sendWatchPackets(player, chunkPos, new MutableObject<>(), false, true);
                    }
                }
                
                // Send new chunks
                sendSpiralChunkWatchPackets(player);
                
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }
    
}
