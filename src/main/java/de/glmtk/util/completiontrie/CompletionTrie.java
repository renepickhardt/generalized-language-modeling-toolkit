package de.glmtk.util.completiontrie;

import static java.util.Collections.emptyIterator;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import de.glmtk.util.StringUtils;


/**
 * <p>
 * A Node consists of:
 * <ul>
 * <li>Character sequence
 * <li>Score
 * <li>Last Sibling?
 * <li>Pointer to first child
 * </ul>
 *
 * <p>
 * If a node has a next sibling is just the next node.
 *
 * <p>
 * Leaf node when pointer to first child is zero.
 *
 * <p>
 * Header (1 byte):
 * <table>
 * <tr>
 * <td>3 bits
 * <td>Length of char sequence in bytes (max char sequence length: 7 bytes)
 * <tr>
 * <td>1 bit
 * <td>Is last sibling?
 * <tr>
 * <td>2 bits
 * <td>Number of bytes for score (<code>00 = 0, 01 = 1, 10 = 2, 11 = 8</code>)
 * <tr>
 * <td>2 bits
 * <td>Number of bytes for first child offset (
 * <code>00 = 1, 01 = 2, 10 = 3, 11 = 4</code>)
 * </table>
 */
public class CompletionTrie implements Iterable<CompletionTrieEntry> {
    /* package */static final int HEADER_SIZE = 1;
    /* package */static final int MAX_CHARS_SIZE = 7;
    /* package */static final int MAX_SCORE_SIZE = 8;
    /* package */static final int MAX_FIRST_CHILD_OFFSET_SIZE = 4;

    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int ROOT = 0;

    private static class Entry {
        public static final Comparator<Entry> BY_SCORE_COMPARATOR =
            new Comparator<Entry>() {
                @Override
                public int compare(Entry lhs,
                                   Entry rhs) {
                    return -Long.compare(lhs.getScore(), rhs.getScore());
                }
            };

        public byte[] chars;
        public long score;
        public int node;
        public byte[] parentChars;
        public long parentScore;

        public Entry(byte[] chars,
                     long score,
                     int node,
                     byte[] parentChars,
                     long parentScore) {
            this.chars = chars;
            this.score = score;
            this.node = node;
            this.parentChars = parentChars;
            this.parentScore = parentScore;
        }

        public byte[] getBytes() {
            return ByteUtils.concat(parentChars, chars);
        }

        public long getScore() {
            return parentScore - score;
        }

        public CompletionTrieEntry toCompletionTrieEntry() {
            return new CompletionTrieEntry(new String(getBytes()), getScore());
        }
    }

    private class CompletionTrieIterator
            implements Iterator<CompletionTrieEntry> {
        private boolean isFirstNode = true;
        private PriorityQueue<Entry> queue = new PriorityQueue<>(11, // 11 is
                                                                     // PriorityQueue.DEFAULT_INITIAL_CAPACITY
            Entry.BY_SCORE_COMPARATOR);

        public CompletionTrieIterator(Entry entry) {
            queue.add(entry);
        }

        @Override
        public boolean hasNext() {
            Entry entry = queue.peek();
            return entry != null
                && !(node_isRoot(entry.node) && node_isLeaf(entry.node));
        }

        @Override
        public CompletionTrieEntry next() {
            return nextEntry().toCompletionTrieEntry();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Entry nextEntry() {
            Entry result = null;
            do {
                Entry entry = queue.poll();
                if (entry == null) {
                    throw new NoSuchElementException();
                }
                int node = entry.node;

                if (node_isLeaf(node)) {
                    if (node_isRoot(node)) {
                        throw new NoSuchElementException();
                    }
                    result = entry;
                } else {
                    int child = node_getFirstChild(node);
                    queue.add(makeChildEntry(child, entry));
                }

                if (isFirstNode) {
                    isFirstNode = false;
                    continue;
                }

                int sibling = node_getNextSibling(node);
                if (sibling != -1) {
                    queue.add(makeSiblingEntry(sibling, entry));
                }
            } while (result == null);
            return result;
        }
    }

    private boolean caseSensitive;
    private byte[] memory;

    public CompletionTrie(byte[] memory,
                          boolean caseSensitive) {
        this.memory = memory;
        this.caseSensitive = caseSensitive;
    }

    public byte[] getMemory() {
        return memory;
    }

    public int getMemoryConsumption() {
        return memory.length;
    }

    private Entry findPath(String string,
                           boolean requireLeaf) {
        if (!caseSensitive) {
            string = string.toLowerCase();
        }

        byte[] prefixChars = string.getBytes(CHARSET);
        int curChar = 0;

        Entry entry = makeRootEntry();

        int node = ROOT, nextNode = -1;
        int nextNodeChar = 0;
        while (curChar != prefixChars.length) {
            int child = node_getFirstChild(node);
            byte[] childChars;
            while (child != -1) {
                childChars = node_getChars(child);
                int curChildChar = 0;
                while (curChar != prefixChars.length
                    && curChildChar != childChars.length
                    && prefixChars[curChar] == childChars[curChildChar]) {
                    ++curChar;
                    ++curChildChar;
                }

                if (curChildChar != 0) {
                    // We found a match.
                    nextNodeChar = curChildChar;
                    nextNode = child;
                    break;
                }

                child = node_getNextSibling(child);
            }

            if (nextNode == -1) {
                break;
            }
            if (nextNodeChar != node_getChars(nextNode).length) {
                node = nextNode;
                entry = makeChildEntry(nextNode, entry);
                break;
            }

            node = nextNode;
            entry = makeChildEntry(nextNode, entry);
            nextNode = -1;
        }

        if (curChar != prefixChars.length) {
            return null;
        }

        if (requireLeaf) {
            if (nextNode != -1) {
                return null;
            }
            if (!node_isLeaf(node)) {
                // Look if we have epsilon leaf node.
                int child = node_getFirstChild(node);
                while (child != -1) {
                    if (header_numCharsBytes(node_getHeader(child)) == 0) {
                        return makeChildEntry(child, entry);
                    }

                    child = node_getNextSibling(child);
                }
                return null;
            }
        }

        return entry;
    }

    public boolean containsPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        return findPath(prefix, false) != null;
    }

    public boolean contains(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        return findPath(string, true) != null;
    }

    public Long get(String string) {
        Entry entry = findPath(string, true);
        if (entry == null) {
            return null;
        }
        return entry.getScore();
    }

    @Override
    public Iterator<CompletionTrieEntry> iterator() {
        return new CompletionTrieIterator(makeRootEntry());
    }

    public Iterator<CompletionTrieEntry> getCompletions(String prefix) {
        if (!caseSensitive) {
            prefix = prefix.toLowerCase();
        }

        Entry entry = findPath(prefix, false);

        if (entry == null) {
            return emptyIterator();
        }
        return new CompletionTrieIterator(entry);
    }

    public List<CompletionTrieEntry> getTopKCompletions(String prefix,
                                                        int k) {
        List<CompletionTrieEntry> result = new ArrayList<>(k);
        Iterator<CompletionTrieEntry> iter = getCompletions(prefix);
        for (int i = 0; i != k && iter.hasNext(); ++i) {
            result.add(iter.next());
        }
        return result;
    }

    private Entry makeRootEntry() {
        long rootScore = node_getScoreDelta(ROOT);
        return new Entry(ByteUtils.EMPTY_BYTE_ARRAY, rootScore, ROOT,
            ByteUtils.EMPTY_BYTE_ARRAY, rootScore + rootScore);
    }

    private Entry makeChildEntry(int child,
                                 Entry parent) {
        return new Entry(node_getChars(child), node_getScoreDelta(child), child,
            parent.getBytes(), parent.getScore());
    }

    private Entry makeSiblingEntry(int sibling,
                                   Entry prevSibling) {
        return new Entry(node_getChars(sibling), node_getScoreDelta(sibling),
            sibling, prevSibling.parentChars, prevSibling.parentScore);
    }

    // NODE ////////////////////////////////////////////////////////////////////

    /* package */static byte node_maxSize() {
        return HEADER_SIZE + MAX_CHARS_SIZE + MAX_SCORE_SIZE
            + MAX_FIRST_CHILD_OFFSET_SIZE;
    }

    /* package */static byte node_potentialSize(byte[] chars,
                                                long scoreDelta,
                                                int firstChildOffset) {
        byte[] scoreDeltaBytes = ByteUtils.toByteArray(scoreDelta);
        byte[] firstChildOffsetBytes = ByteUtils.toByteArray(firstChildOffset);

        return (byte) (HEADER_SIZE + chars.length + scoreDeltaBytes.length
            + firstChildOffsetBytes.length);
    }

    /* package */static void node_create(byte[] memory,
                                         int node,
                                         boolean isLastSibling,
                                         byte[] chars,
                                         long scoreDelta,
                                         int firstChildOffset) {
        if (chars.length > MAX_CHARS_SIZE) {
            throw new IllegalArgumentException(
                "String must be short than " + MAX_CHARS_SIZE + " bytes.");
        }

        byte[] scoreDeltaBytes = ByteUtils.toByteArray(scoreDelta);
        byte[] firstChildOffsetBytes = ByteUtils.toByteArray(firstChildOffset);

        byte numCharsBytes = (byte) chars.length;
        byte numScoreDeltaBytes = (byte) scoreDeltaBytes.length;
        byte numFirstChildOffsetBytes = (byte) firstChildOffsetBytes.length;

        byte header = header_create(numCharsBytes, isLastSibling,
            numScoreDeltaBytes, numFirstChildOffsetBytes);

        memory[node] = header;

        int from = node + HEADER_SIZE;
        int length = numCharsBytes;
        System.arraycopy(chars, 0, memory, from, length);

        from += length;
        length = numScoreDeltaBytes;
        System.arraycopy(scoreDeltaBytes, 0, memory, from, length);

        from += length;
        length = numFirstChildOffsetBytes;
        System.arraycopy(firstChildOffsetBytes, 0, memory, from, length);
    }

    @Deprecated
            /* package */void node_create(int node,
                                          boolean isLastSibling,
                                          String chars,
                                          long scoreDelta,
                                          int firstChildOffset) {
        node_create(memory, node, isLastSibling, chars.getBytes(CHARSET),
            scoreDelta, firstChildOffset);
    }

    /* package */byte node_getHeader(int node) {
        return memory[node];
    }

    /**
     * Size in byte this node occupies.
     */
            /* package */byte node_getSize(int node) {
        byte header = node_getHeader(node);
        // Header + Chars + Score + FirstChildOffset
        return (byte) (HEADER_SIZE + header_numCharsBytes(header)
            + header_numScoreBytes(header)
            + header_numFirstChildOffsetBytes(header));
    }

    @Deprecated
            /* package */String node_getString(int node) {
        return new String(node_getChars(node));
    }

    /* package */byte[] node_getChars(int node) {
        byte header = node_getHeader(node);
        int from = node + HEADER_SIZE;
        int to = from + header_numCharsBytes(header);
        byte[] chars = Arrays.copyOfRange(memory, from, to);
        return chars;
    }

    /* package */long node_getScoreDelta(int node) {
        byte header = node_getHeader(node);
        int from = node + HEADER_SIZE + header_numCharsBytes(header);
        int to = from + header_numScoreBytes(header);
        byte[] score = Arrays.copyOfRange(memory, from, to);
        return ByteUtils.longFromByteArray(score);
    }

    /* package */int node_getFirstChildOffset(int node) {
        byte header = node_getHeader(node);
        int from = node + HEADER_SIZE + header_numCharsBytes(header)
            + header_numScoreBytes(header);
        int to = from + header_numFirstChildOffsetBytes(header);
        byte[] firstChildOffset = Arrays.copyOfRange(memory, from, to);
        return ByteUtils.intFromByteArray(firstChildOffset);
    }

    /* package */boolean node_isRoot(int node) {
        return node == ROOT;
    }

    /* package */boolean node_isLeaf(int node) {
        return node_getFirstChildOffset(node) == 0;
    }

    /* package */boolean node_isLastSibling(int node) {
        byte header = node_getHeader(node);
        return header_isLastSibling(header);
    }

    /* package */int node_getFirstChild(int node) {
        if (node_isLeaf(node)) {
            return -1;
        }
        return node + node_getFirstChildOffset(node);
    }

    /* package */int node_getNextSibling(int node) {
        if (node_isLastSibling(node)) {
            return -1;
        }
        return node + node_getSize(node);
    }

    /* package */String node_toString(int node) {
        StringBuilder builder = new StringBuilder();
        builder.append("---- node: ").append(Integer.toString(node))
            .append('\n');
        builder.append("-- size: ").append(node_getSize(node)).append('\n');
        builder.append(header_toString(memory[node]));
        builder.append("-- chars: \"").append(node_getString(node))
            .append("\"\n");
        builder.append("-- scoreDelta: ").append(node_getScoreDelta(node))
            .append('\n');
        builder.append("-- firstChildOffset: ")
            .append(node_getFirstChildOffset(node)).append('\n');
        return builder.toString();
    }

    // HEADER //////////////////////////////////////////////////////////////////

    private static final byte HEADER_NUM_CHARS_BYTES_OFFSET = (byte) 5;
    private static final byte HEADER_NUM_SCORE_BYTES_OFFSET = (byte) 2;
    private static final byte HEADER_FSTCHILD_OFFSET_OFFSET = (byte) 0;

    private static final byte HEADER_NUM_CHARS_BYTES_MASK = (byte) 0x07;
    private static final byte HEADER_IS_LAST_SIBLING_MASK = (byte) 0x10;
    private static final byte HEADER_NUM_SCORE_BYTES_MASK = (byte) 0x03;
    private static final byte HEADER_FSTCHILD_OFFSET_MASK = (byte) 0x03;

    /* package */static byte header_create(byte numCharsBytes,
                                           boolean isLastSibling,
                                           byte numScoreBytes,
                                           byte numFirstChildOffsetBytes) {
        if (numCharsBytes < 0 || numCharsBytes > 7) {
            throw new IllegalArgumentException(
                "numCharsBytes needs to be one of 0,1,2,3,4,5,6,7.");
        }
        if (!(numScoreBytes == 0 || numScoreBytes == 1 || numScoreBytes == 2
            || numScoreBytes == 8)) {
            throw new IllegalArgumentException(
                "numScoreBytes needs to be one of 0,1,2,8, was: "
                    + numScoreBytes + ".");
        }
        if (!(numFirstChildOffsetBytes == 0 || numFirstChildOffsetBytes == 1
            || numFirstChildOffsetBytes == 2
            || numFirstChildOffsetBytes == 4)) {
            throw new IllegalArgumentException(
                "numFirstChildOffsetBytes needs to be one of 0,1,2,4.");
        }

        if (numScoreBytes == 8) {
            numScoreBytes = 3;
        }
        if (numFirstChildOffsetBytes == 4) {
            numFirstChildOffsetBytes = 3;
        }

        byte result = 0;
        result |= numCharsBytes << HEADER_NUM_CHARS_BYTES_OFFSET;
        if (isLastSibling) {
            result |= HEADER_IS_LAST_SIBLING_MASK;
        }
        result |= numScoreBytes << HEADER_NUM_SCORE_BYTES_OFFSET;
        result |= numFirstChildOffsetBytes << HEADER_FSTCHILD_OFFSET_OFFSET;
        return result;
    }

    /* package */static byte header_numCharsBytes(byte header) {
        return (byte) ((header >>> HEADER_NUM_CHARS_BYTES_OFFSET)
            & HEADER_NUM_CHARS_BYTES_MASK);
    }

    /* package */static boolean header_isLastSibling(byte header) {
        return (header & HEADER_IS_LAST_SIBLING_MASK) != 0;
    }

    /* package */static byte header_numScoreBytes(byte header) {
        byte numScoreBytes = (byte) ((header >>> HEADER_NUM_SCORE_BYTES_OFFSET)
            & HEADER_NUM_SCORE_BYTES_MASK);
        return numScoreBytes == 3 ? 8 : numScoreBytes;
    }

    /* package */static byte header_numFirstChildOffsetBytes(byte header) {
        byte numFirstChildOffsetBytes =
            (byte) ((header >>> HEADER_FSTCHILD_OFFSET_OFFSET)
                & HEADER_FSTCHILD_OFFSET_MASK);
        return numFirstChildOffsetBytes == 3 ? 4 : numFirstChildOffsetBytes;
    }

    /* package */static String header_toString(byte header) {
        StringBuilder builder = new StringBuilder();
        builder.append("-- header: ")
            .append(Integer.toBinaryString(header & 0xFF)).append('\n');
        builder.append("numCharsBytes            = ")
            .append(header_numCharsBytes(header)).append('\n');
        builder.append("isLastSibling            = ")
            .append(header_isLastSibling(header)).append('\n');
        builder.append("numScoreBytes            = ")
            .append(header_numScoreBytes(header)).append('\n');
        builder.append("numFirstChildOffsetBytes = ")
            .append(header_numFirstChildOffsetBytes(header)).append('\n');
        return builder.toString();
    }

    // PRINT ///////////////////////////////////////////////////////////////////

    /**
     * Requires GraphViz and Feh.
     */
    public void printDot(Writer writer) throws IOException {
        writer.append("graph CompletionTrie {\n");

        writer.append("\n// Nodes\n");
        printDot_nodes(writer, ROOT, 0);

        writer.append("\n// Edges\n");
        printDot_edges(writer, ROOT, 0);

        writer.append("\n}\n");
    }

    private String printDot_getNodeChars(int node) {
        String chars = node_getString(node);
        if (chars.isEmpty()) {
            return "ϵ";
        }
        return StringUtils.replaceAll(chars, " ", "_");
    }

    private String printDot_getNodeId(int node) {
        return printDot_getNodeChars(node) + "-" + Integer.toHexString(node);
    }

    private void printDot_nodes(Writer writer,
                                int node,
                                int depth) throws IOException {
        writer.append(StringUtils.repeat("  ", depth));
        writer.append('"').append(printDot_getNodeId(node)).append('"');
        writer.append(" [ label=\"").append(printDot_getNodeChars(node))
            .append("\" ];\n");

        int child = node_getFirstChild(node);
        while (child != -1) {
            printDot_nodes(writer, child, depth + 1);
            child = node_getNextSibling(child);
        }
    }

    private void printDot_edges(Writer writer,
                                int node,
                                int depth) throws IOException {
        String tmp = StringUtils.repeat("  ", depth) + '"'
            + printDot_getNodeId(node) + "\" -- \"";

        int child = node_getFirstChild(node);
        while (child != -1) {
            writer.append(tmp).append(printDot_getNodeId(child)).append('\"');
            writer.append(" [ label=\"")
                .append(Long.toString(node_getScoreDelta(child)))
                .append("\" ];\n");
            printDot_edges(writer, child, depth + 1);
            child = node_getNextSibling(child);
        }
    }
}
