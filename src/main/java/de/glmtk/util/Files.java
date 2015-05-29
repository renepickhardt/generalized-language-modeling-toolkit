package de.glmtk.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class Files {
    public static BufferedReader newBufferedReader(InputStream inputstream,
                                                   Charset charset) {
        return new BufferedReader(new InputStreamReader(inputstream, charset));
    }

    public static BufferedWriter newBufferedWriter(OutputStream outputstream,
                                                   Charset charset) {
        return new BufferedWriter(new OutputStreamWriter(outputstream, charset));
    }
}
