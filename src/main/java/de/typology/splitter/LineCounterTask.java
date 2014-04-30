package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class LineCounterTask implements Runnable {

    protected InputStream inputStream;

    protected OutputStream outputStream;

    protected String delimiter;

    protected boolean setCountToOne;

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
     * @param inputStream
     *            InputStream to be read.
     * @param outputStream
     *            OutputStream to be written to.
     * @param delimiter
     *            Delimiter that separates Sequence and Count.
     * @param setCountToOne
     *            If {@code true} {@code 1+Count} will count the number of lines
     *            and all other counts will be zero. If {@code false} will act
     *            as described above.
     */
    public LineCounterTask(
            InputStream inputStream,
            OutputStream outputStream,
            String delimiter,
            boolean setCountToOne) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
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

            try (BufferedReader inputStreamReader =
                    new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = inputStreamReader.readLine()) != null) {
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

            try (BufferedWriter bufferedWriter =
                    new BufferedWriter(new OutputStreamWriter(outputStream))) {
                bufferedWriter.write(onePlusCount + delimiter + oneCount
                        + delimiter + twoCount + delimiter + threePlusCount
                        + "\n");
            }
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
