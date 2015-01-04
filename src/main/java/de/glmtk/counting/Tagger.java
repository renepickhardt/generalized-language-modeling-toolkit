package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public enum Tagger {
    TAGGER;

    private static final Logger LOGGER = LogManager.getFormatterLogger(Tagger.class);

    public static boolean detectFileTagged(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file,
                Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                for (String tokenAndTag : StringUtils.splitAtChar(line, ' ')) {
                    int lastSlash = tokenAndTag.lastIndexOf('/');
                    if (lastSlash <= 0 || lastSlash == tokenAndTag.length() - 1) {
                        LOGGER.debug(
                                "Detected Corpus untagged, because in line '%d' at least one token does not have the form '<token>/<pos>'.",
                                lineNo);
                        return false;
                    }
                }
            }
        }
        LOGGER.info("Detected Corpus tagged.");
        return true;
    }

    private MaxentTagger tagger = null;
    private int readerMemory;
    private int writerMemory;

    public void tag(Path inputFile,
                    Path outputFile) throws IOException {
        OUTPUT.setPhase(Phase.TAGGING);
        Progress progress = new Progress(Files.size(inputFile));

        if (inputFile.equals(outputFile))
            throw new IllegalArgumentException(String.format(
                    "Input- equals OutputFile: '%s'.", inputFile));

        calculateMemory();

        if (tagger == null)
            tagger = new MaxentTagger(CONFIG.getModel().toString());

        try (BufferedReader reader = NioUtils.newBufferedReader(inputFile,
                Constants.CHARSET, readerMemory);
                BufferedWriter writer = NioUtils.newBufferedWriter(outputFile,
                        Constants.CHARSET, writerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                progress.increase(line.getBytes(Constants.CHARSET).length);

                // Tag
                List<HasWord> sequence = new LinkedList<HasWord>();
                for (String token : StringUtils.splitAtChar(line, ' '))
                    sequence.add(new Word(token));
                List<TaggedWord> taggedSequence = tagger.tagSentence(sequence);

                // Write
                boolean first = true;
                for (TaggedWord tagged : taggedSequence) {
                    if (first)
                        first = false;
                    else
                        writer.write(' ');
                    writer.write(tagged.word());
                    writer.write('/');
                    writer.write(tagged.tag());
                }
                writer.write('\n');
            }
        }
    }

    private void calculateMemory() {
        double AVAILABLE_MEM_RATIO = 0.35;

        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMem = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMem = (long) (AVAILABLE_MEM_RATIO * totalFreeMem);

        readerMemory = (int) (availableMem / 2);
        writerMemory = (int) (availableMem - readerMemory);

        LOGGER.debug("totalFreeMem = %s", humanReadableByteCount(totalFreeMem));
        LOGGER.debug("availableMem = %s", humanReadableByteCount(availableMem));
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }
}
