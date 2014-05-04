package de.typology.filtering;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.TreeSet;

public class Reducer {

    private InputStream input;

    private OutputStream output;

    public Reducer(
            InputStream input,
            OutputStream output) {
        this.input = input;
        this.output = output;
    }

    public void reduce() throws IOException {
        Set<String> sequences = new TreeSet<String>();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sequences.add(line);
            }
        }

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(output))) {
            for (String sequence : sequences) {
                writer.write(sequence + "\n");
            }
        }
    }
}
