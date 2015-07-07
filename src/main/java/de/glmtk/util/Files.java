package de.glmtk.util;

import static java.nio.file.Files.newInputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class Files {
    public static BufferedReader newBufferedReader(InputStream inputstream,
                                                   Charset charset) {
        return new BufferedReader(new InputStreamReader(inputstream, charset));
    }

    public static BufferedWriter newBufferedWriter(OutputStream outputstream,
                                                   Charset charset) {
        return new BufferedWriter(new OutputStreamWriter(outputstream, charset));
    }

    public static LineNumberReader newLineNumberReader(Path path,
                                                       Charset charset) throws IOException {
        return newLineNumberReader(path, charset, 8192);
    }

    public static LineNumberReader newLineNumberReader(Path path,
                                                       Charset charset,
                                                       int sz) throws IOException {
        return new LineNumberReader(new InputStreamReader(newInputStream(path),
                charset), sz);
    }
}
