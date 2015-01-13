package de.glmtk.testutil;

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.Config;
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;

public enum TestCorpus {
    ABC,
    MOBYDICK,
    EN0008T;

    public static void intializeTestCorpora(Config config) throws Exception {
        for (TestCorpus testCorpus : values())
            testCorpus.intialize(config);
    }

    private String corpusName;
    private Path corpus;
    private Path workingDir;
    private Glmtk glmtk;

    private TestCorpus() {
        corpusName = toString();
        corpus = Constants.TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase());
        workingDir = Constants.TEST_RESSOURCES_DIR.resolve(corpusName.toLowerCase()
                + Constants.WORKING_DIR_SUFFIX);
    }

    private void intialize(Config config) throws Exception {
        glmtk = new Glmtk(config, corpus, workingDir);

        Set<Pattern> neededPatterns = Patterns.getCombinations(Constants.TEST_ORDER,
                Arrays.asList(CNT, SKP));
        for (Pattern pattern : new HashSet<>(neededPatterns)) {
            if (pattern.size() != Constants.TEST_ORDER)
                neededPatterns.add(pattern.concat(WSKP));

            if (pattern.contains(SKP))
                neededPatterns.add(pattern.replace(SKP, WSKP));
        }

        glmtk.count(neededPatterns);
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
    public CountCache getCountCache() throws Exception {
        return glmtk.getOrCreateCountCache();
    }

    public String[] getWords() throws Exception {
        Set<String> words = getCountCache().getWords();
        return words.toArray(new String[words.size()]);
    }

    public List<String> getSequenceList(int n,
            int length) throws Exception {
        List<String> result = new LinkedList<>();
        for (int k = 0; k != length; ++k) {
            result.add(getWords()[n % getWords().length]);
            n /= getWords().length;
        }
        Collections.reverse(result);
        return result;
    }
}
