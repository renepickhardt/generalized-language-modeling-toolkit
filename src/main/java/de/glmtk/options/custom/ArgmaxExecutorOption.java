package de.glmtk.options.custom;

import static de.glmtk.util.Maps.maxKeyLength;
import static de.glmtk.util.StringUtils.join;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

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
        executorString = executorString.toUpperCase();
        if (!VALUES.containsKey(executorString))
            throw new OptionException("Option %s argmax executor not "
                    + "recognized: '%s'. Valid Values: %s.", option,
                    executorString, join(VALUES.keySet(), ", "));
        return executorString;
    }

    private String argname;
    private String value = null;

    public ArgmaxExecutorOption(String shortopt,
                                String longopt,
                                String desc) {
        this(shortopt, longopt, desc, DEFAULT_ARGNAME);
    }

    public ArgmaxExecutorOption(String shortopt,
                                String longopt,
                                String desc,
                                String argname) {
        super(shortopt, longopt, desc);

        requireNonNull(argname);

        this.argname = argname;
    }

    public ArgmaxExecutorOption defaultValue(String defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected Multimap<String, String> registerExplanation() {
        return ImmutableMultimap.of(EXPLANATION, argname);
    }

    @Override
    protected org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(argname);
        commonsCliOption.setArgs(1);
        return commonsCliOption;
    }

    @Override
    protected void handleParse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        checkOnlyDefinedOnce();

        value = parseArgmaxExecutor(commonsCliOption.getValue(), this);
    }

    public String getArgmaxExecutor() {
        return value;
    }

    public static ArgmaxQueryExecutor argmaxQueryExecutorFromString(String executor,
                                                                    WeightedSumEstimator estimator,
                                                                    Cache randomAccessCache,
                                                                    CompletionTrieCache sortedAccessCache) {
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
