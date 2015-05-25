package de.glmtk.options;

import static de.glmtk.util.revamp.ListUtils.list;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.glmtk.Constants;

public class OptionManager {
    private List<Option> optionList = list();
    private List<org.apache.commons.cli.Option> commonsCliOptionList = list();
    org.apache.commons.cli.Options commonsCliOptions = new org.apache.commons.cli.Options();

    public OptionManager register(Option... options) {
        for (Option option : options) {
            org.apache.commons.cli.Option commonsCliOption = option.createCommonsCliOption();

            optionList.add(option);
            commonsCliOptionList.add(commonsCliOption);
            commonsCliOptions.addOption(commonsCliOption);
        }
        return this;
    }

    public void help(OutputStream outputStream) {
        // TODO: Header

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream,
                Constants.CHARSET));
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
        formatter.printOptions(pw, 80, commonsCliOptions, 2, 2);
        pw.flush();

        // TODO: Argument explanation
    }

    public void parse(String[] args) throws IOException, OptionException {
        CommandLineParser parser = new PosixParser();
        CommandLine line;
        try {
            line = parser.parse(commonsCliOptions, args);
        } catch (ParseException e) {
            throw new IOException(e);
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
