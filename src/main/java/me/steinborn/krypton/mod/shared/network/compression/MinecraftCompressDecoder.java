package me.steinborn.krypton.mod.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

import static com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible;
import static com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer;

public class MinecraftCompressDecoder extends MessageToMessageDecoder<ByteBuf> {

  private static final int VANILLA_MAXIMUM_UNCOMPRESSED_SIZE = 2 * 1024 * 1024; // 2MiB
  private static final int HARD_MAXIMUM_UNCOMPRESSED_SIZE = 16 * 1024 * 1024; // 16MiB

  private static final int UNCOMPRESSED_CAP =
      Boolean.getBoolean("velocity.increased-compression-cap")
          ? HARD_MAXIMUM_UNCOMPRESSED_SIZE : VANILLA_MAXIMUM_UNCOMPRESSED_SIZE;

  private final int threshold;
  private final VelocityCompressor compressor;

  public MinecraftCompressDecoder(int threshold, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.compressor = compressor;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    PacketByteBuf wrappedBuf = new PacketByteBuf(in);
    int claimedUncompressedSize = wrappedBuf.readVarInt();
    if (claimedUncompressedSize == 0) {
      // This message is not compressed.
      out.add(in.retainedSlice());
      return;
    }

    if (claimedUncompressedSize < threshold) {
      throw new CorruptedFrameException("Uncompressed size " + claimedUncompressedSize + " is less than"
              + " threshold " + threshold);
    }
    if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
      throw new CorruptedFrameException("Uncompressed size " + claimedUncompressedSize + " exceeds hard " +
              "threshold of " + UNCOMPRESSED_CAP);
    }

    ByteBuf compatibleIn = ensureCompatible(ctx.alloc(), compressor, in);
    ByteBuf uncompressed = preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
    try {
      compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
      out.add(uncompressed);
    } catch (Exception e) {
      uncompressed.release();
      throw e;
    } finally {
      compatibleIn.release();
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }
}
