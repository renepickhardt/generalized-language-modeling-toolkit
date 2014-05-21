package de.typology.executables;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.smoothing.Corpus;
import de.typology.smoothing.DeleteCalculator;
import de.typology.smoothing.FalseMaximumLikelihoodEstimator;
import de.typology.smoothing.MaximumLikelihoodEstimator;
import de.typology.smoothing.PropabilityCalculator;
import de.typology.smoothing.SkipCalculator;

public class EntropyPrinter {

    public static int LOG_BASE = 10;

    private static Logger logger = LoggerFactory
            .getLogger(EntropyPrinter.class);

    private Path testingSample;

    private Corpus corpus;

    public EntropyPrinter(
            Path workingDir,
            Path absoluteDir,
            Path continuationDir,
            Path testingSample) throws IOException {
        super();
        this.testingSample = testingSample;

        corpus = new Corpus(absoluteDir, continuationDir, "\t");
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public void printEntropy(PropabilityCalculator calculator)
            throws IOException {
        try (BufferedReader reader =
                Files.newBufferedReader(testingSample, Charset.defaultCharset())) {
            double entropy = 0;
            int cntZero = 0;
            int cntNonZero = 0;
            double logBase = Math.log(LOG_BASE);

            String sequence;
            while ((sequence = reader.readLine()) != null) {
                double propability = calculator.propability(sequence);

                if (propability == 0) {
                    ++cntZero;
                } else {
                    ++cntNonZero;
                    entropy -= Math.log(propability) / logBase;
                    logger.info(sequence + " -> " + propability);
                }
            }

            entropy /= cntNonZero;
            logger.info("entropy = " + entropy + " ; cntZero = " + cntZero
                    + getPercent((double) cntZero / (cntZero + cntNonZero))
                    + ") ; cntNonZero = " + cntNonZero
                    + getPercent((double) cntNonZero / (cntZero + cntNonZero)));
        }
    }

    private String getPercent(double percent) {
        return " (" + String.format("%.2f", percent * 100) + "%)";
    }

    public static void main(String[] args) throws IOException {
        Path workingDir =
                Paths.get("/home/lukas/Documents/langmodels/data/en0008t");
        Path absoluteDir = workingDir.resolve("absolute");
        Path continuationDir = workingDir.resolve("continuation");
        Path testingSample = workingDir.resolve("testing-samples-1.txt");
        EntropyPrinter entropyPrinter =
                new EntropyPrinter(workingDir, absoluteDir, continuationDir,
                        testingSample);

        MaximumLikelihoodEstimator mle =
                new MaximumLikelihoodEstimator(entropyPrinter.getCorpus());
        FalseMaximumLikelihoodEstimator fmle =
                new FalseMaximumLikelihoodEstimator(entropyPrinter.getCorpus());

        logger.info("=== SkipMle ============================================");
        SkipCalculator skipMle = new SkipCalculator(mle);
        entropyPrinter.printEntropy(skipMle);

        logger.info("=== DeleteMle ==========================================");
        DeleteCalculator deleteMle = new DeleteCalculator(mle);
        entropyPrinter.printEntropy(deleteMle);

        logger.info("=== SkipFmle ===========================================");
        SkipCalculator skipFmle = new SkipCalculator(fmle);
        entropyPrinter.printEntropy(skipFmle);

    }
}
