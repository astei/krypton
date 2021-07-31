package me.steinborn.krypton.mixin.shared.fastchunkentityaccess;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import me.steinborn.krypton.mod.shared.WorldEntityByChunkAccess;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a fast way to search the section cache for entities present in a given chunk.
 */
@Mixin(SectionedEntityCache.class)
public abstract class SectionedEntityCacheMixin implements WorldEntityByChunkAccess {

    @Shadow @Final private Long2ObjectMap<EntityTrackingSection<Entity>> trackingSections;

    @Override
    public Collection<Entity> getEntitiesInChunk(final int chunkX, final int chunkZ) {
        final LongSortedSet set = this.getSections(chunkX, chunkZ);
        if (set.isEmpty()) {
            // Nothing in this map?
            return List.of();
        }

        final List<Entity> entities = new ArrayList<>();
        final LongIterator sectionsIterator = set.iterator();
        while (sectionsIterator.hasNext()) {
            final long key = sectionsIterator.nextLong();
            final EntityTrackingSection<Entity> value = this.trackingSections.get(key);
            if (value != null && value.getStatus().shouldTrack()) {
                entities.addAll(((EntityTrackingSectionAccessor<Entity>) value).getCollection());
            }
        }

        return entities;
    }

    @Shadow protected abstract LongSortedSet getSections(int chunkX, int chunkZ);
}
