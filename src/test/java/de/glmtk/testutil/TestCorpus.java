package de.glmtk.testutil;

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;

public enum TestCorpus {

    ABC,

    MOBYDICK,

    EN0008T;

    private String corpusName;

    private Path corpus;

    private Path workingDir;

    private CountCache countCache;

    private TestCorpus() {
        try {
            corpusName = toString();
            corpus =
                    Constants.TEST_RESSOURCES_DIR.resolve(corpusName
                            .toLowerCase());
            workingDir =
                    Constants.TEST_RESSOURCES_DIR.resolve(corpusName
                            .toLowerCase()
                            + Constants.STANDARD_WORKING_DIR_SUFFIX);

            Glmtk glmtk = new Glmtk(corpus, workingDir);

            Set<Pattern> neededPatterns =
                    Patterns.getCombinations(Constants.ORDER,
                            Arrays.asList(CNT, SKP));
            for (Pattern pattern : new HashSet<Pattern>(neededPatterns)) {
                if (pattern.size() != Constants.ORDER) {
                    neededPatterns.add(pattern.concat(WSKP));
                }

                if (pattern.contains(SKP)) {
                    neededPatterns.add(pattern.replace(SKP, WSKP));
                }
            }

            glmtk.count(neededPatterns);
        } catch (Exception e) {
            // Because of enum nature it is necessary to not throw any checked
            // exceptions during construction.
            throw new IllegalStateException(e);
        }
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
