package de.typology.executables;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.typology.counting.AbsoluteCounter;
import de.typology.counting.ContinuationCounter;
import de.typology.indexing.Index;
import de.typology.indexing.IndexBuilder;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.sequencing.Sequencer;
import de.typology.tagging.PosTagger;

public class GlmtkCount extends Executable {

    private static final String OPTION_OUTPUT = "output";

    private static final String OPTION_MODEL_LENGTH = "model-length";

    private static final String OPTION_COUNT_POS = "count-pos";

    private static final String OPTION_TAG_POS = "tag-pos";

    private static final String OPTION_PATTERNS = "patterns";

    private static final String OPTION_NO_ABSCOUNTS = "no-abscounts";

    private static final String OPTION_NO_CONTCOUNTS = "no-contcounts";

    private static final String OPTION_KEEP_TEMP = "keep-temp";

    private Path corpus = null;

    private Path output = null;

    private int modelLength = 5;

    private boolean countPos = false;

    private boolean tagPos = false;

    @SuppressWarnings("unused")
    private String patterns = "cmbskp";

    private boolean noAbsCounts = false;

    private boolean noContCounts = false;

    private boolean keepTemp = false;

    private static Options options;
    static {
        //@formatter:off
        Option help         = new Option("h",  OPTION_HELP,          false, "Print this message.");
        Option version      = new Option("v",  OPTION_VERSION,       false, "Print the version information and exit.");
        Option output       = new Option("o",  OPTION_OUTPUT,        true,  "Use given directory for output.");
               output.setArgName("OUTPUTDIR");
        Option modelLength  = new Option("n",  OPTION_MODEL_LENGTH,  true,  "Compute n-grams up to model length N.");
               modelLength.setArgName("N");
        Option countPos     = new Option("t",  OPTION_COUNT_POS,     false, "If set, will include counts of part of speeches.");
        Option tagPos       = new Option("T",  OPTION_TAG_POS,       false, "If set, corpus will be part of speech tagged before counting (automatically also counts parts of speeches).");
        Option patterns     = new Option("p",  OPTION_PATTERNS,      true,  "noskp or cmbskp");
               patterns.setArgName("PATTERNS");
        Option noAbsCounts  = new Option("a",  OPTION_NO_ABSCOUNTS,  false, "If set, will not aggregate absolute counts (forces no continuation counts).");
        Option noContCounts = new Option("c",  OPTION_NO_CONTCOUNTS, false, "If set, will not aggregate continuation counts.");
        Option keepTemp     = new Option(null, OPTION_KEEP_TEMP,     false, "If set, will not delete temp files.");
        //@formatter:on

        options = new Options();
        options.addOption(help);
        options.addOption(version);
        options.addOption(output);
        options.addOption(modelLength);
        options.addOption(countPos);
        options.addOption(tagPos);
        options.addOption(patterns);
        options.addOption(noAbsCounts);
        options.addOption(noContCounts);
        options.addOption(keepTemp);
    }

    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption(OPTION_VERSION)) {
                System.out
                        .println("GLMTK (generalized language modeling toolkit) version 0.1.");
                return;
            }

            if (line.hasOption(OPTION_HELP)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setSyntaxPrefix("Usage: ");
                formatter.setWidth(80);
                formatter
                        .printHelp("glmtk-count [OPTION]... <CORPUS>", options);
                return;
            }

            if (line.getArgs() == null || line.getArgs().length == 0) {
                System.err.println("glmtk-count: missing corpus\n"
                        + "Try 'glmtk-count --help' for more information.");
                return;
            }

            new GlmtkCount().run(line, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    protected void exec(CommandLine line) throws Exception {
        readOptions(line);

        Files.createDirectories(output);
        Path trainingFile = output.resolve("training.txt");
        Path indexFile = output.resolve("index.txt");
        Path sequencesDir = output.resolve("sequences");
        Path absoluteDir = output.resolve("absolute");
        Path continuationDir = output.resolve("continuation");

        // Tagging
        if (countPos && tagPos) {
            PosTagger tagger =
                    new PosTagger(corpus, trainingFile, config.getModel());
            tagger.tag();
        } else {
            Files.copy(corpus, trainingFile);
        }

        // Indexing
        try (InputStream input = Files.newInputStream(trainingFile);
                OutputStream output = Files.newOutputStream(indexFile)) {
            IndexBuilder indexBuilder =
                    new IndexBuilder(countPos, false, modelLength);
            indexBuilder.buildIndex(input, output, 1, 1);
        }
        Index index;
        try (InputStream input = Files.newInputStream(indexFile)) {
            index = new Index(input);
        }

        // Sequencing
        Sequencer sequencer =
                new Sequencer(trainingFile, sequencesDir, index, 1, countPos,
                        false);
        if (countPos) {
            sequencer.sequence(Pattern.getCombinations(modelLength,
                    new PatternElem[] {
                        PatternElem.CNT, PatternElem.SKP, PatternElem.POS
                    }));
        } else {
            sequencer.sequence(Pattern.getCombinations(modelLength,
                    new PatternElem[] {
                        PatternElem.CNT, PatternElem.SKP
                    }));
        }

        // Absolute
        if (!noAbsCounts) {
            AbsoluteCounter absoluteCounter =
                    new AbsoluteCounter(sequencesDir, absoluteDir, "\t",
                            config.getNumberOfCores(), !keepTemp, true);
            absoluteCounter.count();
        }

        // Continuation
        if (!noContCounts) {
            ContinuationCounter continuationCounter =
                    new ContinuationCounter(absoluteDir, continuationDir,
                            index, "\t", config.getNumberOfCores(), countPos,
                            true);
            continuationCounter.count();
        }

        // TODO: find better way to do this, system properties?
        Files.copy(log, output.resolve("info.log"));
    }

    private void readOptions(CommandLine line) {
        corpus = Paths.get(line.getArgs()[0]);
        if (!Files.exists(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" does not exist.");
        }
        if (Files.isDirectory(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" is a directory.");
        }
        if (!Files.isReadable(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" is not readable.");
        }

        if (line.hasOption(OPTION_OUTPUT)) {
            output = Paths.get(line.getOptionValue(OPTION_OUTPUT));
        } else {
            output = Paths.get(corpus + ".out");
        }
        if (!Files.notExists(output)) {
            throw new IllegalStateException("Given output \"" + output
                    + "\" already exists.");
        }

        if (line.hasOption(OPTION_MODEL_LENGTH)) {
            modelLength =
                    Integer.parseInt(line.getOptionValue(OPTION_MODEL_LENGTH));
        }

        if (line.hasOption(OPTION_COUNT_POS)) {
            countPos = true;
        }

        if (line.hasOption(OPTION_TAG_POS)) {
            countPos = true;
            tagPos = true;
        }

        if (line.hasOption(OPTION_PATTERNS)) {
            patterns = line.getOptionValue(OPTION_PATTERNS);
        }

        if (line.hasOption(OPTION_NO_ABSCOUNTS)) {
            noAbsCounts = true;
            noContCounts = true;
        }

        if (line.hasOption(OPTION_NO_CONTCOUNTS)) {
            noContCounts = true;
        }

        if (line.hasOption(OPTION_KEEP_TEMP)) {
            keepTemp = true;
        }
    }
}
