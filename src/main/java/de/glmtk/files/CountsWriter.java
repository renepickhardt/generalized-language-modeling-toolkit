package de.glmtk.files;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.counts.Counts;
import de.glmtk.util.NioUtils;

public class CountsWriter implements Closeable, AutoCloseable {
    private BufferedWriter writer;

    public CountsWriter(Path file,
                        Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public CountsWriter(Path file,
                        Charset charset,
                        int sz) throws IOException {
        writer = NioUtils.newBufferedWriter(file, charset, sz);
    }

    public void append(String sequence,
                       long count) throws IOException {
        writer.append(sequence).append('\t').append(Long.toString(count)).append(
                '\n');
    }

    public void append(String sequence,
                       Counts counts) throws IOException {
        writer.append(sequence);
        writer.append('\t').append(Long.toString(counts.getOnePlusCount()));
        writer.append('\t').append(Long.toString(counts.getOneCount()));
        writer.append('\t').append(Long.toString(counts.getTwoCount()));
        writer.append('\t').append(Long.toString(counts.getThreePlusCount()));
        writer.append('\n');
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
