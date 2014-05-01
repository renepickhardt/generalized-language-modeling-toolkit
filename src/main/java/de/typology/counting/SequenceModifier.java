package de.typology.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * A class for modifying the sequences in workingDirectory based on the given
 * Pattern. The modified sequences are returned as outputStream
 * 
 * @author Martin Koerner
 * 
 */
public class SequenceModifier implements Runnable {

    private File workingDirectory;

    private OutputStream outputStream;

    private String delimiter;

    private boolean[] pattern;

    private boolean modifyCount;

    private boolean setCountToOne;

    public SequenceModifier(
            File workingDirectory,
            OutputStream outputStream,
            String delimiter,
            boolean[] pattern,
            boolean modifyCount,
            boolean setCountToOne) {
        this.workingDirectory = workingDirectory;
        this.outputStream = outputStream;
        this.delimiter = delimiter;
        this.pattern = pattern;
        this.modifyCount = modifyCount;
        this.setCountToOne = setCountToOne;
    }

    @Override
    public void run() {
        BufferedWriter outputStreamWriter =
                new BufferedWriter(new OutputStreamWriter(outputStream));
        try {
            for (File trainingFile : workingDirectory.listFiles()) {
                BufferedReader trainingFileReader =
                        new BufferedReader(new FileReader(trainingFile));
                String line;
                while ((line = trainingFileReader.readLine()) != null) {
                    String[] lineSplit = line.split(delimiter);
                    if (modifyCount) {
                        String[] words = lineSplit[0].split("\\s");
                        String modifiedWords = "";
                        try {
                            for (int i = 0; i < pattern.length; i++) {
                                if (pattern[i]) {
                                    modifiedWords += words[i] + " ";
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        modifiedWords = modifiedWords.replaceFirst(" $", "");
                        // TODO: better solution?
                        if (words[0].equals("<fs>")) {
                            // for kneser-ney smoothing: every sequence that
                            // starts
                            // with <fs> counts as a new sequence
                            if (workingDirectory.getName().equals("1")) {
                                continue;
                            }
                            if (!pattern[0]) {
                                // set <s> in _1 to zero
                                if (workingDirectory.getName().equals("11")
                                        && words[1].equals("<s>")) {
                                    outputStreamWriter.write("<s>" + delimiter
                                            + "0\n");
                                } else {
                                    outputStreamWriter.write(modifiedWords
                                            + delimiter
                                            + line.split(delimiter)[1] + "\n");
                                }
                            }
                            // if pattern[0]==true: leave out sequence
                        } else {
                            if (setCountToOne) {
                                outputStreamWriter.write(modifiedWords
                                        + delimiter + "1\n");
                            } else {
                                outputStreamWriter.write(modifiedWords
                                        + delimiter + lineSplit[1] + "\n");
                            }
                        }
                    } else {
                        outputStreamWriter.write(line + "\n");
                    }

                }
                trainingFileReader.close();
            }
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
