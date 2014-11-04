package de.glmtk.smoothing.helper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.smoothing.CountCache;
import de.glmtk.utils.StringUtils;

public class TestCorpus {

    public static final TestCorpus ABC, MOBY_DICK;

    static {
        try {
            ABC = new TestCorpus("ABC");
            MOBY_DICK = new TestCorpus("MobyDick");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Static class initalization failed", e);
        }
    }

    private String corpusName = null;

    private Path workingDir = null;

    private CountCache countCache = null;

    private TestCorpus(
            String name) throws IOException, InterruptedException {
        this(name, Constants.TEST_RESSOURCES_DIR.resolve(name.toLowerCase()
                + ".txt"), Constants.TEST_RESSOURCES_DIR.resolve(name
                        .toLowerCase()));
    }

    private TestCorpus(
            String name,
            Path corpus,
            Path workingDir) throws IOException, InterruptedException {
        corpusName = name;
        this.workingDir = workingDir;

        Glmtk glmtk = new Glmtk();
        glmtk.setCorpus(corpus);
        glmtk.setWorkingDir(workingDir);
        glmtk.count();

        countCache = new CountCache(workingDir);

        //        workingDir = output;
        //
        //        if (!Files.exists(output)) {
        //            Files.createDirectory(output);
        //        }

        //        Path indexFile = workingDir.resolve("index.txt");
        //        Path sequencesDir = workingDir.resolve("sequences");
        //        absoluteDir = workingDir.resolve("absolute");
        //        continuationDir = workingDir.resolve("continuation");
        //
        //        // index
        //        if (!Files.exists(indexFile)) {
        //            try (InputStream input = Files.newInputStream(trainingFile);
        //                    OutputStream output = Files.newOutputStream(indexFile)) {
        //                IndexBuilder indexBuilder = new IndexBuilder(false, false, 5);
        //                indexBuilder.buildIndex(input, output, 1, 1);
        //            }
        //        }
        //        try (InputStream input = Files.newInputStream(indexFile)) {
        //            index = new Index(input);
        //        }
        //
        //        // sequences
        //        if (!Files.exists(sequencesDir)) {
        //            Sequencer sequencer =
        //                    new Sequencer(trainingFile, sequencesDir, index, 1, false,
        //                            false);
        //            sequencer.sequence(Pattern.getCombinations(5,
        //                    Arrays.asList(PatternElem.CNT, PatternElem.SKP)));
        //        }
        //
        //        // absolute
        //        if (!Files.exists(absoluteDir)) {
        //            AbsoluteCounter absoluteCounter =
        //                    new AbsoluteCounter(sequencesDir, absoluteDir, "\t", 1,
        //                            false, true);
        //            absoluteCounter.count();
        //        }
        //
        //        // continuation
        //        if (!Files.exists(continuationDir)) {
        //            ContinuationCounter continuationCounter =
        //                    new ContinuationCounter(absoluteDir, continuationDir,
        //                            index, "\t", 1, false, true);
        //            continuationCounter.count();
        //        }
    }

    public String getCorpusName() {
        return corpusName;
    }

    public CountCache getCountCache() {
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

    // TODO: what is this?
    @Deprecated
    public Path getSequencesTestingSample(int length) throws IOException {
        Path sequencesTestSample =
                workingDir.resolve("sequences-testing-samples-" + length);
        if (!Files.exists(sequencesTestSample)) {
            try (BufferedWriter writer =
                    Files.newBufferedWriter(sequencesTestSample,
                            Charset.defaultCharset())) {
                for (int i = 0; i != ((int) Math.pow(getWords().length, length)); ++i) {
                    writer.write(StringUtils.join(getSequenceList(i, length),
                            " "));
                    writer.write("\n");
                }
            }
        }
        return sequencesTestSample;
    }

    //    public Path getWorkingDir() {
    //        return workingDir;
    //    }
    //
    //    public void setWorkingDir(Path workingDir) {
    //        this.workingDir = workingDir;
    //    }
    //
    //    public Path getAbsoluteDir() {
    //        return absoluteDir;
    //    }
    //
    //    public void setAbsoluteDir(Path absoluteDir) {
    //        this.absoluteDir = absoluteDir;
    //    }
    //
    //    public Path getContinuationDir() {
    //        return continuationDir;
    //    }
    //
    //    public void setContinuationDir(Path continuationDir) {
    //        this.continuationDir = continuationDir;
    //    }
    //
    //    public CountCache getCorpus() throws IOException {
    //        return new CountCache(getAbsoluteDir(), getContinuationDir());
    //    }

}
