package me.steinborn.krypton.mod.shared.player;


public interface KryptonServerPlayerEntity {
    void setUpdatedViewDistance(boolean updatedViewDistance);
    
    int getPlayerViewDistance();
    
    boolean isUpdatedViewDistance();
}
