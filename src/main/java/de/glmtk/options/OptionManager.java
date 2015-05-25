package de.glmtk.options;

import static com.google.common.collect.Lists.newArrayList;
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.Constants;

public class OptionManager {
    private List<Option> optionList = newArrayList();
    private List<org.apache.commons.cli.Option> commonsCliOptionList = newArrayList();
    org.apache.commons.cli.Options commonsCliOptions = new org.apache.commons.cli.Options();

    public OptionManager register(Option... options) {
        for (Option option : options) {
            org.apache.commons.cli.Option commonsCliOption = option.createCommonsCliOption();
            requireNonNull(commonsCliOption);

            optionList.add(option);
            commonsCliOptionList.add(commonsCliOption);
            commonsCliOptions.addOption(commonsCliOption);
        }
        return this;
    }

    public void help(OutputStream outputStream) {
        requireNonNull(outputStream);

        // TODO: Header

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
        for (Option option : optionList) {
            Multimap<String, String> explanation = option.registerExplanation();
            if (explanation != null)
                explanations.putAll(explanation);
        }

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

        @SuppressWarnings("unchecked")
        Iterator<org.apache.commons.cli.Option> iter = line.iterator();
        while (iter.hasNext()) {
            org.apache.commons.cli.Option commonsCliOption = iter.next();

            // This lookup is O(n), shouldn't matter though, since we always
            // only have a few options.
            int index = commonsCliOptionList.indexOf(commonsCliOption);

            optionList.get(index).parse(commonsCliOption);
        }
    }
}
