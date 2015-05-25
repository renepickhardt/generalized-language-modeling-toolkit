package de.glmtk.options.custom;

import static de.glmtk.util.Maps.maxKeyLength;
import static de.glmtk.util.StringUtils.join;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;
import de.glmtk.querying.argmax.BeamSearchArgmaxQueryExecutor;
import de.glmtk.querying.argmax.NoRandomAccessArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ThresholdArgmaxQueryExecutor;
import de.glmtk.querying.argmax.TrivialArgmaxQueryExecutor;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;

public class ArgmaxExecutorOption extends Option {
    public static final String DEFAULT_ARGNAME = "ARGMAX_EXECUTOR";

    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    static {
        VALUES.put("TA", "TopK Treshold Algorithm");
        VALUES.put("NRA", "TopK No Random Access");
        VALUES.put("BEAM", "Beam Search");
        VALUES.put("SMPL", "Trivial");
    }

    protected static final String EXPLANATION;
    static {
        int longestAbbr = maxKeyLength(VALUES);

        StringBuilder sb = new StringBuilder();
        sb.append("Where <%s> may be any of:\n");
        for (Entry<String, String> executor : VALUES.entrySet())
            sb.append(format("  * %-" + longestAbbr + "s - %s\n",
                    executor.getKey(), executor.getValue()));

        EXPLANATION = sb.toString();
    }

    public static final String parseArgmaxExecutor(String executorString,
                                                   Option option) throws OptionException {
        requireNonNull(executorString);
        requireNonNull(option);

        executorString = executorString.toUpperCase();
        if (!VALUES.containsKey(executorString))
            throw new OptionException("Option %s argmax executor not "
                    + "recognized: '%s'. Valid Values: %s.", option,
                    executorString, join(VALUES.keySet(), ", "));
        return executorString;
    }

    private Arg arg = new Arg(DEFAULT_ARGNAME, 1, EXPLANATION);
    private String value = null;

    public ArgmaxExecutorOption(String shortopt,
                                String longopt,
                                String desc) {
        super(shortopt, longopt, desc);
    }

    public ArgmaxExecutorOption argName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    public ArgmaxExecutorOption setArgName(String argName) {
        requireNonNull(argName);
        arg.name = argName;
        return this;
    }

    public ArgmaxExecutorOption defaultValue(String defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(arg);
    }

    @Override
    protected void parse() throws OptionException {
        value = parseArgmaxExecutor(arg.values.get(0), this);
    }

    public String getArgmaxExecutor() {
        return value;
    }

    public static ArgmaxQueryExecutor argmaxQueryExecutorFromString(String executor,
                                                                    WeightedSumEstimator estimator,
                                                                    Cache randomAccessCache,
                                                                    CompletionTrieCache sortedAccessCache) {
        requireNonNull(executor);
        requireNonNull(estimator);

        switch (executor) {
            case "TA":
                return new ThresholdArgmaxQueryExecutor(estimator,
                        randomAccessCache, sortedAccessCache);

            case "NRA":
                return new NoRandomAccessArgmaxQueryExecutor(estimator,
                        sortedAccessCache);

            case "BEAM":
                return new BeamSearchArgmaxQueryExecutor(estimator,
                        sortedAccessCache);

            case "SMPL":
                return new TrivialArgmaxQueryExecutor(estimator,
                        randomAccessCache);

            default:
                throw new SwitchCaseNotImplementedException();
        }
    }
}
