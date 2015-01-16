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

package de.glmtk.counting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Counts;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;
import de.glmtk.util.StringUtils;

/**
 * Checks whether counts present in count files are correct, but not if there
 * are sequences missing.
 */
@RunWith(Parameterized.class)
public class CountingTest extends TestCorporaTest {
    private static final Logger LOGGER = LogManager.getFormatterLogger(CountingTest.class);

    private static final double SELECTION_CHANCE = 0.001;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { {TestCorpus.ABC},
                {TestCorpus.MOBYDICK}, {TestCorpus.EN0008T}});
    }

    private TestCorpus testCorpus;
    private List<String> corpusLines;
    private CountCache countCache;
    private Map<Pattern, Map<String, Long>> absolute;
    private Map<Pattern, Map<String, Counts>> continuation;

    @SuppressWarnings("unchecked")
    public CountingTest(TestCorpus testCorpus) throws Exception {
        LOGGER.info("===== %s corpus =====", testCorpus.getCorpusName());
        this.testCorpus = testCorpus;

        LOGGER.info("Loading corpus...");
        corpusLines = Files.readAllLines(testCorpus.getCorpus(),
                Constants.CHARSET);

        LOGGER.info("Loading counts...");
        countCache = testCorpus.getCountCache();

        Field absoluteField = CountCache.class.getDeclaredField("absolute");
        absoluteField.setAccessible(true);
        absolute = (Map<Pattern, Map<String, Long>>) absoluteField.get(countCache);

        Field continuationField = CountCache.class.getDeclaredField("continuation");
        continuationField.setAccessible(true);
        continuation = (Map<Pattern, Map<String, Counts>>) continuationField.get(countCache);
    }

    @Test
    public void testAbsolute() {
        LOGGER.info("=== Absolute");

        for (Entry<Pattern, Map<String, Long>> patternCounts : absolute.entrySet()) {
            Pattern pattern = patternCounts.getKey();
            Map<String, Long> countsWithPattern = patternCounts.getValue();

            LOGGER.info("# %s", pattern);

            long time = System.currentTimeMillis();
            long numRead = 0;
            long total = countsWithPattern.size();

            for (Entry<String, Long> sequenceCounts : countsWithPattern.entrySet()) {
                if (config.getUpdateIntervalLog() != 0) {
                    ++numRead;
                    long curTime = System.currentTimeMillis();
                    if (curTime - time >= config.getUpdateIntervalLog()) {
                        time = curTime;
                        LOGGER.info("%6.2f%%", 100.0f * numRead / total);
                    }
                }

                if (testCorpus != TestCorpus.ABC
                        && testCorpus != TestCorpus.MOBYDICK)
                    if (Math.random() > SELECTION_CHANCE)
                        continue;

                String sequence = sequenceCounts.getKey();
                long count = sequenceCounts.getValue();

                assertSequenceHasAbsoluteCount(sequence, count);
            }
        }
    }

    private void assertSequenceHasAbsoluteCount(String sequence,
                                                long count) {
        String regexString = sequenceToRegex(sequence);
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(regexString);
        LOGGER.trace("  %s (regex='%s')", sequence, regexString);

        int numMatches = 0;
        for (String line : corpusLines) {
            Matcher matcher = regex.matcher(line);

            int numLineMatches = 0;
            if (matcher.find())
                do {
                    ++numLineMatches;
                    LOGGER.trace(matcher);
                } while (matcher.find(matcher.start(1)));

            numMatches += numLineMatches;

            LOGGER.trace("    %s (%s)", line, numLineMatches);
        }

        if (count != numMatches) {
            LOGGER.info("%s (count=%s, matches=%s)", sequence, count,
                    numMatches);
            assertEquals(numMatches, count);
        }
    }

    @Test
    public void testContinuationCounts() {
        LOGGER.info("=== Continuation");

        for (Entry<Pattern, Map<String, Counts>> patternCounts : continuation.entrySet()) {
            Pattern pattern = patternCounts.getKey();
            Map<String, Counts> countsWthPattern = patternCounts.getValue();

            LOGGER.info("# %s", pattern);

            long time = System.currentTimeMillis();
            long numRead = 0;
            long total = countsWthPattern.size();

            for (Entry<String, Counts> sequenceCounts : countsWthPattern.entrySet()) {
                if (config.getUpdateIntervalLog() != 0) {
                    ++numRead;
                    long curTime = System.currentTimeMillis();
                    if (curTime - time >= config.getUpdateIntervalLog()) {
                        time = curTime;
                        LOGGER.info("%6.2f%%", 100.0f * numRead / total);
                    }
                }

                if (testCorpus != TestCorpus.ABC
                        && testCorpus != TestCorpus.MOBYDICK)
                    if (Math.random() > SELECTION_CHANCE)
                        continue;

                String sequence = sequenceCounts.getKey();
                Counts counts = sequenceCounts.getValue();

                assertSequenceHasContinuationCount(pattern, sequence, counts);
            }
        }
    }

    private void assertSequenceHasContinuationCount(Pattern pattern,
                                                    String sequence,
                                                    Counts counts) {
        String regexString = sequenceToRegex(sequence);
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(regexString);
        LOGGER.trace("  %s (regex='%s')", sequence, regexString);

        Map<String, Long> matches = new HashMap<>();
        for (String line : corpusLines)
            if (config.getUpdateIntervalLog() != 0) {
                Matcher matcher = regex.matcher(line);

                if (matcher.find())
                    do {
                        String found = keyFromGroup(pattern,
                                matcher.group(0).trim());
                        Long foundCount = matches.get(found);
                        matches.put(found, foundCount == null
                                ? 1
                                        : foundCount + 1);
                        LOGGER.trace(matcher);
                    } while (matcher.find(matcher.start(1)));

                LOGGER.trace("    %s", line);
            }

        Counts foundCounts = new Counts();
        for (Long count : matches.values())
            foundCounts.addOne(count);

        if (!counts.equals(foundCounts)) {
            LOGGER.info("%s (counts=%s, foundCounts=%s)", sequence, counts,
                    foundCounts);
            fail();
        }
    }

    private String keyFromGroup(Pattern pattern,
                                String group) {
        List<String> split = StringUtils.splitAtChar(group, ' ');
        List<String> result = new LinkedList<>();
        int i = 0;
        for (PatternElem elem : pattern) {
            if (elem == PatternElem.WSKP)
                result.add(split.get(i));
            else
                result.add("-");
            ++i;
        }
        return StringUtils.join(result, " ");
    }

    private String sequenceToRegex(String sequence) {
        StringBuilder regex = new StringBuilder();
        regex.append("(?:^| )");

        boolean first = true;
        for (String word : StringUtils.splitAtChar(sequence, ' ')) {
            if (!first)
                regex.append(' ');

            if (!word.equals(PatternElem.SKP_WORD)
                    && !word.equals(PatternElem.WSKP_WORD))
                regex.append(word.replaceAll("[^\\w ]", "\\\\$0"));
            else
                regex.append("\\S+");

            if (first) {
                regex.append("()");
                first = false;
            }
        }

        regex.append("(?: |$)");
        return regex.toString();
    }
}
