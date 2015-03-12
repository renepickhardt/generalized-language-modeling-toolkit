package de.glmtk.util.completiontrie;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import de.glmtk.util.StringUtils;

public class CompletionTrieBuilder {
    private static class Node {
        public byte[] chars = null;
        public long score = 0;
        public Node parent = null;
        public NavigableSet<Node> childs = new TreeSet<>(NODE_SCORE_COMPARATOR);

        // used only during build()
        public long scoreDelta = 0;
        public boolean isLastSibling = false;
        public int layer = 0;
        public int firstChildPointer = 0;

        @Override
        public String toString() {
            StringBuilder a = new StringBuilder("Node "
                    + System.identityHashCode(this) + "[chars=\""
                    + new String(chars) + "\", score=" + score + ", childs=");
            for (Node child : childs)
                a.append(System.identityHashCode(child)).append(',');
            a.append(", parent=" + System.identityHashCode(parent)
                    + ", scoreDelta=" + scoreDelta + ", isLastSibling="
                    + isLastSibling + ", layer=" + layer
                    + ", firstChildOffset=" + firstChildPointer + "]");
            return a.toString();
        }
    }

    private static final Comparator<Node> NODE_SCORE_COMPARATOR = new Comparator<Node>() {
        @Override
        public int compare(Node lhs,
                           Node rhs) {
            int cmp = -Long.compare(lhs.score, rhs.score);
            if (cmp != 0)
                return cmp;
            return ByteUtils.compare(lhs.chars, rhs.chars);
        }
    };

    private static final Comparator<Node> NODE_BUILD_COMPARATOR = new Comparator<Node>() {
        @Override
        public int compare(Node lhs,
                           Node rhs) {
            if (lhs == rhs)
                return 0;

            int cmp = -Integer.compare(lhs.layer, rhs.layer);
            if (cmp != 0)
                return cmp;

            cmp = -compare(lhs.parent, rhs.parent);
            if (cmp != 0)
                return cmp;

            cmp = Long.compare(lhs.score, rhs.score);
            if (cmp != 0)
                return cmp;

            return -ByteUtils.compare(lhs.chars, rhs.chars);
        }
    };

    private boolean caseSensitive;
    private Node root;
    private List<Node> nodes;

    public CompletionTrieBuilder(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        reset();
    }

    public void reset() {
        root = new Node();
        root.chars = new byte[0];
        root.score = 0L;
        root.parent = null;
        nodes = new ArrayList<>();
        nodes.add(root);
    }

    /**
     * The scenario of duplicate key insertion is detected and an exception is
     * thrown but calculated score value may be compromised.
     */
    public void add(String string,
                    long score) {
        Objects.requireNonNull(string, "Can't add null string.");
        if (string.isEmpty())
            throw new IllegalArgumentException("Can't add empty string.");

        if (!caseSensitive)
            string = string.toLowerCase();

        byte[] chars = string.getBytes(Charset.forName("UTF-8"));
        int curChar = 0;

        if (root.score < score)
            root.score = score;

        Node node = root, nextNode = null;
        long nodeScore = node.score;
        int nextNodeChar = 0;
        while (curChar != chars.length) {
            for (Node child : node.childs) {
                int curChildChar = 0;
                while (curChar != chars.length
                        && curChildChar != child.chars.length
                        && chars[curChar] == child.chars[curChildChar]) {
                    ++curChar;
                    ++curChildChar;
                }

                if (curChildChar != 0) {
                    // We found a match.
                    nextNodeChar = curChildChar;
                    nextNode = child;
                    nodeScore = child.score;

                    if (child.score < score) {
                        node.childs.remove(child);
                        child.score = score;
                        node.childs.add(child);
                    }
                    break;
                }
            }

            if (nextNode == null)
                break;
            if (nextNodeChar != nextNode.chars.length) {
                node = nextNode;
                break;
            }

            node = nextNode;
            nextNode = null;
        }

        if (nextNodeChar != 0 && nextNodeChar != node.chars.length) {
            // We need to split node
            Node splitNode = new Node();
            splitNode.chars = Arrays.copyOfRange(node.chars, nextNodeChar,
                    node.chars.length);
            splitNode.score = nodeScore;
            splitNode.parent = node;
            splitNode.childs = node.childs;
            for (Node child : splitNode.childs)
                child.parent = splitNode;
            nodes.add(splitNode);

            node.childs = new TreeSet<>(NODE_SCORE_COMPARATOR);
            node.childs.add(splitNode);
            node.chars = Arrays.copyOf(node.chars, nextNodeChar);
        }

        if (node != root) {
            // Do we need to add an epsilon node to retain this string as leaf?
            boolean isLeaf = node.childs.isEmpty();
            // Do we need to add an epsilon node to mark this node as leaf?
            boolean noCharsLeft = curChar == chars.length;

            if (isLeaf && noCharsLeft && nodeScore != score)
                // TODO: update instead
                // if nodeScore > score, we don't have to to anything
                // if nodeSocre < score, we just have to walk up parent path,
                // and recalc scores.
                throw new IllegalStateException(
                        "Attempted string to add already present with different score: '"
                                + string + "'.");

            if (isLeaf || noCharsLeft) {
                Node epsNode = new Node();
                epsNode.chars = new byte[0];
                epsNode.score = isLeaf ? nodeScore : score;
                epsNode.parent = node;
                node.childs.add(epsNode);
                nodes.add(epsNode);
            }
        }

        while (curChar != chars.length) {
            Node newNode = new Node();
            int newNodeCharsLength = Math.min(chars.length - curChar,
                    CompletionTrie.MAX_CHARS_SIZE);
            newNode.chars = Arrays.copyOfRange(chars, curChar, curChar
                    + newNodeCharsLength);
            curChar += newNodeCharsLength;
            newNode.score = score;
            newNode.parent = node;
            node.childs.add(newNode);
            nodes.add(newNode);
            node = newNode;
        }
    }

    private byte calcNodeSize(Node node,
                              int pointer) {
        return CompletionTrie.node_potentialSize(node.chars, node.scoreDelta,
                node.firstChildPointer == 0 ? 0 : node.firstChildPointer
                        - pointer);
    }

    public CompletionTrie build() {
        updateAuxiliaryAttributes();
        Collections.sort(nodes, NODE_BUILD_COMPARATOR);

        int maxMemory = nodes.size() * CompletionTrie.node_maxSize();
        byte[] memory = new byte[maxMemory];
        int pointer = maxMemory;

        for (Node node : nodes) {
            int nodeSize = calcNodeSize(node, pointer);
            // Recalculate size as with the new offset the child offset might
            // need one more byte
            nodeSize = calcNodeSize(node, pointer - nodeSize);

            pointer -= nodeSize;

            int firstChildOffset = 0;
            if (node.firstChildPointer != 0)
                firstChildOffset = node.firstChildPointer - pointer;

            CompletionTrie.node_create(memory, pointer, node.isLastSibling,
                    node.chars, node.scoreDelta, firstChildOffset);

            if (node.parent != null)
                node.parent.firstChildPointer = pointer;
        }

        memory = Arrays.copyOfRange(memory, pointer, maxMemory);

        return new CompletionTrie(memory, caseSensitive);
    }

    private void updateAuxiliaryAttributes() {
        updateAuxiliaryAttributes(root, 2 * root.score, true, 0);
    }

    /**
     * Set {@link Node#scoreDelta}, {@link Node#isLastSibling} and
     * {@link Node#layer} for a node and all its children recursively.
     */
    private void updateAuxiliaryAttributes(Node node,
                                           long parentScore,
                                           boolean isLastSibling,
                                           int trieLayer) {
        node.scoreDelta = parentScore - node.score;
        node.isLastSibling = isLastSibling;
        node.layer = trieLayer;

        if (node.childs.isEmpty())
            return;
        parentScore = node.score;
        ++trieLayer;
        Iterator<Node> childIter = node.childs.iterator();
        while (true) {
            Node child = childIter.next();
            if (childIter.hasNext())
                updateAuxiliaryAttributes(child, parentScore, false, trieLayer);
            else {
                updateAuxiliaryAttributes(child, parentScore, true, trieLayer);
                break;
            }
        }
    }

    // PRINT ///////////////////////////////////////////////////////////////////

    /**
     * Prints GraphViz DOT fromat.
     */
    public void printDot(Writer writer) throws IOException {
        updateAuxiliaryAttributes();

        writer.append("graph CompletionTrieBuilder {\n");

        writer.append("\n// Nodes\n");
        printDot_nodes(writer, root, 0);

        writer.append("\n// Edges\n");
        printDot_edges(writer, root, 0);

        writer.append("\n}\n");
    }

    private String printDot_getNodeChars(Node node) {
        if (node == null)
            return "null";
        if (node.chars.length == 0)
            return "ϵ";
        return StringUtils.replaceAll(new String(node.chars), " ", "_");
    }

    private String printDot_getNodeId(Node node) {
        return printDot_getNodeChars(node) + "-"
                + Integer.toHexString(System.identityHashCode(node));
    }

    private void printDot_nodes(Writer writer,
                                Node node,
                                int depth) throws IOException {
        writer.append(StringUtils.repeat("  ", depth));
        writer.append('"').append(printDot_getNodeId(node)).append('"');
        writer.append(" [ label=\"").append(printDot_getNodeChars(node));
        //        writer.append(" [" + printDot_getNodeChars(node.parent) + ", "
        //                + node.scoreDelta + ", " + node.layer + ", "
        //                + node.firstChildOffset + ", " + node.isLastSibling + "]");
        writer.append("\" ];\n");

        for (Node child : node.childs)
            printDot_nodes(writer, child, depth + 1);
    }

    private void printDot_edges(Writer writer,
                                Node node,
                                int depth) throws IOException {
        String tmp = StringUtils.repeat("  ", depth) + '"'
                + printDot_getNodeId(node) + "\" -- \"";

        for (Node child : node.childs) {
            writer.append(tmp).append(printDot_getNodeId(child)).append('\"');
            writer.append(" [ label=\"").append(Long.toString(child.scoreDelta)).append(
                    "\" ];\n");
            printDot_edges(writer, child, depth + 1);
        }
    }
}
