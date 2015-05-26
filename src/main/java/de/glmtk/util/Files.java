package de.glmtk.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Files {
    public static BufferedReader newBufferedReader(InputStream inputstream,
                                                   Charset charset) {
        return new BufferedReader(new InputStreamReader(inputstream, charset));
    }
}
