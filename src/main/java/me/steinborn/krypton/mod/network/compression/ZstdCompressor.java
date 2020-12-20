package me.steinborn.krypton.mod.network.compression;

import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class ZstdCompressor implements CompressionAlgorithm {

    @Override
    public VelocityCompressor createCompressor(int level) {
        return new CompressorImpl(level);
    }

    private static class CompressorImpl implements VelocityCompressor {
        private final ZstdCompressCtx compressCtx;
        private final ZstdDecompressCtx decompressCtx;

        private CompressorImpl(int level) {
            this.compressCtx = new ZstdCompressCtx();
            this.decompressCtx = new ZstdDecompressCtx();

            this.compressCtx.setLevel(level);
        }

        @Override
        public void inflate(ByteBuf in, ByteBuf out, int expectedSize) throws DataFormatException {
            out.ensureWritable(expectedSize);

            int decompressed = this.decompressCtx.decompress(out.nioBuffer(out.writerIndex(), out.writableBytes()),
                    in.nioBuffer());
            if (decompressed != expectedSize) {
                throw new IllegalStateException("Got an invalid size; decompressed " + decompressed + " but needed " + expectedSize);
            }
            out.writerIndex(out.writerIndex() + decompressed);
        }

        @Override
        public void deflate(ByteBuf in, ByteBuf out) throws DataFormatException {
            ByteBuffer outAsNio = out.nioBuffer(out.writerIndex(), out.writableBytes());
            int compressed = this.compressCtx.compress(outAsNio, in.nioBuffer());
            in.skipBytes(in.readableBytes());
            out.writerIndex(out.writerIndex() + compressed);
        }

        @Override
        public void close() {
            this.compressCtx.close();
            this.decompressCtx.close();
        }

        @Override
        public BufferPreference preferredBufferType() {
            return BufferPreference.DIRECT_REQUIRED;
        }
    }
}
