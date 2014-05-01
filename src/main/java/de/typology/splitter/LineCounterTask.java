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

    private boolean setCountToOne;

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
     * @param setCountToOne
     *            If {@code true} {@code 1+Count} will count the number of lines
     *            and all other counts will be zero. If {@code false} will act
     *            as described above.
     */
    public LineCounterTask(
            InputStream input,
            OutputStream output,
            String delimiter,
            boolean setCountToOne) {
        this.input = input;
        this.output = output;
        this.delimiter = delimiter;
        this.setCountToOne = setCountToOne;
    }

    @Override
    public void run() {
        try {
            long onePlusCount = 0L;
            long oneCount = 0L;
            long twoCount = 0L;
            long threePlusCount = 0L;

            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (setCountToOne) {
                        ++onePlusCount;
                    } else {
                        long count = Long.parseLong(line.split(delimiter)[1]);

                        onePlusCount += count;
                        if (count == 1L) {
                            oneCount += count;
                        }
                        if (count == 2L) {
                            twoCount += count;
                        }
                        if (count >= 3L) {
                            threePlusCount += count;
                        }
                    }
                }
            }

            try (BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(output))) {
                writer.write(onePlusCount + delimiter + oneCount + delimiter
                        + twoCount + delimiter + threePlusCount + "\n");
            }
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
