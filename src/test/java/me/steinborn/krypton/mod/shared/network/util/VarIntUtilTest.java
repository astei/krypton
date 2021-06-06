package me.steinborn.krypton.mod.shared.network.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VarIntUtilTest {

    private static int vanillaGetVarLongLength(long value) {
        for(int i = 1; i < 10; ++i) {
            if ((value & -1L << i * 7) == 0L) {
                return i;
            }
        }

        return 10;
    }

    @Test
    void ensureConsistencyAcrossNumberBitsInt() {
        for (int i = 0; i <= 31; i++) {
            int number = (1 << i) - 1;
            assertEquals(vanillaGetVarLongLength(number), VarIntUtil.getVarIntLength(number),
                    "mismatch with " + i + "-bit number");
        }
    }
}