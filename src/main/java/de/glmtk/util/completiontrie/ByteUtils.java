package de.glmtk.util.completiontrie;

public class ByteUtils {
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static byte[] concat(byte[] left,
                                byte[] right) {
        byte[] result = new byte[left.length + right.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    public static int compare(byte[] left,
                              byte[] right) {
        for (int i = 0, j = 0; i != left.length && j != right.length; ++i, ++j) {
            byte a = (byte) (left[i] & 0xFF);
            byte b = (byte) (right[j] & 0xFF);
            if (a != b)
                return a - b;
        }
        return left.length - right.length;
    }

    public static byte[] toByteArray(int val) {
        if (val == 0)
            return EMPTY_BYTE_ARRAY;
        if ((val & 0xFFFF0000) != 0)
            return new byte[] {(byte) (val & 0xFF),
                (byte) ((val >>> 8) & 0xFF), (byte) ((val >>> 16) & 0xFF),
                (byte) ((val >>> 24) & 0xFF)};
        if ((val & 0x0000FF00) != 0)
            return new byte[] {(byte) (val & 0xFF), (byte) ((val >>> 8) & 0xFF)};
        return new byte[] {(byte) (val & 0xFF)};
    }

    public static byte[] toByteArray(long val) {
        if (val == 0)
            return EMPTY_BYTE_ARRAY;
        if ((val & 0xFFFFFFFFFFFF0000L) != 0)
            return new byte[] {(byte) (val & 0xFF),
                (byte) ((val >>> 8) & 0xFF), (byte) ((val >>> 16) & 0xFF),
                (byte) ((val >>> 24) & 0xFF), (byte) ((val >>> 32) & 0xFF),
                (byte) ((val >>> 40) & 0xFF), (byte) ((val >>> 48) & 0xFF),
                (byte) ((val >>> 56) & 0xFF)};
        if ((val & 0x000000000000FF00L) != 0)
            return new byte[] {(byte) (val & 0xFF), (byte) ((val >>> 8) & 0xFF)};
        return new byte[] {(byte) (val & 0xFF)};
    }

    @SuppressWarnings("fallthrough")
    public static int intFromByteArray(byte[] bytes) {
        int result = 0;
        //@formatter:off
        switch (bytes.length) {
            case 4: result |= (bytes[3] & 0xFF) << 24;
            case 3: result |= (bytes[2] & 0xFF) << 16;
            case 2: result |= (bytes[1] & 0xFF) << 8;
            case 1: result |= (bytes[0] & 0xFF);
            default:
        }
        //@formatter:on
        return result;
    }

    @SuppressWarnings("fallthrough")
    public static long longFromByteArray(byte[] bytes) {
        long result = 0L;
        //@formatter:off
        switch (bytes.length) {
            case 8: result |= (long) (bytes[7] & 0xFF) << 56;
            case 7: result |= (long) (bytes[6] & 0xFF) << 48;
            case 6: result |= (long) (bytes[5] & 0xFF) << 40;
            case 5: result |= (long) (bytes[4] & 0xFF) << 32;
            case 4: result |= (long) (bytes[3] & 0xFF) << 24;
            case 3: result |= (long) (bytes[2] & 0xFF) << 16;
            case 2: result |= (long) (bytes[1] & 0xFF) << 8;
            case 1: result |=        (bytes[0] & 0xFF);
            default:
        }
        //@formatter:on
        return result;
    }
}
