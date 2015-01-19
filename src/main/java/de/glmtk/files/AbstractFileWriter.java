package de.glmtk.files;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.util.NioUtils;

public abstract class AbstractFileWriter implements Closeable, AutoCloseable {
    protected BufferedWriter writer;

    public AbstractFileWriter(Path file,
                              Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AbstractFileWriter(Path file,
                              Charset charset,
                              int sz) throws IOException {
        writer = NioUtils.newBufferedWriter(file, charset, sz);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
