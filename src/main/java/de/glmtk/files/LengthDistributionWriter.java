package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class LengthDistributionWriter extends AbstractFileWriter {
    public LengthDistributionWriter(Path file,
                                    Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public LengthDistributionWriter(Path file,
                                    Charset charset,
                                    int sz) throws IOException {
        super(file, charset, sz);
    }

    public void append(int length,
                       double frequency) throws IOException {
        writer.append(Integer.toString(length)).append('\t').append(
                Double.toString(frequency)).append('\n');
    }
}
