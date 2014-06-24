package de.typology.executables;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.Config;

public abstract class Executable {

    private static Logger logger = LoggerFactory.getLogger(Executable.class);

    protected Config config;

    protected abstract void exec();

    public void run() {
        try {
            logger.info("Starting " + getClass().getSimpleName() + ".");

            config = Config.get();
            logger.info("Config: " + config);

            exec();

            logger.info("Done.");
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter();
                    PrintWriter stackTraceWriter = new PrintWriter(stackTrace)) {
                e.printStackTrace(stackTraceWriter);
                logger.error("Exception " + stackTrace.toString());
            } catch (IOException e1) {
            }
        }
    }
}
