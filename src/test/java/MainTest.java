import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void removesZeroAfterFiveOnes() {
        assertEquals("111111", Main.destuffBits("1111101"));
    }

    @Test
    void goodChecksum() {
        assertTrue(Main.checksumOk(new byte[]{72, 105, (byte) 177})); // 72 + 105 = 177
    }

    @Test
    void badChecksum() {
        assertFalse(Main.checksumOk(new byte[]{72, 106, (byte) 177}));
    }
}