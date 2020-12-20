package me.steinborn.krypton.mod.network.compression;

import com.velocitypowered.natives.util.Natives;

public class CompressionNegotiation {
    /**
     * The canonical compression algorithm used by the Minecraft protocol.
     */
    public static final CompressionAlgorithm ZLIB = level -> Natives.compress.get().create(level);

    /**
     * The zstandard compression algorithm. Use a low compression level for best results.
     */
    public static final CompressionAlgorithm ZSTD = ZLIB; //new ZstdCompressor();
}
