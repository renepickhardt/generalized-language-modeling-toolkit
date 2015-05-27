package de.glmtk.output;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static de.glmtk.util.Files.newBufferedReader;
import static de.glmtk.util.ReflectionUtils.setFinalStaticField;
import static de.glmtk.util.StringUtils.splitSparse;
import static de.glmtk.util.ThreadUtils.executeProcess;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.Math.ceil;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiOutputStream;
import org.fusesource.jansi.WindowsAnsiOutputStream;

import com.google.common.base.Optional;

import de.glmtk.Constants;

/**
 * Utility for formatting terminal output to stderr.
 *
 * <p>
 * Many terminal emulators support more than just printing out line after line.
 * For example they can print messages in bold, or delete the previous line, to
 * enable effects like progress bars. Using these capabilities in a good way can
 * significantly enhance user experience. However the API for formatting
 * terminal output is quite cryptic, this class serves as a convenience wrapper.
 *
 * <p>
 * It is a good practice to split program output:
 * <ul>
 * <li>Program results should be printed to stdout.
 * <li>Program status messages should be printed to stderr.
 * </ul>
 *
 * <p>
 * Following this convention is important, because it allows to do stuff like
 * piping program results to a file (redirecting stdout), while still seeing
 * status messages on the console, for example warnings about incorrect
 * arguments. Because stdout can be redirected or possibly be parsed by other
 * programs, one should not format output to stdout. Further formatting stderr
 * output is only valid if stderr is printed to a terminal, and not itself
 * redirected somewhere.
 *
 * This means:
 * <ul>
 * <li>If you want to output computed program results, output them plainly with
 * {@code System.out.println()} and related functions.
 * <li>If you want to output program status messages, print these to stderr
 * using only the methods in this class. You may further format the output using
 * the convenience methods in this class.
 * </ul>
 *
 * <p>
 * This class is internally implemented as a wrapper on top of <a
 * href="https://github.com/fusesource/jansi">Jansi</a>. However its behaviour
 * differs from our use case:
 */
public class Output {
    private Output() {
    }

    private static final long TERMINAL_WIDTH_UPDATE_INTERVALL = 500;
    private static final long TERMINAL_WDITH_UPDATE_TIME = 10;

    private static boolean isFormattingEnabled = false;
    private static int terminalWidth = 80;
    private static long lastTerminalWidthCheck = 0;

    /**
     * Count of volatile line printed with {@link #printlnVolatile}, since last
     * non-volatile print {@link #print}.
     *
     * <ul>
     * <li>Reset by {@link #print}.
     * <li>Increased by {@link #printlnVolatile}.
     * <li>Decreased by {@link #eraseLine()}.
     * </ul>
     */
    private static int numVolatileLines = 0;

    /**
     * Try to enable output formatting on stderr.
     *
     * <p>
     * If initializing formatting fails, all formatting methods in this class
     * will become no-ops, so they can always safely be used.
     *
     * <p>
     * Jansi will always try to wrap {@link System#out} and {@link System#err}
     * with its own formatting output stream. However for both streams it will
     * only work if stdout is actually printed to a terminal.
     *
     * <p>
     * We instead never want to wrap stdout, and only wrap stderr, if stderr is
     * a terminal. Since Java has no capability to check whether an output
     * stream prints to a terminal or not, we rely on the environment variable
     * {@code glmtk.isttyStderr} to specify whether we should enable output
     * formatting or not.
     *
     * @return If successful, returns {@link Optional#absent()}, if not will
     *         return a string describing the error wrapped in
     *         {@link Optional#of(Object)}.
     */
    public static Optional<String> initializeStderrFormatting() {
        boolean isttyStderr = parseBoolean(System.getProperty("glmtk.isttyStderr"));
        if (!isttyStderr)
            return Optional.of("Ansi codes will not be enables because "
                    + "ISTTY_STDERR is 'false'.");

        try {
            setFinalStaticField(AnsiConsole.class.getField("out"),
                    AnsiConsole.system_out);
            setFinalStaticField(AnsiConsole.class.getField("err"),
                    new PrintStream(wrapStderrStream(AnsiConsole.system_err)));
            AnsiConsole.systemInstall();
            isFormattingEnabled = true;
            return Optional.absent();
        } catch (Throwable e) {
            return Optional.of("Ansi codes could not be enables because: "
                    + getStackTraceAsString(e));
        }
    }

    /**
     * OS-dependently tries to wrap given {@code stream} with a formatting
     * stream.
     *
     * <p>
     * Implemented like Jansi's
     * {@link AnsiConsole#wrapOutputStream(OutputStream)}, without the check
     * whether specifically stdout is a terminal.
     */
    private static OutputStream wrapStderrStream(OutputStream stream) {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows"))
            try {
                return new WindowsAnsiOutputStream(stream);
            } catch (Throwable e) {
                return new AnsiOutputStream(stream);
            }

        return new FilterOutputStream(stream) {
            @Override
            public void close() throws IOException {
                write(AnsiOutputStream.REST_CODE);
                flush();
                super.close();
            }
        };
    }

    public static boolean isFormattingEnabled() {
        return isFormattingEnabled;
    }

    public static int getTerminalWidth() {
        updateTerminalWidth();
        return terminalWidth;
    }

    /**
     * Retrieves and stores the terminal with in {@link #terminalWidth}.
     *
     * <p>
     * The terminal width is queried with {@code bash -c tput cols 2> /dev/tty}.
     * As this may be an expensive operation, it is only performed if the last
     * call to this method was more than 500ms ago. Also we won't wait longer
     * than 10ms for the process to complete.
     */
    private static void updateTerminalWidth() {
        long time = currentTimeMillis();
        if (time - lastTerminalWidthCheck < TERMINAL_WIDTH_UPDATE_INTERVALL)
            return;
        lastTerminalWidthCheck = time;

        try {
            Process tputColsProc = getRuntime().exec(
                    new String[] {"bash", "-c", "tput cols 2> /dev/tty"});

            executeProcess(tputColsProc, TERMINAL_WDITH_UPDATE_TIME,
                    MILLISECONDS);

            try (BufferedReader reader = newBufferedReader(
                    tputColsProc.getInputStream(), Constants.CHARSET)) {
                terminalWidth = parseInt(reader.readLine());
            }
        } catch (Throwable e) {
        }
    }

    public static void flush() {
        err.flush();
    }

    public static void print(String message) {
        checkNotNull(message);
        err.print(message);
        numVolatileLines = 0;
    }

    public static void print(String format,
                             Object... args) {
        checkNotNull(format);
        print(format(format, args));
    }

    public static void print(Object object) {
        checkNotNull(object);
        print(object.toString());
    }

    public static void println() {
        print("\n");
    }

    public static void println(String message) {
        checkNotNull(message);
        print(message);
        println();
    }

    public static void println(String format,
                               Object... args) {
        checkNotNull(format);
        print(format, args);
        println();
    }

    public static void println(Object object) {
        checkNotNull(object);
        print(object);
        println();
    }

    public static void printlnWarning(String message) {
        checkNotNull(message);
        print(addPrefixAndColor(message, "Warning: ", Color.YELLOW));
    }

    public static void printlnWarning(String format,
                                      Object... args) {
        checkNotNull(format);
        printlnWarning(format(format, args));
    }

    public static void printlnWarning(Object object) {
        checkNotNull(object);
        printlnWarning(object.toString());
    }

    public static void printlnError(String message) {
        checkNotNull(message);
        print(addPrefixAndColor(message, "Error: ", Color.RED));
    }

    public static void printlnError(String format,
                                    Object... args) {
        checkNotNull(format);
        printlnError(format(format, args));
    }

    public static void printlnError(Object object) {
        checkNotNull(object);
        printlnError(object.toString());
    }

    /**
     * @return Guaranteed to have trailing newline.
     */
    private static String addPrefixAndColor(String message,
                                            String prefix,
                                            Color color) {
        StringBuilder sb = new StringBuilder();
        for (String line : splitSparse(message, '\n')) {
            if (isFormattingEnabled)
                sb.append(ansi().fg(color));
            sb.append(prefix).append(line);
            if (isFormattingEnabled)
                sb.append(ansi().fg(Color.DEFAULT));
            sb.append('\n');
        }

        return sb.toString();
    }

    /**
     * Prints like {@link #println}, except that printed lines are subject to be
     * removed with {@link #eraseLine()}.
     */
    public static void printlnVolatile() {
        err.println();
        ++numVolatileLines;
    }

    public static void printlnVolatile(String message) {
        checkNotNull(message);
        err.println(message);
        int terminalWidth = getTerminalWidth();
        List<String> lines = splitSparse(message, '\n');
        for (String line : lines)
            numVolatileLines += ceil(((float) line.length() / terminalWidth));
    }

    public static void printlnVolatile(String format,
                                       Object... args) {
        checkNotNull(format);
        printlnVolatile(format(format, args));
    }

    public static void printlnVolatile(Object object) {
        checkNotNull(object);
        printlnVolatile(object.toString());
    }

    public static String bold(String message) {
        checkNotNull(message);
        if (!isFormattingEnabled)
            return message;
        return ansi().bold() + message + ansi().boldOff();
    }

    public static String bold(String format,
                              Object... args) {
        checkNotNull(format);
        return bold(format(format, args));
    }

    public static String bold(Object object) {
        checkNotNull(object);
        return bold(object.toString());
    }

    /**
     * Erases the previous printed line, the cursor is then placed on the
     * beginning of that line.
     *
     * <p>
     * Note that only lines printed with {@link #printlnVolatile} may be removed
     * by this.
     *
     * @see #eraseLines(int)
     */
    public static void eraseLine() {
        if (!isFormattingEnabled || numVolatileLines == 0)
            return;
        --numVolatileLines;
        err.print(ansi().cursorUp(1).eraseLine());
        flush();
    }

    public static void eraseLines(int count) {
        checkArgument(count >= 0, "Argument 'count' must be positive!");
        for (int i = 0; i != count; ++i)
            eraseLine();
    }
}
