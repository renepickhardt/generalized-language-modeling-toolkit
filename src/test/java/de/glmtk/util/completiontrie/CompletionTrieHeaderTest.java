package de.glmtk.util.completiontrie;

import static de.glmtk.util.completiontrie.CompletionTrie.header_create;
import static de.glmtk.util.completiontrie.CompletionTrie.header_isLastSibling;
import static de.glmtk.util.completiontrie.CompletionTrie.header_numCharsBytes;
import static de.glmtk.util.completiontrie.CompletionTrie.header_numFirstChildOffsetBytes;
import static de.glmtk.util.completiontrie.CompletionTrie.header_numScoreBytes;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class CompletionTrieHeaderTest {
    @Parameters(name = "numCharsBytes={0}, isLastSibling={1}, numScoreBytes={2}, numFirstChildOffsetBytes={3}")
    public static Iterable<Object[]> data() {
        byte[] numCharsBytesValues = { 0, 1, 2, 3, 4, 5, 6, 7 };
        boolean[] isLastSiblingValues = { false, true };
        byte[] numScoreBytesValues = { 0, 1, 2, 8 };
        byte[] numFirstChildOffsetBytesValue = { 0, 1, 2, 4 };

        int numPermutations = numCharsBytesValues.length
            * isLastSiblingValues.length * numScoreBytesValues.length
            * numFirstChildOffsetBytesValue.length;

        List<Object[]> data = new ArrayList<>(numPermutations);
        for (Byte numCharsBytes : numCharsBytesValues) {
            for (Boolean isLastSibling : isLastSiblingValues) {
                for (Byte numScoreBytes : numScoreBytesValues) {
                    for (Byte numFirstChildOffsetBytes : numFirstChildOffsetBytesValue) {
                        data.add(new Object[] { numCharsBytes, isLastSibling,
                            numScoreBytes, numFirstChildOffsetBytes });
                    }
                }
            }
        }

        return data;
    }

    private byte numCharsBytes;
    private boolean isLastSibling;
    private byte numScoreBytes;
    private byte numFirstChildOffsetBytes;
    private byte header;

    public CompletionTrieHeaderTest(byte numCharsBytes,
                                    boolean isLastSibling,
                                    byte numScoreBytes,
                                    byte numFirstChildOffsetBytes) {
        this.numCharsBytes = numCharsBytes;
        this.isLastSibling = isLastSibling;
        this.numScoreBytes = numScoreBytes;
        this.numFirstChildOffsetBytes = numFirstChildOffsetBytes;
        header = header_create(numCharsBytes, isLastSibling, numScoreBytes,
            numFirstChildOffsetBytes);
    }

    @Test
    public void testNumCharsBytes() {
        assertEquals(numCharsBytes, header_numCharsBytes(header));
    }

    @Test
    public void testIsLastSibling() {
        assertEquals(isLastSibling, header_isLastSibling(header));
    }

    @Test
    public void testNumScoreBytes() {
        assertEquals(numScoreBytes, header_numScoreBytes(header));
    }

    @Test
    public void testnumFirstChildOffsetBytes() {
        assertEquals(numFirstChildOffsetBytes,
            header_numFirstChildOffsetBytes(header));
    }
}
