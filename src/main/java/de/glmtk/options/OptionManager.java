package de.glmtk.options;

import static com.google.common.collect.Lists.newArrayList;
import static de.glmtk.options.Option.GREATER_ONE;
import static de.glmtk.options.Option.MAX_ONE;
import static de.glmtk.util.StringUtils.join;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.Constants;
import de.glmtk.options.Option.Arg;
import de.glmtk.options.Option.Type;

public class OptionManager {
    private static class OptionWrapper {
        public Option option;
        public List<Arg> args;
        public String argNames;
        public org.apache.commons.cli.Option commonsCliOption;

        public OptionWrapper(Option option) {
            requireNonNull(option);

            this.option = option;
            args = option.arguments();
            boolean hasArgs = args.size() != 0;
            commonsCliOption = new org.apache.commons.cli.Option(
                    option.shortopt, option.longopt, hasArgs, option.desc);

            if (hasArgs) {
                int numArgs = 0;
                StringBuilder argNamesBuilder = new StringBuilder();
                for (Arg arg : args) {
                    if (numArgs == org.apache.commons.cli.Option.UNLIMITED_VALUES)
                        throw new RuntimeException("Only the last option may "
                                + "have an unspecified number of values.");

                    if (numArgs != 0)
                        argNamesBuilder.append("> <");
                    argNamesBuilder.append(arg.name);

                    if (arg.count == GREATER_ONE) {
                        argNamesBuilder.append(MULTIPLE_ARG_SUFFIX);
                        numArgs = org.apache.commons.cli.Option.UNLIMITED_VALUES;
                    } else if (arg.count == MAX_ONE) {
                        argNamesBuilder.append(OPTIONAL_ARG_SUFFIX);
                        numArgs = org.apache.commons.cli.Option.UNLIMITED_VALUES;
                    } else
                        numArgs += arg.count;
                }
                argNames = argNamesBuilder.toString();
                commonsCliOption.setArgName(argNamesBuilder.toString());
                commonsCliOption.setArgs(numArgs);
            }
        }
    }

    private static final String MULTIPLE_ARG_SUFFIX = "...";
    private static final String OPTIONAL_ARG_SUFFIX = "?";

    private List<OptionWrapper> options = newArrayList();
    private List<OptionWrapper> inputArgs = newArrayList();
    /** For sorting options in {@link #help(OutputStream)}. */
    private List<org.apache.commons.cli.Option> commonsCliOptionList = newArrayList();
    private org.apache.commons.cli.Options commonsCliOptions = new org.apache.commons.cli.Options();

    public OptionManager options(Option... options) {
        requireNonNull(options);

        for (Option option : options) {
            option.type = Type.OPTION;
            OptionWrapper optionWrapper = new OptionWrapper(option);

            this.options.add(optionWrapper);
            commonsCliOptionList.add(optionWrapper.commonsCliOption);
            commonsCliOptions.addOption(optionWrapper.commonsCliOption);
        }
        return this;
    }

    // TODO: catch if option is both registered as an option and as an inputArg.
    // TODO: Catch if an argument parses a dynamic number of values that is not the last.
    public OptionManager inputArgs(Option... options) {
        requireNonNull(options);

        for (Option option : options) {
            option.type = Type.INPUT_ARG;
            OptionWrapper optionWrapper = new OptionWrapper(option);

            inputArgs.add(optionWrapper);
        }
        return this;
    }

    /**
     * Will contain leading space if not empty.
     */
    public String getInputArgsLine() {
        StringBuilder sb = new StringBuilder();
        for (OptionWrapper optionWrapper : inputArgs) {
            sb.append(" <");
            sb.append(optionWrapper.argNames);
            sb.append('>');
        }

        return sb.toString();
    }

    public void help(OutputStream outputStream) {
        requireNonNull(outputStream);

        HelpFormatter formatter = new HelpFormatter();
        formatter.setLongOptPrefix(" --");
        formatter.setOptionComparator(new Comparator<org.apache.commons.cli.Option>() {
            @Override
            public int compare(org.apache.commons.cli.Option o1,
                               org.apache.commons.cli.Option o2) {
                return commonsCliOptionList.indexOf(o1)
                        - commonsCliOptionList.indexOf(o2);
            }
        });

        Multimap<String, String> explanations = LinkedHashMultimap.create();
        for (OptionWrapper optionWrapper : options)
            for (Arg arg : optionWrapper.args)
                if (arg.explanation != null)
                    explanations.put(arg.explanation, arg.name);

        // Must not close PrintWriter, as it will also close the outputStream
        // which is not our resource. For Example: We my close System.out
        // and make further writing to stdout.
        @SuppressWarnings("resource")
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream,
                Constants.CHARSET));
        formatter.printOptions(pw, 80, commonsCliOptions, 2, 2);

        for (String explanation : explanations.keySet()) {
            Collection<String> argnames = explanations.get(explanation);
            pw.append('\n').append(format(explanation, join(argnames, ", ")));
        }

        pw.flush();
    }

    public void parse(String[] args) throws OptionException {
        requireNonNull(args);

        CommandLineParser parser = new PosixParser();
        CommandLine line;
        try {
            line = parser.parse(commonsCliOptions, args);
        } catch (ParseException e) {
            throw new OptionException(e.getMessage() + ".");
        }

        parseArgs(line);
        parseOptions(line);
    }

    private void parseArgs(CommandLine line) throws OptionException {
        @SuppressWarnings("unchecked")
        Iterator<String> valuesIter = line.getArgList().iterator();

        for (OptionWrapper optionWrapper : inputArgs) {
            if (!valuesIter.hasNext())
                break;

            for (Arg arg : optionWrapper.args)
                parseOptionArgs(valuesIter, optionWrapper, arg);

            optionWrapper.option.runParse();
        }

        if (valuesIter.hasNext())
            throw new OptionException("Too many input arguments: '%s'.", join(
                    ImmutableList.copyOf(valuesIter), "', '"));
    }

    private void parseOptions(CommandLine line) throws OptionException {
        @SuppressWarnings("unchecked")
        Iterator<org.apache.commons.cli.Option> iter = line.iterator();
        for (org.apache.commons.cli.Option commonsCliOption : ImmutableList.copyOf(iter)) {
            @SuppressWarnings("unchecked")
            List<String> values = commonsCliOption.getValuesList();

            // This lookup is O(n), shouldn't matter though, since we always
            // only have a few options.
            int index = commonsCliOptionList.indexOf(commonsCliOption);

            OptionWrapper optionWrapper = options.get(index);

            Iterator<String> valuesIter = values.iterator();
            for (Arg arg : optionWrapper.args)
                parseOptionArgs(valuesIter, optionWrapper, arg);

            optionWrapper.option.runParse();
        }
    }

    private void parseOptionArgs(Iterator<String> valuesIter,
                                 OptionWrapper optionWrapper,
                                 Arg arg) throws OptionException {
        arg.values = newArrayList();

        try {
            if (arg.count == GREATER_ONE) {
                arg.values.add(valuesIter.next());
                while (valuesIter.hasNext())
                    arg.values.add(valuesIter.next());
            } else if (arg.count == MAX_ONE) {
                if (valuesIter.hasNext())
                    arg.values.add(valuesIter.next());
            } else
                for (int i = 0; i != arg.count; ++i)
                    arg.values.add(valuesIter.next());
        } catch (NoSuchElementException e) {
            throw new OptionException("%s got too few "
                    + "arguments, need to have the form: <%s>",
                    optionWrapper.option, optionWrapper.argNames);
        }

        arg.value = arg.values.isEmpty() ? null : arg.values.get(0);
    }
}
