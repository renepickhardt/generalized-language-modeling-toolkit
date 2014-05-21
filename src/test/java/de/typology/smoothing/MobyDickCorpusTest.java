package de.typology.smoothing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;

import de.typology.counting.AbsoluteCounter;
import de.typology.counting.ContinuationCounter;
import de.typology.indexing.Index;
import de.typology.indexing.IndexBuilder;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.sequencing.Sequencer;
import de.typology.smoothing.legacy.DiscountSmoother;
import de.typology.smoothing.legacy.InterpolatedKneserNeySmoother;
import de.typology.smoothing.legacy.MaximumLikelihoodSmoother;
import de.typology.smoothing.legacy.PropabilityCond2Smoother;
import de.typology.utils.StringUtils;

public class MobyDickCorpusTest {

    protected static Path mobydickbDir;

    protected static Index mobydickWordIndex;

    protected static Path mobydickAbsoluteDir;

    protected static Path mobydickContinuationDir;

    @BeforeClass
    public static void setUpMobyDickCorpus() throws IOException,
            InterruptedException {
        Path resourcesDir = Paths.get("src/test/resources");

        Path mobydickTrainingFile = resourcesDir.resolve("mobydick.txt");

        mobydickbDir = resourcesDir.resolve("mobydick");
        if (!Files.exists(mobydickbDir)) {
            Files.createDirectory(mobydickbDir);
        }

        Path mobydickWordIndexFile = mobydickbDir.resolve("index.txt");
        Path mobydickSequencesDir = mobydickbDir.resolve("sequences");
        mobydickAbsoluteDir = mobydickbDir.resolve("absolute");
        mobydickContinuationDir = mobydickbDir.resolve("continuation");

        // index
        if (!Files.exists(mobydickWordIndexFile)) {
            try (InputStream input = Files.newInputStream(mobydickTrainingFile);
                    OutputStream output =
                            Files.newOutputStream(mobydickWordIndexFile)) {
                IndexBuilder indexBuilder = new IndexBuilder(false, false, 5);
                indexBuilder.buildIndex(input, output, 1, 1);
            }
        }
        try (InputStream input = Files.newInputStream(mobydickWordIndexFile)) {
            mobydickWordIndex = new Index(input);
        }

        // sequences
        if (!Files.exists(mobydickSequencesDir)) {
            Sequencer sequencer =
                    new Sequencer(mobydickTrainingFile, mobydickSequencesDir,
                            mobydickWordIndex, 1, false, false);
            sequencer.sequence(Pattern.getCombinations(5, new PatternElem[] {
                PatternElem.CNT, PatternElem.SKP
            }));
        }

        // absolute
        if (!Files.exists(mobydickAbsoluteDir)) {
            AbsoluteCounter absoluteCounter =
                    new AbsoluteCounter(mobydickSequencesDir,
                            mobydickAbsoluteDir, "\t", 1, false, true);
            absoluteCounter.count();
        }

        // continuation
        if (!Files.exists(mobydickContinuationDir)) {
            ContinuationCounter continuationCounter =
                    new ContinuationCounter(mobydickAbsoluteDir,
                            mobydickContinuationDir, mobydickWordIndex, "\t",
                            1, false, true);
            continuationCounter.count();
        }
    }

    public static String getMobyDickSequence(int num, int length) {
        String[] words =
                {
                    "A", "BOOK", "BY", "CHER", "DICK", "DIFFERENT", "JOHN",
                    "MARY", "MOBY", "READ", "SHE"
                };

        List<String> result = new LinkedList<String>();
        for (int k = 0; k != length; ++k) {
            result.add(words[num % words.length]);
            num /= words.length;
        }

        return StringUtils.join(result, " ");
    }

    public static MaximumLikelihoodSmoother newMaximumLikelihoodSmoother()
            throws IOException {
        return new MaximumLikelihoodSmoother(mobydickAbsoluteDir,
                mobydickContinuationDir, "\t");
    }

    public static DiscountSmoother newDiscountSmoother(double absoluteDiscount)
            throws IOException {
        return new DiscountSmoother(mobydickAbsoluteDir,
                mobydickContinuationDir, "\t", absoluteDiscount);
    }

    public static PropabilityCond2Smoother newPropabilityCond2Smoother(
            double absoluteDiscount) throws IOException {
        return new PropabilityCond2Smoother(mobydickAbsoluteDir,
                mobydickContinuationDir, "\t", absoluteDiscount);
    }

    public static InterpolatedKneserNeySmoother
        newInterpolatedKneserNeySmoother() throws IOException {
        return new InterpolatedKneserNeySmoother(mobydickAbsoluteDir,
                mobydickContinuationDir, "\t");
    }

}
