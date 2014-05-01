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
     * Expects an {@code inputStream} where each line is formatted as
     * {@code <Sequence><Delimiter><Count>}. Writes to {@code outputStream}:
     * {@code <1+Count><1Count><2Count><3Count>} where:
     * 
     * <ul>
     * <li>{@code 1+Count} is the sum of the counts of all lines.</li>
     * <li>{@code 1Count} is the number of lines where count is {@code 1}.</li>
     * <li>{@code 2Count} is the number of lines where count is {@code 2}.</li>
     * <li>{@code 3+Count} is the number of line where count is {@code 3+}.</li>
     * </ul>
     * 
     * @param input
     *            InputStream to be read.
     * @param output
     *            OutputStream to be written to.
     * @param delimiter
     *            Delimiter that separates Sequence and Count.
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
            String result;

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(input))) {
                if (countLines) {
                    Long count = 0L;
                    while (reader.readLine() != null) {
                        ++count;
                    }
                    result =
                            count + delimiter + "0" + delimiter + "0"
                                    + delimiter + "0";
                } else {
                    Counter counter = new Counter();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        counter.add(Long.parseLong(line.split(delimiter)[1]));
                    }
                    result = counter.toString(delimiter);
                }
            }

            try (BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(output))) {
                writer.write(result + "\n");
            }
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
