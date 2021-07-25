package me.steinborn.krypton.mod.shared.network.compression;

import com.velocitypowered.natives.compression.VelocityCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

public class MinecraftCompressDecoder extends ByteToMessageDecoder {

  private static final int UNCOMPRESSED_CAP = 8 * 1024 * 1024; // 8MiB

  private int threshold;
  private final boolean validate;
  private final VelocityCompressor compressor;

  public MinecraftCompressDecoder(int threshold, boolean validate, VelocityCompressor compressor) {
    this.threshold = threshold;
    this.validate = validate;
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
        if (validate) {
          if (claimedUncompressedSize < this.threshold) {
            throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is below server threshold of " + this.threshold);
          }

          if (claimedUncompressedSize > UNCOMPRESSED_CAP) {
            throw new DecoderException("Badly compressed packet - size of " + claimedUncompressedSize + " is larger than maximum of " + UNCOMPRESSED_CAP);
          }
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

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
    compressor.close();
  }
}
