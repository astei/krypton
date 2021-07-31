package me.steinborn.krypton.mixin.shared.network.microopt;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.world.EntityTrackingListener;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
public class TacsTrackedEntityMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newIdentityHashSet()Ljava/util/Set;"))
    public Set<EntityTrackingListener> construct$useFastutil() {
        // Change from Tuinity.
        //
        // The fastutil ReferenceOpenHashSet has significantly better performance characteristics than the JDK
        // IdentityHashMap wrapped as a set.
        return new ReferenceOpenHashSet<>();
    }
}
