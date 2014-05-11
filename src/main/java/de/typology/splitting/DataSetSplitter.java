package de.typology.splitting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.Config;

/**
 * This class splits and samples a given input file into training and test data
 * 
 * The thresholds can be configured in config.txt the relevant fields are
 * <ul>
 * <li>sampleRate</li>
 * <li>splitDataRatio</li>
 * </ul>
 * 
 * @author Rene Pickhardt, Martin Koerner
 * 
 */
public class DataSetSplitter {

    private Logger logger = LogManager.getLogger();

    private File dir;

    private String inputName;

    public DataSetSplitter(
            File dir,
            String inputName) {
        this.dir = dir;
        this.inputName = inputName;
    }

    /**
     * Takes a given input file and provides a 3 way split. The file can be
     * sampled via the sampleRatio. A high sample ratio means that a large
     * portion of the file is being thrown away
     * 
     * There the splitDataRatio specifies the percentage of the file that is
     * used as training data. The rest will be used as test and learing data.
     * 
     * The last parameter splitTestRatio is usually set to 50 and means that the
     * test data is also devided again into learning and testing data
     * 
     * 
     * @param trainingFile
     *            potentially large text file that needs to be split
     * @param trainingFileName
     *            filename where training data are to be stored
     * @param learningFileName
     *            filename where learning data are to be stored
     * @param testingFileName
     *            filename where test data are to be stored
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void split(
            String trainingFileName,
            String learningFileName,
            String testingFileName) throws IOException {
        logger.info("splitting into training, testing and learning file: "
                + dir + "/" + inputName);
        try (BufferedReader reader =
                new BufferedReader(new FileReader(dir.getAbsolutePath() + "/"
                        + inputName));
                BufferedWriter trainingDataWriter =
                        new BufferedWriter(new FileWriter(dir.getAbsolutePath()
                                + "/" + trainingFileName));
                BufferedWriter learningDataWriter =
                        new BufferedWriter(new FileWriter(dir.getAbsolutePath()
                                + "/" + learningFileName));
                BufferedWriter testingDataWriter =
                        new BufferedWriter(new FileWriter(dir.getAbsolutePath()
                                + "/" + testingFileName))) {
            int rand;
            String line;
            while ((line = reader.readLine()) != null) {
                rand = (int) (Math.random() * 100);
                if (rand >= Config.get().sampleRate) {
                    // keep data
                    rand = (int) (Math.random() * 100);
                    if (rand >= Config.get().splitDataRatio) {
                        // TODO make this flexible
                        line = "<s> " + line + " </s>";

                        // store data in testing or learning file
                        rand = (int) (Math.random() * 100);
                        if (rand >= Config.get().splitTestRatio) {
                            learningDataWriter.write(line + "\n");
                        } else {
                            testingDataWriter.write(line + "\n");
                        }
                    } else {
                        // store data in training file
                        trainingDataWriter.write(line + "\n");
                    }
                }
            }
        }
        logger.info("splitting done");
    }

    /**
     * splits testing.txt into testing-samples-1.txt - test-samples-n.txt.
     */
    public void splitIntoSequences(
            File trainingFile,
            int maxSequenceLength,
            int numberOfSequences) throws IOException {
        String[] fileNameSplit = trainingFile.getName().split("\\.");

        HashMap<Integer, BufferedWriter> testSequenceFileWriters =
                new HashMap<Integer, BufferedWriter>();
        for (int i = 1; i <= maxSequenceLength; i++) {
            testSequenceFileWriters.put(i, new BufferedWriter(new FileWriter(
                    new File(dir.getAbsolutePath() + "/" + fileNameSplit[0]
                            + "-samples-" + i + "." + fileNameSplit[1]))));
        }

        // get total count from stats file
        long sequenceCount = 0;
        try (BufferedReader reader =
                new BufferedReader(new FileReader(trainingFile))) {
            String line;
            // count sequences
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s");
                if (words.length < maxSequenceLength) {
                    continue;
                } else {
                    int sequenceStart = 0;
                    while (words.length - sequenceStart >= maxSequenceLength) {
                        sequenceCount++;
                        sequenceStart++;
                    }
                }
            }
        }

        logger.info("sequenceCount: " + sequenceCount);
        double sequenceProbability = (double) numberOfSequences / sequenceCount;
        long skipDistance = sequenceCount / numberOfSequences;
        logger.info("skipDistance: " + skipDistance);

        logger.info("splitting " + trainingFile.getName() + " into sequences");
        try (BufferedReader reader =
                new BufferedReader(new FileReader(trainingFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s");
                int linePointer = 0;
                while (words.length - linePointer >= maxSequenceLength) {
                    // build current sequence
                    String sequence = "";
                    for (int i = 0; i < maxSequenceLength; i++) {
                        sequence += words[linePointer + i] + " ";
                    }

                    sequence = sequence.replaceFirst(" $", "");
                    if (Math.random() <= sequenceProbability) {
                        String[] sequenceWords = sequence.split("\\s");
                        for (int i = 1; i <= maxSequenceLength; i++) {
                            // build result sequence
                            String resultSequence = "";
                            for (int j = 0; j < i; j++) {
                                resultSequence += sequenceWords[j] + " ";
                            }
                            resultSequence =
                                    resultSequence.replaceFirst(" $", "");
                            testSequenceFileWriters.get(i).write(
                                    resultSequence + "\n");
                        }
                    }
                    linePointer++;
                }
            }
        }

        for (BufferedWriter testSequenceWriters : testSequenceFileWriters
                .values()) {
            testSequenceWriters.close();
        }
    }
}
