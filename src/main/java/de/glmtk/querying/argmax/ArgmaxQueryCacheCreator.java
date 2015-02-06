/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
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

package de.glmtk.querying.argmax;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.files.CountsReader;
import de.glmtk.querying.probability.QueryCacherCreator;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class ArgmaxQueryCacheCreator extends QueryCacherCreator {
    private static final String ARGMAX_WILDCARD = "*";

    private class Worker extends QueryCacherCreator.Worker {
        private Queue<String> neededWildcardSequences;
        private String nextWildcardSequence;

        @Override
        protected void extractSequences(Pattern pattern) throws IOException {
            Pattern corePattern = calculateCorePattern(pattern);
            if (corePattern.isEmpty()) {
                neededSequences = new ArrayDeque<>();
                neededSequences.add(trailingSequence.substring(1));
                neededWildcardSequences = new ArrayDeque<>();
                return;
            }

            Set<String> sequences = new HashSet<>();
            Set<String> wildcardSequences = new HashSet<>();
            try (BufferedReader reader = NioUtils.newBufferedReader(queryFile,
                    Constants.CHARSET, readerMemory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    List<String> sequenceList = StringUtils.split(line, ' ');
                    sequenceList.add(ARGMAX_WILDCARD);
                    String[] split = sequenceList.toArray(new String[sequenceList.size()]);
                    String[] words = new String[split.length];
                    String[] poses = new String[split.length];
                    StringUtils.extractWordsAndPoses(split, queryFileTagged,
                            words, poses);

                    int patternSize = corePattern.size();
                    for (int p = 0; p <= words.length - patternSize; ++p) {
                        String sequence = corePattern.apply(words, poses, p);
                        if (!sequence.endsWith(ARGMAX_WILDCARD))
                            sequences.add(leadingSequence + sequence
                                    + trailingSequence);
                        else
                            wildcardSequences.add(leadingSequence + sequence
                                    + trailingSequence);
                    }
                }
            }

            neededSequences = new ArrayDeque<>(new TreeSet<>(sequences));
            neededWildcardSequences = new ArrayDeque<>(new TreeSet<>(
                    wildcardSequences));
        }

        @Override
        protected void filterAndWriteSequenceCounts() throws IOException {
            try (CountsReader reader = new CountsReader(patternFile,
                    Constants.CHARSET, readerMemory);
                    BufferedWriter writer = NioUtils.newBufferedWriter(
                            targetPatternFile, Constants.CHARSET, writerMemory)) {
                nextSequence = neededSequences.poll();
                nextWildcardSequence = neededWildcardSequences.poll();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (nextSequence == null && nextWildcardSequence == null)
                        break;

                    String sequence = reader.getSequence();
                    if (sequence == null)
                        continue;

                    if (isSequenceNeeded(sequence)
                            || isWildcardSequenceNeeded(sequence))
                        writer.append(line).append('\n');
                }
            }
        }

        private boolean isWildcardSequenceNeeded(String sequence) {
            if (nextWildcardSequence == null)
                return false;

            List<String> sequenceList = StringUtils.split(sequence, ' ');
            int wildCardPos = sequenceList.size()
                    - (trailingSequence.length() / 2) - 1;
            String argmaxToken = sequenceList.get(wildCardPos);
            sequenceList.set(wildCardPos, ARGMAX_WILDCARD);
            sequence = StringUtils.join(sequenceList, ' ');

            int cmp;
            while ((cmp = sequence.compareTo(nextWildcardSequence)) >= 0) {
                if (cmp == 0) {
                    // TODO: improve vocab to not being a hashset, since I know
                    // it ordering a an ordered list with a pointer should be
                    // enough
                    if (vocab != null && !vocab.contains(argmaxToken))
                        return false;

                    return true;
                }

                nextWildcardSequence = neededWildcardSequences.poll();
                if (nextWildcardSequence == null)
                    return false;
            }

            return false;
        }
    }

    private Set<String> vocab;

    public ArgmaxQueryCacheCreator(Config config) {
        super(config);
    }

    /**
     * Like
     * {@link #createQueryCache(String, Path, boolean, Set, Status, GlmtkPaths)}
     * but allows to restrict allow argmax arguments by specifying a vocab file.
     */
    public GlmtkPaths createQueryCache(String name,
                                       Path queryFile,
                                       boolean queryFileTagged,
                                       Path vocabFile,
                                       Set<Pattern> patterns,
                                       Status status,
                                       GlmtkPaths paths) throws Exception {
        loadVocab(vocabFile);

        return super.createQueryCache(name, queryFile, queryFileTagged,
                patterns, status, paths);
    }

    private void loadVocab(Path vocabFile) throws IOException {
        vocab = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(vocabFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                vocab.add(line);
            }
        }
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }
}
