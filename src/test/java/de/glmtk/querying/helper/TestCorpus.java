package de.glmtk.querying.helper;

import static de.glmtk.Constants.TEST_RESSOURCES_DIR;
import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.SKP;
import static de.glmtk.utils.PatternElem.WSKP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.PatternCalculator;

public class TestCorpus {

    public static final TestCorpus ABC, MOBY_DICK, EN0008T;

    static {
        try {
            ABC = new TestCorpus("ABC");
            MOBY_DICK = new TestCorpus("MobyDick");
            EN0008T = new TestCorpus("en0008t");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Static class initalization failed", e);
        }
    }

    private String corpusName;

    private Path corpus;

    private Path workingDir;

    private CountCache countCache;

    private TestCorpus(
            String corpusName) throws IOException, InterruptedException {
        this(corpusName, TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase()),
                TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase() + ".out"));
    }

    private TestCorpus(
            String corpusName,
            Path corpus,
            Path workingDir) throws IOException, InterruptedException {
        this.corpusName = corpusName;
        this.corpus = corpus;
        this.workingDir = workingDir;

        Glmtk glmtk = new Glmtk(corpus, workingDir);

        Set<Pattern> neededPatterns =
                PatternCalculator.getCombinations(Constants.MODEL_SIZE,
                        Arrays.asList(CNT, SKP));
        for (Pattern pattern : neededPatterns) {
            if (pattern.size() != Constants.MODEL_SIZE) {
                neededPatterns.add(pattern.concat(WSKP));
            }

            if (pattern.contains(SKP)) {
                neededPatterns.add(pattern.replace(SKP, WSKP));
            }
        }

        glmtk.count(false, neededPatterns);
    }

    public String getCorpusName() {
        return corpusName;
    }

    public Path getCorpus() {
        return corpus;
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    /**
     * Lazily loaded.
     */
    public CountCache getCountCache() throws IOException {
        if (countCache == null) {
            countCache = new CountCache(workingDir);
        }
        return countCache;
    }

    public String[] getWords() {
        Set<String> words = countCache.getWords();
        return words.toArray(new String[words.size()]);
    }

    public List<String> getSequenceList(int n, int length) {
        List<String> result = new LinkedList<String>();
        for (int k = 0; k != length; ++k) {
            result.add(getWords()[n % getWords().length]);
            n /= getWords().length;
        }
        Collections.reverse(result);
        return result;
    }

}
