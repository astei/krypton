package me.steinborn.krypton.mod.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;

public interface CompressionAlgorithm {
    VelocityCompressor createCompressor(int level);
}
