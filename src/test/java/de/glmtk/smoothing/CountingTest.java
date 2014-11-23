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
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.glmtk.Config;
import de.glmtk.Counter;
import de.glmtk.querying.CountCache;
import de.glmtk.smoothing.helper.TestCorporaTest;
import de.glmtk.smoothing.helper.TestCorpus;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.PatternElem;
import de.glmtk.utils.StringUtils;

/**
 * Checks whether counts present in count files are correct, but not if there
 * are sequences missing.
 */
public class CountingTest extends TestCorporaTest {

    // TODO: extend for POS

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(CountingTest.class);

    private static Config config = null;

    @BeforeClass
    public static void loadConfig() {
        LOGGER.info("Loading config...");
        config = Config.get();
    }

    public static void loadTestCorpora() {
    }

    @Test
    public void testAbc() throws IOException {
        testCounting(TestCorpus.ABC);
    }

    @Test
    public void testMobyDick() throws IOException {
        testCounting(TestCorpus.MOBY_DICK);
    }

    @Ignore
    @Test
    public void testEn0008t() throws IOException {
        testCounting(TestCorpus.EN0008T);
    }

    private void testCounting(TestCorpus testCorpus) throws IOException {
        LOGGER.info("===== %s corpus =====", testCorpus.getCorpusName());

        LOGGER.info("Loading corpus...");
        long corpusSize = Files.size(testCorpus.getCorpus());
        List<String> corpusContents = new LinkedList<String>();
        try (BufferedReader reader =
                Files.newBufferedReader(testCorpus.getCorpus(),
                        Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                corpusContents.add(line);
            }
        }

        LOGGER.info("Loading counts...");
        CountCache countCache = testCorpus.getCountCache();

        testAbsoluteCounts(corpusContents, corpusSize,
                countCache.getAbsolute(), config.getUpdateInterval());
        testContinuationCounts(corpusContents, corpusSize,
                countCache.getContinuation(), config.getUpdateInterval());
    }

    private void testAbsoluteCounts(
            List<String> corpusContents,
            long corpusSize,
            Map<Pattern, Map<String, Long>> absoluteCounts,
            int updateInterval) {
        LOGGER.info("=== Absolute");

        for (Map.Entry<Pattern, Map<String, Long>> patternCounts : absoluteCounts
                .entrySet()) {
            Pattern pattern = patternCounts.getKey();
            Map<String, Long> counts = patternCounts.getValue();

            LOGGER.info("# %s", pattern);

            long readSize = 0;
            long totalSize = corpusSize * counts.size();
            long time = System.currentTimeMillis();

            for (Map.Entry<String, Long> sequenceCount : counts.entrySet()) {
                String sequence = sequenceCount.getKey();
                long count = sequenceCount.getValue();

                String regexString = sequenceToRegex(sequence);
                java.util.regex.Pattern regex =
                        java.util.regex.Pattern.compile(regexString);
                LOGGER.trace("  %s (regex='%s')", sequence, regexString);
                int numMatches = 0;
                for (String line : corpusContents) {
                    readSize += line.getBytes().length;
                    if (updateInterval != 0) {
                        long curTime = System.currentTimeMillis();
                        if (curTime - time >= updateInterval) {
                            time = curTime;
                            LOGGER.info("%6.2f%%", 100.0f * readSize
                                    / totalSize);
                        }
                    }

                    Matcher matcher = regex.matcher(line);

                    int numLineMatches = 0;
                    if (matcher.find()) {
                        do {
                            ++numLineMatches;
                            LOGGER.trace(matcher);
                        } while (matcher.find(matcher.start(1)));
                    }

                    numMatches += numLineMatches;

                    LOGGER.trace("    %s (%s)", line, numLineMatches);
                }

                if (count != numMatches) {
                    LOGGER.debug("{} (count=%s, matches=%s)", sequence, count,
                            numMatches);
                    assertEquals(numMatches, count);
                }
            }
        }
    }

    private void testContinuationCounts(
            List<String> corpusContents,
            long corpusSize,
            Map<Pattern, Map<String, Counter>> continuationCounts,
            int updateInterval) {
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
                regex.append(word.replaceAll("[^\\w ]", "\\\\$0"));
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
