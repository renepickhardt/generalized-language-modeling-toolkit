package de.typology.executables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.Config;
import de.typology.utils.StringUtils;

public abstract class Executable {

    protected static final String OPTION_HELP = "help";

    protected static final String OPTION_VERSION = "version";

    private static Logger logger = LoggerFactory.getLogger(Executable.class);

    protected Path log;

    protected Config config;

    protected CommandLine line;

    protected abstract List<Option> getOptions();

    protected abstract String getUsage();

    protected abstract String getArgError();

    protected abstract void exec() throws Exception;

    public void run(String[] args) {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = format.format(Calendar.getInstance().getTime());
            log = Paths.get("logs/" + time + ".log");
            config = Config.get();

            if (parseArguments(args)) {
                return;
            }

            printLogHeader(args);
            exec();
            printLogFooter();
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter();
                    PrintWriter stackTraceWriter = new PrintWriter(stackTrace)) {
                e.printStackTrace(stackTraceWriter);
                logger.error("Exception " + stackTrace.toString());
            } catch (IOException ee) {
            }
        }
    }

    /**
     * @return If true, program should terminate.
     */
    private boolean parseArguments(String[] args) {
        Options options = new Options();
        for (Option option : getOptions()) {
            options.addOption(option);
        }

        try {
            CommandLineParser parser = new PosixParser();
            line = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            return true;
        }

        if (line.hasOption(OPTION_VERSION)) {
            System.out
                    .println("GLMTK (generalized language modeling toolkit) version 0.1.");
            return true;
        }

        if (line.hasOption(OPTION_HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setSyntaxPrefix("Usage: ");
            formatter.setWidth(80);
            formatter.setOptionComparator(new Comparator<Option>() {

                @Override
                public int compare(Option o1, Option o2) {
                    return getOptions().indexOf(o1) - getOptions().indexOf(o2);
                }

            });
            formatter.printHelp(getUsage(), options);
            return true;
        }

        if (line.getArgs() == null || line.getArgs().length == 0) {
            System.err.println(getArgError());
            return true;
        }

        return false;
    }

    private void printLogHeader(String[] args) throws IOException,
            InterruptedException {
        logger.info(StringUtils.repeat("=", 80));
        logger.info(getClass().getSimpleName());

        logger.info(StringUtils.repeat("-", 80));

        // log git commit
        Process gitLogProc = Runtime.getRuntime().exec(new String[] {
            "git", "log", "-1", "--format=%H: %s"
        });
        gitLogProc.waitFor();
        try (BufferedReader gitLogReader =
                new BufferedReader(new InputStreamReader(
                        gitLogProc.getInputStream()))) {
            String gitCommit = gitLogReader.readLine();
            logger.info("Git Commit: " + gitCommit);
        }

        // log Arguments
        logger.info("Arguments: " + StringUtils.join(args, " "));

        // log config
        logger.info("Config: " + config);

        logger.info(StringUtils.repeat("-", 80));
    }

    private void printLogFooter() {
        logger.info("Done.");
    }

}
