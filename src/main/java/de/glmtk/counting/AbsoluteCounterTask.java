package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AbsoluteCounterTask is reading the sequences for one pattern and one index
 * file
 * into a map and aggregating the number of occurences.\ Afterwards the file is
 * made persistent in the absolute directory with the same pattern and name and
 * finally the original file from the sequence directory is deleted.
 * 
 * The map containing all the aggregated counts must still fit into main memory
 * 
 * The output file is sorted if sortCounts is set true in the config.txt This
 * obviously
 * takes more time
 */
public class AbsoluteCounterTask implements Runnable {

    private static Logger logger = LogManager
            .getFormatterLogger(AbsoluteCounterTask.class);

    private static int numTasks = 1;

    private static int numCompleteTasks = 0;

    private Path inputFile;

    private Path outputFile;

    private String delimiter;

    private int bufferSize;

    private boolean deleteTempFiles;

    private boolean sortCounts;

    /**
     * prepares an absolute counter task
     * 
     * @param inputFile
     *            file containing sequences of arbitrary length
     * @param outputFile
     *            file to which the aggregated counts should be written
     * @param delimiter
     *            TODO: is this still needed? (I have the feeling it is legacy)
     * @param bufferSize
     *            TODO: why pass this here?
     * @param deleteTempFiles
     *            specifying if directories should be deleted
     * @param sortCounts
     *            specifying if aggregated counts should be sorted
     *            lexicographically
     * 
     *            TODO: are the last 4 parameters mandatory?
     */
    public AbsoluteCounterTask(
            Path inputFile,
            Path outputFile,
            String delimiter,
            int bufferSize,
            boolean deleteTempFiles,
            boolean sortCounts) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.delimiter = delimiter;
        this.bufferSize = bufferSize;
        this.deleteTempFiles = deleteTempFiles;
        this.sortCounts = sortCounts;
    }

    /**
     * calculates the Aggregated counts
     * 
     * <ol>
     * <li>aggregates the counts</li>
     * <li>[optionally] sorts the map lexicographically</li>
     * <li>makes the counts persistent</li>
     * <li>[optionally] deletes the input file</li>
     * </ol>
     */
    @Override
    public void run() {
        try {
            Map<String, Integer> sequenceCounts = getSequenceCounts();

            if (sortCounts) {
                sequenceCounts = sortCounts(sequenceCounts);
            }

            writeSequenceCounts(sequenceCounts);

            if (deleteTempFiles) {
                Files.delete(inputFile);
            }

            ++numCompleteTasks;
            logger.info("%6.2f%% Finished absolute counts for: %s/%s", 100.f
                    * numCompleteTasks / numTasks, inputFile.getParent()
                    .getFileName(), inputFile.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * aggregates the sequences from inputFile and returns a Map of strings with
     * Integer Counts
     * 
     * @return Map containing the Counts of each sequence in inputFile
     * @throws IOException
     */
    private Map<String, Integer> getSequenceCounts() throws IOException {
        Map<String, Integer> sequenceCounts = new HashMap<String, Integer>();

        try (InputStream input = Files.newInputStream(inputFile);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(input),
                                bufferSize)) {
            String sequence;
            // TODO: watch memory consumption. This Map here can become very
            // large and if it does not fit into main memory we need a different
            // strategy.
            while ((sequence = reader.readLine()) != null) {
                Integer count = sequenceCounts.get(sequence);
                sequenceCounts.put(sequence, count == null ? 1 : count + 1);
            }
        }

        return sequenceCounts;
    }

    /**
     * sorts the sequences lexicographically
     * 
     * @param sequenceCounts
     * @return sorted Map with lexicographical ordering of sequenceCounts
     */
    private Map<String, Integer>
        sortCounts(Map<String, Integer> sequenceCounts) {
        sequenceCounts = new TreeMap<String, Integer>(sequenceCounts);
        return sequenceCounts;
    }

    /**
     * makes the aggregated counts of sequences persistent.
     * 
     * @param sequenceCounts
     * @throws IOException
     */
    private void writeSequenceCounts(Map<String, Integer> sequenceCounts)
            throws IOException {
        try (OutputStream output = Files.newOutputStream(outputFile);
                BufferedWriter writer =
                        new BufferedWriter(new OutputStreamWriter(output),
                                bufferSize)) {
            for (Map.Entry<String, Integer> sequenceCount : sequenceCounts
                    .entrySet()) {
                String sequence = sequenceCount.getKey();
                Integer count = sequenceCount.getValue();
                writer.write(sequence);
                writer.write(delimiter);
                writer.write(count.toString());
                writer.write('\n');
            }
        }
    }

    /**
     * helper function to estimate the progressbar during calculation
     * The progress is calculated as the number of tasks which have been
     * completed divided by the number of total tasks
     * 
     * @param numTasks
     */
    public static void setNumTasks(int numTasks) {
        AbsoluteCounterTask.numTasks = numTasks;
    }

}
