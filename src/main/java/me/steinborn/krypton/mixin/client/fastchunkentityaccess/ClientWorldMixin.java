package me.steinborn.krypton.mixin.client.fastchunkentityaccess;

import me.steinborn.krypton.mod.shared.WorldEntityByChunkAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(ClientWorld.class)
@Environment(EnvType.CLIENT)
public class ClientWorldMixin implements WorldEntityByChunkAccess {

    @Shadow @Final private ClientEntityManager<Entity> entityManager;

    @Override
    public Collection<Entity> getEntitiesInChunk(int chunkX, int chunkZ) {
        return ((WorldEntityByChunkAccess) this.entityManager.cache).getEntitiesInChunk(chunkX, chunkZ);
    }
}
