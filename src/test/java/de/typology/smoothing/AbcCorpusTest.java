package de.typology.smoothing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;

import de.typology.counting.AbsoluteCounter;
import de.typology.counting.ContinuationCounter;
import de.typology.indexing.WordIndex;
import de.typology.indexing.WordIndexBuilder;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.sequencing.Sequencer;

public class AbcCorpusTest {

    protected static Path abcDir;

    protected static WordIndex abcWordIndex;

    protected static Path abcAbsoluteDir;

    protected static Path abcContinuationDir;

    @BeforeClass
    public static void setUpAbcCorpus() throws IOException,
            InterruptedException {
        Path resourcesDir = Paths.get("src/test/resources");

        Path abcTaggedFile = resourcesDir.resolve("abc_tagged.txt");

        abcDir = resourcesDir.resolve("abc");
        if (!Files.exists(abcDir)) {
            Files.createDirectory(abcDir);
        }

        Path abcWordIndexFile = abcDir.resolve("index.txt");
        Path abcSequencesDir = abcDir.resolve("sequences");
        abcAbsoluteDir = abcDir.resolve("absolute");
        abcContinuationDir = abcDir.resolve("continuation");

        // index
        if (!Files.exists(abcWordIndexFile)) {
            try (InputStream input = Files.newInputStream(abcTaggedFile);
                    OutputStream output =
                            Files.newOutputStream(abcWordIndexFile)) {
                WordIndexBuilder wordIndexBuilder = new WordIndexBuilder();
                wordIndexBuilder.buildIndex(input, output, 1, "", "");
            }
        }
        try (InputStream input = Files.newInputStream(abcWordIndexFile)) {
            abcWordIndex = new WordIndex(input);
        }

        // sequences
        if (!Files.exists(abcSequencesDir)) {
            Sequencer sequencer =
                    new Sequencer(abcTaggedFile, abcSequencesDir, abcWordIndex,
                            1, false);
            sequencer.sequence(Pattern.getCombinations(5, new PatternElem[] {
                PatternElem.CNT, PatternElem.SKP, PatternElem.POS
            }));
        }

        // absolute
        if (!Files.exists(abcAbsoluteDir)) {
            AbsoluteCounter absoluteCounter =
                    new AbsoluteCounter(abcSequencesDir, abcAbsoluteDir, "\t",
                            1, false, true);
            absoluteCounter.count();
        }

        // continuation
        if (!Files.exists(abcContinuationDir)) {
            ContinuationCounter continuationCounter =
                    new ContinuationCounter(abcAbsoluteDir, abcContinuationDir,
                            abcWordIndex, "\t", 1, true);
            continuationCounter.count();
        }
    }

    public static String getAbcSequence(int num, int length) {
        StringBuilder result = new StringBuilder();

        boolean frist = true;
        for (int k = 0; k != length; ++k) {
            if (frist) {
                frist = false;
            } else {
                result.append(" ");
            }
            switch (num % 3) {
                case 0:
                    result.append("a");
                    break;
                case 1:
                    result.append("b");
                    break;
                case 2:
                    result.append("c");
                    break;
            }
            num /= 3;
        }

        return result.reverse().toString();
    }

    public static MaximumLikelihoodSmoother newMaximumLikelihoodSmoother()
            throws IOException {
        return new MaximumLikelihoodSmoother(abcAbsoluteDir,
                abcContinuationDir, "\t");
    }

    public static DiscountSmoother newDiscountSmoother(double absoluteDiscount)
            throws IOException {
        return new DiscountSmoother(abcAbsoluteDir, abcContinuationDir, "\t",
                absoluteDiscount);
    }

    public static PropabilityCond2Smoother newPropabilityCond2Smoother(
            double absoluteDiscount) throws IOException {
        return new PropabilityCond2Smoother(abcAbsoluteDir, abcContinuationDir,
                "\t", absoluteDiscount);
    }

    public static InterpolatedKneserNeySmoother
        newInterpolatedKneserNeySmoother() throws IOException {
        return new InterpolatedKneserNeySmoother(abcAbsoluteDir,
                abcContinuationDir, "\t");
    }

}
