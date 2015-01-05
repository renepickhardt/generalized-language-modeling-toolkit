package de.glmtk.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {
    public static String getStackTrace(Throwable t) {
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            return "ExceptionUtils.getStackTrace failed: " + e.getMessage();
        }
    }
}
