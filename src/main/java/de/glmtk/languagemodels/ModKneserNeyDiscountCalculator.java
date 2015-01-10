package de.glmtk.languagemodels;

import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.Status;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class ModKneserNeyDiscountCalculator {

    @SuppressWarnings("unused")
    private Config config;

    public ModKneserNeyDiscountCalculator(Config config) {
        this.config = config;
    }

    public void calculateDiscounts(Status status,
                                   Path nGramTimesFile,
                                   Path outputFile) throws IOException {
        OUTPUT.setPhase(Phase.CALCULATING_DISCOUNTS);

        // TODO: if status is done, return

        Progress progress = OUTPUT.newProgress(NioUtils.calcNumberOfLines(nGramTimesFile));

        try (BufferedReader reader = Files.newBufferedReader(nGramTimesFile,
                Constants.CHARSET);
                BufferedWriter writer = Files.newBufferedWriter(outputFile,
                        Constants.CHARSET)) {
            int lineNo = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                ++lineNo;

                List<String> split = StringUtils.splitAtChar(line, '\t');
                if (split.size() != 5)
                    throw new FileFormatException(line, lineNo, nGramTimesFile,
                            "ngram times",
                            "Expected line to have format '<pattern> <count> <count> <count> <count>'.");

                Pattern pattern = Patterns.get(split.get(0));
                long[] nGramTimesCounts = new long[4];
                for (int i = 0; i != 4; ++i)
                    nGramTimesCounts[i] = Long.parseLong(split.get(i + 1));

                double[] discounts = calcDiscounts(nGramTimesCounts);

                writer.append(pattern.toString());
                for (int i = 0; i != 3; ++i)
                    writer.append('\t').append(Double.toString(discounts[i]));
                writer.append('\n');

                progress.increase(1);
            }
        }
    }

    private double[] calcDiscounts(long[] nGramTimesCounts) {
        double[] discounts = new double[3];

        double y = (double) nGramTimesCounts[0]
                / (nGramTimesCounts[0] + nGramTimesCounts[1]);

        discounts[0] = 1.0 - 2.0 * y * nGramTimesCounts[1]
                / nGramTimesCounts[0];
        discounts[1] = 2.0 - 3.0 * y * nGramTimesCounts[2]
                / nGramTimesCounts[1];
        discounts[2] = 3.0 - 4.0 * y * nGramTimesCounts[3]
                / nGramTimesCounts[2];

        return discounts;
    }
}
