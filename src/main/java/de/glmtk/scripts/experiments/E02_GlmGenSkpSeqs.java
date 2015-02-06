/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.scripts.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.glmtk.util.ArrayUtils;
import de.glmtk.util.StringUtils;

/**
 * 2014-12-22 Trying to efficiently generate unique skipped sequences per order
 * for recursive calls of GLM.
 *
 * <p>
 * Result: Generating all unique skipped sequences is the same as generating
 * permutations of cnt and skp, where cnt is dependent on position but counts as
 * a duplicate concerning permutation. An efficient implementation is <a
 * href="http://marknelson.us/2002/03/01/next-permutation/">
 * std::next_permutation()</a>
 */
public class E02_GlmGenSkpSeqs {
    private static List<String> getSkippedSequence(List<String> sequence,
            boolean[] pattern) {
        List<String> result = new ArrayList<>(sequence);
        int i = 0;
        for (boolean p : pattern) {
            if (p)
                result.set(i, "*");
            ++i;
        }
        return result;
    }

    private static boolean nextGenerateSkippedSequences(@SuppressWarnings("unused") List<String> sequence,
                                                        boolean[] pattern,
                                                        int first,
                                                        int last) {
        if (first == last)
            return false;
        else if (first + 1 == last)
            return false;

        int i = last - 1;

        while (true) {
            int ii = i--;
            if (pattern[i] && !pattern[ii]) { // pattern[i] > pattern[ii]
                int j = last - 1;
                while (!pattern[i] || pattern[j])
                    --j;
                ArrayUtils.swap(pattern, i, j);
                ArrayUtils.reverse(pattern, ii, last);
                return true;

            }
            if (i == first) {
                ArrayUtils.reverse(pattern, first, last);
                return false;
            }
        }
    }

    private static void generateSkippedSequences(List<String> sequence,
                                                 boolean[] pattern) {
        do
            System.out.println(getSkippedSequence(sequence, pattern));
        while (nextGenerateSkippedSequences(sequence, pattern, 0,
                sequence.size()));
    }

    private static void generateSkippedSequences(List<String> sequence) {
        for (int order = 0; order != sequence.size() + 1; ++order) {
            System.out.println("-----------");
            System.out.println("--- order=" + order);
            boolean[] pattern = new boolean[sequence.size()];
            for (int i = 0; i != order; ++i)
                pattern[i] = true;
            for (int i = order; i != sequence.size(); ++i)
                pattern[i] = false;
            generateSkippedSequences(sequence, pattern);
        }
        System.out.println("-----------");
    }

    @SuppressWarnings("unused")
    private static void generateSkippedSequencesRec(List<String> sequence,
                                                    boolean[] pattern,
                                                    int index) {
        if (index == pattern.length - 1) {
            System.out.println(StringUtils.join(getSkippedSequence(sequence,
                    pattern), " "));
            return;
        }

        generateSkippedSequencesRec(sequence, pattern, index + 1);
        for (int i = index + 1; i != sequence.size(); ++i) {
            if (pattern[index] == pattern[i])
                continue;
            ArrayUtils.swap(pattern, index, i);
            generateSkippedSequencesRec(sequence, pattern, index + 1);
        }
        for (int i = sequence.size() - 1; i != index; --i)
            ArrayUtils.swap(pattern, index, i);
    }

    public static void main(String[] args) {
        generateSkippedSequences(Arrays.asList("a", "b", "c", "d", "e"));
    }
}
