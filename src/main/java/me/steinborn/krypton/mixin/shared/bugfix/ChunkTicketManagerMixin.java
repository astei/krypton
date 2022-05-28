package me.steinborn.krypton.mixin.shared.bugfix;


import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(ChunkTicketManager.class)
public class ChunkTicketManagerMixin {

    @Redirect(method = "handleChunkLeave(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
              at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
    public Object handleChunkLeave(Long2ObjectMap<ObjectSet<ServerPlayerEntity>> instance, long l) {
        ObjectSet<ServerPlayerEntity> objectSet = instance.get(l);
        if (objectSet == null)
            return ObjectSets.emptySet();
        else
            return objectSet;
    }
}
