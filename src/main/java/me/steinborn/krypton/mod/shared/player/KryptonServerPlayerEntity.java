package me.steinborn.krypton.mod.shared.player;


public interface KryptonServerPlayerEntity {
    void setNeedsChunksReloaded(boolean needsChunksReloaded);

    int getPlayerViewDistance();

    boolean getNeedsChunksReloaded();
}
