package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Calculates counts of lines in a InputStream.
 */
public class LineCounterTask implements Runnable {

    private InputStream input;

    private OutputStream output;

    private String delimiter;

    private boolean countLines;

    /**
     * Expects an {@code input} where each line is formatted as
     * {@code <Sequence><Delimiter><Count>}. Writes to {@code output}:
     * {@code <1+Count><1Count><2Count><3+Count>} where:
     * 
     * <ul>
     * <li>{@code 1+Count} is the sum of the counts of all lines.</li>
     * <li>{@code 1Count} is the number of lines where count was {@code 1}.</li>
     * <li>{@code 2Count} is the number of lines where count was {@code 2}.</li>
     * <li>{@code 3+Count} is the number of line where count was {@code 3+}.</li>
     * </ul>
     * 
     * @param input
     *            InputStream to be read.
     * @param output
     *            OutputStream to be written to.
     * @param delimiter
     *            Delimiter that separates Sequences and Counts.
     * @param countLines
     *            If {@code true} {@code 1+Count} will count the number of lines
     *            and all other counts will be zero. If {@code false} will act
     *            as described above.
     */
    public LineCounterTask(
            InputStream input,
            OutputStream output,
            String delimiter,
            boolean countLines) {
        this.input = input;
        this.output = output;
        this.delimiter = delimiter;
        this.countLines = countLines;
    }

    @Override
    public void run() {
        try {
            Counter counter = new Counter();

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    counter.add(countLines ? 1 : Long.parseLong(line
                            .split(delimiter)[1]));
                }
                if (countLines) {
                    counter.setOneCount(0);
                }
            }

            try (BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(output))) {
                writer.write(counter.toString(delimiter) + "\n");
            }
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
