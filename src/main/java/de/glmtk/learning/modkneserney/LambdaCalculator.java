package de.glmtk.learning.modkneserney;

import static de.glmtk.Constants.MODEL_MODKNESERNEY;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.SKP_WORD;
import static de.glmtk.common.PatternElem.WSKP;
import static de.glmtk.common.PatternElem.WSKP_WORD;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.AbstractWorkerPriorityExecutor;
import de.glmtk.common.Cache;
import de.glmtk.common.CacheBuilder;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;
import de.glmtk.counts.LambdaCount;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.files.CountsReader;
import de.glmtk.files.LambdaCountsReader;
import de.glmtk.files.LambdaCountsWriter;
import de.glmtk.logging.Logger;
import de.glmtk.util.StringUtils;

public class LambdaCalculator extends AbstractWorkerPriorityExecutor<Pattern> {
    private static final Logger LOGGER = Logger.get(LambdaCalculator.class);

    private static Set<Pattern> filterPatterns(Collection<Pattern> counted,
            Collection<Pattern> patterns) {
        Set<Pattern> result = new HashSet<>();
        for (Pattern contPattern : patterns) {
            if (contPattern.get(contPattern.size() - 1) != WSKP
                    || contPattern.size() == 1
                    || !getHistPattern(contPattern).containsOnly(CNT)
                    || !counted.contains(contPattern)
                    || !counted.contains(getAbsPattern(contPattern))
                    || (contPattern.size() != 2 && !counted.contains(getHistLambdaPattern(contPattern))))
                continue;

            result.add(contPattern);
        }
        return result;
    }

    private static Pattern getAbsPattern(Pattern contPattern) {
        return contPattern.set(contPattern.size() - 1, SKP);
    }

    private static Pattern getHistPattern(Pattern contPattern) {
        return contPattern.range(0, contPattern.size() - 1);
    }

    private static Pattern getHistLambdaPattern(Pattern contPattern) {
        return contPattern.range(0, contPattern.size() - 2).concat(WSKP);
    }

    private class Worker extends AbstractWorkerPriorityExecutor<Pattern>.Worker {
        @Override
        protected void work(Pattern contPattern,
                            int patternNo) throws Exception {
            Pattern absPattern = getAbsPattern(contPattern);
            Pattern histPattern = getHistPattern(contPattern);
            Pattern histLambdaPattern = getHistLambdaPattern(contPattern);

            LOGGER.trace("contPattern       : %s", contPattern);
            LOGGER.trace("absPattern        : %s", absPattern);
            LOGGER.trace("histPattern       : %s", histPattern);
            LOGGER.trace("histLambdaPattern : %s", histLambdaPattern);

            Path contCountFile = continuationDir.resolve(contPattern.toString());
            Path absCountFile = absoluteDir.resolve(absPattern.toString());
            Path histCountFile = absoluteDir.resolve(histPattern.toString());
            Path histLambdaFile = lambdaDir.resolve(histLambdaPattern.toString());

            Path lambdaFile = lambdaDir.resolve(contPattern.toString());

            boolean checkHistLambda = histLambdaPattern.size() > 1;

            Discount discount = cache.getDiscount(Constants.MODEL_MODKNESERNEY,
                    histPattern);

            try (CountsReader contReader = new CountsReader(contCountFile,
                    Constants.CHARSET, readerMemory / 4);
                    CountsReader absReader = new CountsReader(absCountFile,
                            Constants.CHARSET, readerMemory / 4);
                    CountsReader histReader = new CountsReader(histCountFile,
                            Constants.CHARSET, readerMemory / 4);
                    LambdaCountsReader histLambdaReader = !checkHistLambda
                            ? null
                                    : new LambdaCountsReader(histLambdaFile,
                                            Constants.CHARSET, readerMemory / 4);
                    LambdaCountsWriter writer = new LambdaCountsWriter(
                            lambdaFile, Constants.CHARSET, writerMemory)) {
                while (contReader.readLine() != null) {
                    String contSequence = contReader.getSequence();
                    List<String> split = StringUtils.splitAtChar(contSequence,
                            ' ');

                    String histSequence = StringUtils.join(split.subList(0,
                            split.size() - 1), ' ');
                    String absSequence = histSequence + " " + SKP_WORD;

                    absReader.forwardToSequence(absSequence);
                    histReader.forwardToSequence(histSequence);

                    Counts cont = contReader.getCounts();

                    long absDen = absReader.getCount();
                    long contDen = contReader.getCount();

                    double gammaNum = discount.getOne() * cont.getOneCount()
                            + discount.getTwo() * cont.getTwoCount()
                            + discount.getThree() * cont.getThreePlusCount();
                    double gammaHigh = gammaNum / absDen;
                    double gammaLow = gammaNum / contDen;

                    LOGGER.trace("contSequence : %s", contSequence);
                    LOGGER.trace("absSequence  : %s", absSequence);
                    LOGGER.trace("histSequence : %s", histSequence);

                    LambdaCounts histLambdas = new LambdaCounts();
                    LambdaCount histLambda = new LambdaCount(1.0, 1.0);
                    if (checkHistLambda) {
                        String histLambdaSequence = StringUtils.join(
                                split.subList(0, split.size() - 2), ' ')
                                + " " + WSKP_WORD;
                        LOGGER.trace("histLambdaSequence : %s",
                                histLambdaSequence);
                        histLambdaReader.forwardToSequence(histLambdaSequence);
                        histLambdas = histLambdaReader.getLambdaCounts();
                        histLambda = histLambdas.get(0);
                    }

                    LambdaCount lambda = new LambdaCount(gammaHigh
                            * histLambda.getLow(), gammaLow
                            * histLambda.getLow());
                    LambdaCounts lambdas = new LambdaCounts();
                    lambdas.append(lambda);
                    for (LambdaCount l : histLambdas)
                        lambdas.append(l);

                    writer.append(contSequence, lambdas);
                }
            }

            status.addLambda(MODEL_MODKNESERNEY, contPattern);
        }
    }

    private Path absoluteDir;
    private Path continuationDir;
    private Path lambdaDir;
    private Status status;
    private Cache cache;

    public LambdaCalculator(Config config) {
        super(config);
    }

    public void run(Collection<Pattern> patterns,
                    GlmtkPaths paths,
                    Status status) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_LAMBDAS);

        absoluteDir = paths.getAbsoluteDir();
        continuationDir = paths.getContinuationDir();
        lambdaDir = paths.getModKneserNeyLambdaDir();

        this.status = status;

        cache = new CacheBuilder(paths).withDiscounts(MODEL_MODKNESERNEY).build();

        patterns = filterPatterns(status.getCounted(), patterns);
        patterns.removeAll(status.getLambdas(MODEL_MODKNESERNEY));

        Files.createDirectories(lambdaDir);

        work(patterns);
    }

    @Override
    protected Collection<Worker> createWorkers() {
        // Can't really parallelize MKN lambda calculation.
        return Arrays.asList(new Worker());
    }
}
