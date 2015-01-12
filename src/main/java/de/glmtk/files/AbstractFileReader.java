package de.glmtk.files;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.util.NioUtils;

public abstract class AbstractFileReader implements Closeable, AutoCloseable {
    protected static long parseNumber(String value) throws NumberFormatException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(String.format(
                    "Unable to parse '%s' as a number.", value));
        }
    }

    protected Path file;
    protected int lineNo;
    protected String line;
    private BufferedReader reader;

    public AbstractFileReader(Path file,
                              Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public AbstractFileReader(Path file,
                              Charset charset,
                              int sz) throws IOException {
        this.file = file;
        lineNo = -1;
        line = "undefined"; // so isEof() is not true if readLine() hasn't been called yet.
        reader = NioUtils.newBufferedReader(file, charset, sz);
    }

    public String readLine() throws IOException {
        line = reader.readLine();
        ++lineNo;
        parseLine();
        return line;
    }

    protected abstract void parseLine();

    public boolean isEof() {
        return line == null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
