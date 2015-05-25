package de.glmtk.executables;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.common.NGram;
import de.glmtk.common.Patterns;
import de.glmtk.logging.Logger;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.util.StringUtils;

public class GlmtkDelUnk extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkDelUnk.class);

    public static void main(String[] args) {
        new GlmtkDelUnk().run(args);
    }

    private CorpusOption optionCorpus;

    private Path corpus = null;
    private Path workingDir = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-delunk";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption("c", "corpus",
                "Give corpus and maybe working directory.");

        optionManager.register(optionCorpus);
    }

    @Override
    protected String getHelpHeader() {
        return "Takes input on stdin. Outputs all lines not containing unkown words.";
    }

    @Override
    protected String getHelpFooter() {
        return null;
    }

    @Override
    protected void parseOptions(String[] args) throws Exception {
        super.parseOptions(args);

        corpus = optionCorpus.getCorpus();
        workingDir = optionCorpus.getWorkingDir();
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        CacheSpecification chacheSpec = new CacheSpecification();
        chacheSpec.withCounts(Patterns.getMany("1")).withCacheImplementation(
                CacheImplementation.HASH_MAP).withProgress();
        Cache cache = chacheSpec.build(glmtk.getPaths());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in, Constants.CHARSET));
                OutputStreamWriter writer = new OutputStreamWriter(System.out,
                        Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = StringUtils.split(line.trim(), ' ');

                boolean allWordsSeen = true;
                for (String word : words) {
                    NGram ngram = new NGram(word);
                    if (cache.getCount(ngram) == 0) {
                        allWordsSeen = false;
                        break;
                    }
                }

                if (allWordsSeen)
                    writer.append(line).append('\n');
                writer.flush();
            }
        }
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-",
                80 - getExecutableName().length()));
        LOGGER.debug("Corpus:     %s", corpus);
        LOGGER.debug("WorkingDir: %s", workingDir);
    }
}
