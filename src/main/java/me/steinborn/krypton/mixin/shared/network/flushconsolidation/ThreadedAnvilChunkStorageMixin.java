package me.steinborn.krypton.mixin.shared.network.flushconsolidation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.steinborn.krypton.mod.shared.network.util.AutoFlushUtil;
import me.steinborn.krypton.mod.shared.player.KryptonServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.PlayerChunkWatchingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into various methods in {@code ThreadedAnvilChunkStorage} to utilize flush consolidation for sending chunks all at once to the
 * client, along with loading chunks in a spiral order. Helpful for heavy server activity or flying very quickly.
 * <p>
 * Note for anyone attempting to modify this class in the future: for some reason, mojang includes both the chunk loading & chunk unloading
 * packets in the <i>same</i> method. This is why chunks must <i>always</i> be sent to the player when they leave an area.
 */
@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {
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
     * @reason optimize sending chunks
     * @author solonovamax
     */
    @Overwrite
    public void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added) {
        boolean doesNotGenerateChunks = this.doesNotGenerateChunks(player);
        boolean isWatchingWorld = !this.playerChunkWatchingManager.isWatchInactive(player);

        int chunkPosX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        int chunkPosZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

        AutoFlushUtil.setAutoFlush(player, false);
        try {
            if (added) {
                this.playerChunkWatchingManager.add(ChunkPos.toLong(chunkPosX, chunkPosZ), player, doesNotGenerateChunks);
                this.updateWatchedSection(player);

                if (!doesNotGenerateChunks) {
                    this.ticketManager.handleChunkEnter(ChunkSectionPos.from(player), player);
                }

                // Send spiral watch packets if added
                sendSpiralChunkWatchPackets(player);
            } else {
                ChunkSectionPos chunkSectionPos = player.getWatchedSection();
                this.playerChunkWatchingManager.remove(chunkSectionPos.toChunkPos().toLong(), player);

                if (isWatchingWorld) {
                    this.ticketManager.handleChunkLeave(chunkSectionPos, player);
                }

                // Loop through & unload chunks if removed
                unloadChunks(player, chunkPosX, chunkPosZ, watchDistance);
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * @author Andrew Steinborn
     * @reason Add support for flush consolidation & optimize sending chunks
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
        if (player.getWorld() == this.world)
            this.sendChunkWatchPackets(oldPos, player);
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

    /**
     * @param player                The player
     * @param pos                   The position of the chunk to send
     * @param mutableObject         A new mutable object
     * @param oldWithinViewDistance If the chunk was previously within the player's view distance
     * @param newWithinViewDistance If the chunk is now within the player's view distance
     */
    @Shadow
    public abstract void sendWatchPackets(ServerPlayerEntity player, ChunkPos pos, MutableObject<ChunkDataS2CPacket> mutableObject,
                                          boolean oldWithinViewDistance, boolean newWithinViewDistance);

    @Shadow
    protected abstract boolean doesNotGenerateChunks(ServerPlayerEntity player);

    @Shadow
    protected abstract ChunkSectionPos updateWatchedSection(ServerPlayerEntity serverPlayerEntity);

    private void sendChunkWatchPackets(ChunkSectionPos oldPos, ServerPlayerEntity player) {
        AutoFlushUtil.setAutoFlush(player, false);
        try {
            int oldChunkX = oldPos.getSectionX();
            int oldChunkZ = oldPos.getSectionZ();

            int newChunkX = ChunkSectionPos.getSectionCoord(player.getBlockX());
            int newChunkZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());

            int playerViewDistance = getPlayerViewDistance(player); // +1 for buffer

            if (shouldReloadAllChunks(player)) { // Player updated view distance, unload chunks & resend (only unload chunks not visible)
                //noinspection InstanceofIncompatibleInterface
                if (player instanceof KryptonServerPlayerEntity kryptonPlayer)
                    kryptonPlayer.setNeedsChunksReloaded(false);

                for (int curX = newChunkX - watchDistance - 1; curX <= newChunkX + watchDistance + 1; ++curX) {
                    for (int curZ = newChunkZ - watchDistance - 1; curZ <= newChunkZ + watchDistance + 1; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inNew = isWithinDistance(curX, curZ, newChunkX, newChunkZ, playerViewDistance);

                        this.sendWatchPackets(player, chunkPos, new MutableObject<>(), true, inNew);
                    }
                }

                // Send new chunks
                sendSpiralChunkWatchPackets(player);
            } else if (Math.abs(oldChunkX - newChunkX) > playerViewDistance * 2 ||
                       Math.abs(oldChunkZ - newChunkZ) > playerViewDistance * 2) {
                // If the player is not near the old chunks, send all new chunks & unload old chunks

                // Unload previous chunks
                // Chunk unload packets are very light, so we can just do it like this
                unloadChunks(player, oldChunkX, oldChunkZ, watchDistance);

                // Send new chunks
                sendSpiralChunkWatchPackets(player);
            } else {
                int minSendChunkX = Math.min(newChunkX, oldChunkX) - playerViewDistance - 1;
                int minSendChunkZ = Math.min(newChunkZ, oldChunkZ) - playerViewDistance - 1;
                int maxSendChunkX = Math.max(newChunkX, oldChunkX) + playerViewDistance + 1;
                int maxSendChunkZ = Math.max(newChunkZ, oldChunkZ) + playerViewDistance + 1;

                // We're sending *all* chunks in the range of where the player was, to where the player currently is.
                // This is because the #sendWatchPackets method will also unload chunks.
                // For chunks outside of the view distance, it does nothing.
                for (int curX = minSendChunkX; curX <= maxSendChunkX; ++curX) {
                    for (int curZ = minSendChunkZ; curZ <= maxSendChunkZ; ++curZ) {
                        ChunkPos chunkPos = new ChunkPos(curX, curZ);
                        boolean inOld = isWithinDistance(curX, curZ, oldChunkX, oldChunkZ, playerViewDistance);
                        boolean inNew = isWithinDistance(curX, curZ, newChunkX, newChunkZ, playerViewDistance);
                        this.sendWatchPackets(player, chunkPos, new MutableObject<>(), inOld, inNew);
                    }
                }
            }
        } finally {
            AutoFlushUtil.setAutoFlush(player, true);
        }
    }

    /**
     * Sends watch packets to the client in a spiral for a player, which has *no* chunks loaded in the area.
     */
    private void sendSpiralChunkWatchPackets(ServerPlayerEntity player) {
        int chunkPosX = ChunkSectionPos.getSectionCoord(player.getBlockX());
        int chunkPosZ = ChunkSectionPos.getSectionCoord(player.getBlockZ());


        // + 1 because mc adds 1 when it sends chunks
        int playerViewDistance = getPlayerViewDistance(player) + 1;

        int x = 0, z = 0, dx = 0, dz = -1;
        int t = playerViewDistance * 2;
        int maxI = t * t * 2;
        for (int i = 0; i < maxI; i++) {
            if ((-playerViewDistance <= x) && (x <= playerViewDistance) && (-playerViewDistance <= z) && (z <= playerViewDistance)) {
                boolean inNew = isWithinDistance(chunkPosX, chunkPosZ, chunkPosX + x, chunkPosZ + z, playerViewDistance);

                this.sendWatchPackets(player,
                        new ChunkPos(chunkPosX + x, chunkPosZ + z),
                        new MutableObject<>(), false, inNew
                );
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

    private void unloadChunks(ServerPlayerEntity player, int chunkPosX, int chunkPosZ, int distance) {
        for (int curX = chunkPosX - distance - 1; curX <= chunkPosX + distance + 1; ++curX) {
            for (int curZ = chunkPosZ - distance - 1; curZ <= chunkPosZ + distance + 1; ++curZ) {
                ChunkPos chunkPos = new ChunkPos(curX, curZ);

                this.sendWatchPackets(player, chunkPos, new MutableObject<>(), true, false);
            }
        }
    }

    private int getPlayerViewDistance(ServerPlayerEntity playerEntity) {
        //noinspection InstanceofIncompatibleInterface
        return playerEntity instanceof KryptonServerPlayerEntity kryptonPlayerEntity
               ? kryptonPlayerEntity.getPlayerViewDistance() != -1
                 // if -1, the view distance hasn't been set
                 // We *actually* need to send view distance + 1, because mc doesn't render chunks adjacent to unloaded ones
                 ? Math.min(this.watchDistance,
                            kryptonPlayerEntity.getPlayerViewDistance() +
                            1)
                 : this.watchDistance : this.watchDistance;
    }

    private boolean shouldReloadAllChunks(ServerPlayerEntity playerEntity) {
        //noinspection InstanceofIncompatibleInterface
        return playerEntity instanceof KryptonServerPlayerEntity kryptonPlayerEntity && kryptonPlayerEntity.getNeedsChunksReloaded();
    }
}
