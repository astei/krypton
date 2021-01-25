package me.steinborn.krypton.mod.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

public class MinecraftCompressDecoder extends ByteToMessageDecoder {

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
    if (in.readableBytes() != 0) {
      PacketByteBuf packetBuf = new PacketByteBuf(in);
      int claimedUncompressedSize = packetBuf.readVarInt();

      if (claimedUncompressedSize == 0) {
        out.add(packetBuf.readBytes(packetBuf.readableBytes()));
      } else {
        if (claimedUncompressedSize < this.threshold) {
          throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is below server threshold of " + this.threshold);
        }

        if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
          throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than maximum of " + UNCOMPRESSED_CAP);
        }

        ByteBuf compatibleIn = com.velocitypowered.natives.util.MoreByteBufUtils.ensureCompatible(ctx.alloc(), compressor, in);
        ByteBuf uncompressed = com.velocitypowered.natives.util.MoreByteBufUtils.preferredBuffer(ctx.alloc(), compressor, claimedUncompressedSize);
        try {
          compressor.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
          out.add(uncompressed);
          in.clear();
        } catch (Exception e) {
          uncompressed.release();
          throw e;
        } finally {
          compatibleIn.release();
        }
      }

    }
  }

  @Override
  public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }
}
