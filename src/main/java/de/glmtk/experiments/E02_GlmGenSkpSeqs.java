package de.glmtk.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.glmtk.utils.StringUtils;

/**
 * 2014-12-22
 * Trying to efficiently generate unique skipped sequences per order for
 * recursive calls of GLM.
 *
 * <p>
 * Result: Generating all unique skipped sequences is the same as generating
 * permutations of token and skp, where token is dependent on position but
 * counts as a duplicate concerning permutation. An efficient implenetation is
 * <a href="http://marknelson.us/2002/03/01/next-permutation/">
 * std::next_permutation()</a>
 */
public class E02_GlmGenSkpSeqs {

    private static void swap(boolean[] array, int i, int j) {
        boolean tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    private static void reverse(boolean[] array, int from, int to) {
        for (int i = from; i != from + (to - from) / 2; ++i) {
            swap(array, i, to + from - i - 1);
        }
    }

    private static List<String> getSkippedSequence(
            List<String> sequence,
            boolean[] tokens) {
        List<String> result = new ArrayList<String>(sequence);
        int i = 0;
        for (boolean t : tokens) {
            if (t) {
                result.set(i, "*");
            }
            ++i;
        }
        return result;
    }

    private static boolean nextGenerateSkippedSequences(
            List<String> sequence,
            boolean[] tokens,
            int first,
            int last) {
        if (first == last) {
            return false;
        } else if (first + 1 == last) {
            return false;
        }

        int i = last - 1;

        while (true) {
            int ii = i--;
            if (tokens[i] && !tokens[ii]) { // tokens[i] > tokens[ii]
                int j = last - 1;
                while (!tokens[i] || tokens[j]) { // tokens[i] <= tokens[ii]
                    --j;
                }
                swap(tokens, i, j);
                reverse(tokens, ii, last);
                return true;

            }
            if (i == first) {
                reverse(tokens, first, last);
                return false;
            }
        }
    }

    private static void generateSkippedSequences(
            List<String> sequence,
            boolean[] tokens) {
        do {
            System.out.println(getSkippedSequence(sequence, tokens));
        } while (nextGenerateSkippedSequences(sequence, tokens, 0,
                sequence.size()));
    }

    private static void generateSkippedSequences(List<String> sequence) {
        for (int order = 0; order != sequence.size() + 1; ++order) {
            System.out.println("-----------");
            System.out.println("--- order=" + order);
            boolean[] tokens = new boolean[sequence.size()];
            for (int i = 0; i != order; ++i) {
                tokens[i] = true;
            }
            for (int i = order; i != sequence.size(); ++i) {
                tokens[i] = false;
            }
            generateSkippedSequences(sequence, tokens);
        }
        System.out.println("-----------");
    }

    @SuppressWarnings("unused")
    private static void generateSkippedSequencesRec(
            List<String> sequence,
            boolean[] tokens,
            int index) {
        if (index == tokens.length - 1) {
            System.out.println(StringUtils.join(
                    getSkippedSequence(sequence, tokens), " "));
            return;
        }

        generateSkippedSequencesRec(sequence, tokens, index + 1);
        for (int i = index + 1; i != sequence.size(); ++i) {
            if (tokens[index] == tokens[i]) {
                continue;
            }
            swap(tokens, index, i);
            generateSkippedSequencesRec(sequence, tokens, index + 1);
        }
        for (int i = sequence.size() - 1; i != index; --i) {
            swap(tokens, index, i);
        }
    }

    public static void main(String[] args) {
        generateSkippedSequences(Arrays.asList("a", "b", "c", "d", "e"));
    }

}
