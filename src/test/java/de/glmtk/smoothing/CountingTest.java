package de.glmtk.smoothing;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import de.glmtk.Counter;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;
import de.glmtk.smoothing.helper.LoggingTest;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.utils.StringUtils;

public class CountingTest extends LoggingTest {

    // TODO: extend for POS

    private static final Logger LOGGER = LogManager
            .getLogger(CountingTest.class);

    @Test
    public void testAbc() throws IOException {
        testCounting(TestCorpus.ABC);
    }

    @Test
    public void testMobyDick() throws IOException {
        testCounting(TestCorpus.MOBY_DICK);
    }

    @Test
    public void testEn0008t() throws IOException {
        testCounting(TestCorpus.EN0008T);
    }

    private void testCounting(TestCorpus testCorpus) throws IOException {
        LOGGER.info("===== {} corpus =====", testCorpus.getCorpusName());

        // Load corpus into memory
        List<String> corpusContents = new LinkedList<String>();
        try (BufferedReader reader =
                Files.newBufferedReader(testCorpus.getCorpus(),
                        Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                corpusContents.add(line);
            }
        }

        CountCache countCache = testCorpus.getCountCache();
        testAbsoluteCounts(corpusContents, countCache.getAbsolute());
        testContinuationCounts(corpusContents, countCache.getContinuation());
    }

    private void testAbsoluteCounts(
            List<String> corpusContents,
            Map<Pattern, Map<String, Long>> absoluteCounts) {
        LOGGER.info("=== Absolute");

        for (Map.Entry<Pattern, Map<String, Long>> patternCounts : absoluteCounts
                .entrySet()) {
            Pattern pattern = patternCounts.getKey();
            LOGGER.info("# {}", pattern);

            Map<String, Long> counts = patternCounts.getValue();
            for (Map.Entry<String, Long> sequenceCount : counts.entrySet()) {
                String sequence = sequenceCount.getKey();
                long count = sequenceCount.getValue();

                String regexString = sequenceToRegex(sequence);
                java.util.regex.Pattern regex =
                        java.util.regex.Pattern.compile(regexString);
                LOGGER.trace("  {} (regex='{}')", sequence, regexString);
                int numMatches = 0;
                for (String line : corpusContents) {
                    Matcher matcher = regex.matcher(line);

                    int numLineMatches = 0;
                    if (matcher.find()) {
                        do {
                            ++numLineMatches;
                            LOGGER.trace(matcher);
                        } while (matcher.find(matcher.start(1)));
                    }

                    numMatches += numLineMatches;

                    LOGGER.trace("    {} ({})", line, numLineMatches);
                }

                if (count != numMatches) {
                    LOGGER.debug("{} (count={}, matches={})", sequence, count,
                            numMatches);
                    assertEquals(count, numMatches);
                }
            }
        }
    }

    private void testContinuationCounts(
            List<String> corpusContents,
            Map<Pattern, Map<String, Counter>> continuationCounts) {
        LOGGER.info("=== Continuation");
    }

    private String sequenceToRegex(String sequence) {
        StringBuilder regex = new StringBuilder();
        regex.append("(?:^| )");

        boolean first = true;
        for (String word : StringUtils.splitAtChar(sequence, ' ')) {
            if (!first) {
                regex.append(' ');
            }

            if (!word.equals(PatternElem.SKIPPED_WORD)) {
                regex.append(word);
            } else {
                regex.append("\\S+");
            }

            if (first) {
                regex.append("()");
                first = false;
            }
        }

        regex.append("(?: |$)");
        return regex.toString();
    }

}
