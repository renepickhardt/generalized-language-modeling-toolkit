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

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.Config;

public abstract class Executable {

    private static Logger logger = LoggerFactory.getLogger(Executable.class);

    protected Config config;

    protected Path log;

    protected abstract void exec(CommandLine line);

    public void run(CommandLine line) {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = format.format(Calendar.getInstance().getTime());
            log = Paths.get("logs/" + time + ".log");

            logger.info("Starting " + getClass().getSimpleName() + ".");

            // log Git Commit
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

            // log Config
            config = Config.get();
            logger.info("Config: " + config);

            exec(line);

            logger.info("Done.");
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter();
                    PrintWriter stackTraceWriter = new PrintWriter(stackTrace)) {
                e.printStackTrace(stackTraceWriter);
                logger.error("Exception " + stackTrace.toString());
            } catch (IOException ee) {
            }
        }
    }
}
