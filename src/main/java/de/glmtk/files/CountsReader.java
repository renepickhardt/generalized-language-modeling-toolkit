package de.glmtk.files;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Comparator;

import de.glmtk.common.Counter;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.NioUtils;
import de.glmtk.util.ObjectUtils;

public class CountsReader implements Closeable, AutoCloseable {
    public static final Comparator<CountsReader> SEQUENCE_COMPARATOR = new Comparator<CountsReader>() {
        @Override
        public int compare(CountsReader lhs,
                           CountsReader rhs) {
            if (lhs == rhs)
                return 0;
            else if (lhs == null)
                return 1;
            else if (rhs == null)
                return -1;
            else
                return ObjectUtils.compare(lhs.sequence, rhs.sequence);
        }
    };

    private Path file;
    private BufferedReader reader;
    private int lineNo;
    private String line;
    private String sequence;
    private Counter counter;

    public CountsReader(Path file,
                        Charset charset,
                        int sz) throws IOException {
        this.file = file;
        reader = NioUtils.newBufferedReader(file, charset, sz);
        lineNo = -1;
        line = null;
        sequence = null;
        counter = null;
    }

    public String readLine() throws IOException {
        line = reader.readLine();
        ++lineNo;
        if (line == null) {
            sequence = null;
            counter = null;
        } else {
            counter = new Counter();
            try {
                sequence = Counter.getSequenceAndCounter(line, counter);
            } catch (IllegalArgumentException e) {
                throw new FileFormatException(line, lineNo, file, "chunk",
                        e.getMessage());
            }
        }

        return line;
    }

    public String getSequence() {
        return sequence;
    }

    public Counter getCounter() {
        return counter;
    }

    public boolean isEof() {
        return sequence == null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
