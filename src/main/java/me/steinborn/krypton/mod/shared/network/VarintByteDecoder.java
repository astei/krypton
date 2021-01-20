package me.steinborn.krypton.mod.shared.network;

import io.netty.util.ByteProcessor;

public class VarintByteDecoder implements ByteProcessor {
    private int readVarint;
    private int bytesRead;
    private DecodeResult result = DecodeResult.TOO_SHORT;

    @Override
    public boolean process(byte k) {
        readVarint |= (k & 0x7F) << bytesRead++ * 7;
        if (bytesRead > 3) {
            result = DecodeResult.TOO_BIG;
            return false;
        }
        if ((k & 0x80) != 128) {
            result = DecodeResult.SUCCESS;
            return false;
        }
        return true;
    }

    public int readVarint() {
        return readVarint;
    }

    public int varintBytes() {
        return bytesRead;
    }

    public DecodeResult getResult() {
        return result;
    }

    public void reset() {
        readVarint = 0;
        bytesRead = 0;
        result = DecodeResult.TOO_SHORT;
    }

    public enum DecodeResult {
        SUCCESS,
        TOO_SHORT,
        TOO_BIG
    }
}
