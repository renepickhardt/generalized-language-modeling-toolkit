package de.glmtk.scripts;

import static java.lang.Character.charCount;
import static java.lang.Character.toChars;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.common.PatternElem;
import de.glmtk.counting.Tagger;

public class ReplaceReservedSymbols {
    private static final List<String> RESERVED_SYMBOLS = Arrays.asList(
            PatternElem.SKP_WORD, PatternElem.WSKP_WORD,
            Character.toString(Tagger.POS_SEPARATOR));

    public static void main(String args[]) throws IOException {
        Path inputFile = Paths.get("/home/lukas/langmodels/data/oanc.raw");
        Path outputFile = Paths.get("/home/lukas/langmodels/data/oanc.noreserved");

        try (BufferedReader reader = newBufferedReader(inputFile,
                Constants.CHARSET);
                BufferedWriter writer = newBufferedWriter(outputFile,
                        Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null)
                writer.append(replaceReservedSymbolsInLine(line)).append('\n');
        }
    }

    private static String replaceReservedSymbolsInLine(String line) {
        StringBuilder result = new StringBuilder();

        int offset = 0;
        int length = line.length();
        while (offset != length) {
            int curChar = line.codePointAt(offset);
            offset += charCount(curChar);

            String string = String.valueOf(toChars(curChar));
            if (RESERVED_SYMBOLS.contains(string))
                string = "?";
            result.append(string);
        }

        return result.toString();
    }
}
