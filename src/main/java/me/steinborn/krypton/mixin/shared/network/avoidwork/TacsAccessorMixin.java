package me.steinborn.krypton.mixin.shared.network.avoidwork;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface TacsAccessorMixin {

    @Accessor
    ServerWorld getWorld();
}
