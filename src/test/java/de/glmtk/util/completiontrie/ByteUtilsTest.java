package de.glmtk.util.completiontrie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ByteUtilsTest {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        int numElems = 63;
        List<Object[]> data = new ArrayList<>(2 * numElems + 1);
        data.add(new Object[] { Long.valueOf(0L) });
        long val = 1;
        for (int i = 0; i != numElems; ++i) {
            data.add(new Object[] { Long.valueOf(val) });
            data.add(new Object[] { Long.valueOf(-val) });
            val *= 2;
        }
        return data;
    }

    private long val;

    public ByteUtilsTest(long val) {
        this.val = val;
    }

    @Test
    public void testIntConversion() {
        assumeTrue(val == (int) val);
        byte[] bytes = ByteUtils.toByteArray((int) val);
        int newVal = ByteUtils.intFromByteArray(bytes);
        assertEquals(val, newVal);
    }

    @Test
    public void testLongConversion() {
        byte[] bytes = ByteUtils.toByteArray(val);
        long newVal = ByteUtils.longFromByteArray(bytes);
        assertEquals(val, newVal);
    }
}
