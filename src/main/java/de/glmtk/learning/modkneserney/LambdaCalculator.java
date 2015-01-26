package de.glmtk.learning.modkneserney;

import static de.glmtk.Constants.MODEL_MODKNESERNEY;
import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.common.Pattern.WSKP_PATTERN;
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.AbstractWorkerExecutor;
import de.glmtk.common.BackoffMode;
import de.glmtk.common.Config;
import de.glmtk.common.NGram;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.common.Status;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.files.LambdaCountsWriter;
import de.glmtk.files.SequenceReader;
import de.glmtk.util.StringUtils;

public class LambdaCalculator extends AbstractWorkerExecutor<Pattern> {
    private class Worker extends AbstractWorkerExecutor<Pattern>.Worker {
        @Override
        protected void work(Pattern pattern,
                            int patternNo) throws IOException {
            int trailingSkpLength = (" " + PatternElem.SKP_WORD).length();

            Path sequenceFile = absoluteDir.resolve(pattern.concat(SKP).toString());
            Path lambdaFile = lambdaDir.resolve(pattern.toString());
            try (SequenceReader reader = new SequenceReader(sequenceFile,
                    Constants.CHARSET);
                    LambdaCountsWriter writer = new LambdaCountsWriter(
                            lambdaFile, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequenceString = reader.getSequence();
                    sequenceString = sequenceString.substring(0,
                            sequenceString.length() - trailingSkpLength);

                    NGram sequence = new NGram(StringUtils.split(
                            sequenceString, ' '));
                    int sequenceOrder = sequence.size();

                    LambdaCounts lambdas = new LambdaCounts();
                    lambdas.append(calcGamma(sequence, true));
                    for (int i = 1; i != sequenceOrder; ++i) {
                        sequence = sequence.backoff(BackoffMode.DEL);
                        double gamma = calcGamma(sequence, false);
                        double lambda = gamma * lambdas.get(i - 1);
                        lambdas.append(lambda);
                    }

                    writer.append(sequenceString, lambdas);
                }
            }

            status.addLambda(MODEL_MODKNESERNEY, pattern);
        }

        private double calcGamma(NGram sequence,
                                 boolean highestOrder) {
            Discount discount = cache.getDiscount(MODEL_MODKNESERNEY,
                    sequence.getPattern());
            Counts contCount = cache.getContinuation(sequence.concat(WSKP_NGRAM));

            long denominator;
            if (highestOrder)
                denominator = cache.getAbsolute(sequence.concat(SKP_NGRAM));
            else
                denominator = cache.getContinuation(
                        WSKP_NGRAM.concat(sequence).concat(WSKP_NGRAM)).getOnePlusCount();

            return (discount.getOne() * contCount.getOneCount()
                    + discount.getTwo() * contCount.getTwoCount() + discount.getThree()
                    * contCount.getThreePlusCount())
                    / denominator;
        }
    }

    private Path absoluteDir;
    private Path lambdaDir;
    private Status status;
    private Cache cache;

    public LambdaCalculator(Config config) {
        super(config);
    }

    public void run(int order,
                    GlmtkPaths paths,
                    Status status) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_LAMBDAS);

        absoluteDir = paths.getAbsoluteDir();
        lambdaDir = paths.getModKneserNeyLambdaDir();

        this.status = status;

        Set<Pattern> countsPatterns = new HashSet<>();
        Set<Pattern> lambdaPatterns = new HashSet<>();
        Pattern pattern = Patterns.get();
        for (int i = 0; i != order - 1; ++i) {
            pattern = pattern.concat(CNT);

            // pattern to calculate lambdas for
            lambdaPatterns.add(pattern);

            // pattern to get continuation count
            countsPatterns.add(pattern.concat(WSKP));
            // pattern to get highest order denominator
            countsPatterns.add(pattern.concat(SKP));
            if (i != order - 2)
                // pattern to get lower orders denominator
                countsPatterns.add(WSKP_PATTERN.concat(pattern).concat(WSKP));
        }
        lambdaPatterns.removeAll(status.getLambdas(MODEL_MODKNESERNEY));

        cache = new CacheBuilder().withCounts(countsPatterns).withDiscounts(
                MODEL_MODKNESERNEY).build(paths);

        Files.createDirectories(lambdaDir);

        work(lambdaPatterns);
    }

    @Override
    protected Collection<Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }
}
