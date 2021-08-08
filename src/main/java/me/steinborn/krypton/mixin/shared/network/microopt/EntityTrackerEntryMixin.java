package me.steinborn.krypton.mixin.shared.network.microopt;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Collections;emptyList()Ljava/util/List;"))
    public List<Entity> construct$initialPassengersListIsGuavaImmutableList() {
        // This is a tiny micro-optimization, but in most cases, the passengers list for an entity is typically empty.
        // Furthermore, it is using Guava's ImmutableList type, but the constructor uses the JDK (pre-Java 9) empty
        // collections. By using Guava's collection type here, this check can often be simplified to a simple reference
        // equality check, which is very cheap.
        return ImmutableList.of();
    }
}
