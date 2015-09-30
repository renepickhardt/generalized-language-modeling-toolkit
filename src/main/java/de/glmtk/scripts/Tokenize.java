package de.glmtk.scripts;

import static java.lang.Character.charCount;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.toChars;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.glmtk.Constants;


/**
 * Takes a file and puts spaces between all tokens.
 *
 * Example: {@code She said: "Hello you're 13th".} becomes
 * {@code She said : " Hello you ' re 13th " .}
 */
public class Tokenize {
    public static void main(String[] args) throws IOException {
        Path inputFile =
            Paths.get("/home/lukas/langmodels/data/oanc.noreserved");
        Path outputFile = Paths.get("/home/lukas/langmodels/data/oanc");

        try (BufferedReader reader =
            newBufferedReader(inputFile, Constants.CHARSET);
             BufferedWriter writer =
                 newBufferedWriter(outputFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.append(tokenizeLine(line)).append('\n');
            }
        }
    }

    private static String tokenizeLine(String line) {
        StringBuilder result = new StringBuilder();

        int offset = 0;
        int length = line.length();
        boolean lastCharWasSpace = true;
        while (offset != length) {
            int curChar = line.codePointAt(offset);
            offset += charCount(curChar);

            if (isLetterOrDigit(curChar)) {
                result.append(toChars(curChar));
                lastCharWasSpace = false;
            } else if (curChar == ' ') {
                if (!lastCharWasSpace) {
                    result.append(' ');
                }
                lastCharWasSpace = true;
            } else { // curChar is special character
                if (!lastCharWasSpace) {
                    result.append(' ');
                }
                result.append(toChars(curChar));
                result.append(' ');
                lastCharWasSpace = true;
            }
        }

        // Delete trailing whitespace that occurs if last char was a special
        // character
        int resultLength = result.length();
        if (resultLength != 0 && result.charAt(resultLength - 1) == ' ') {
            result.deleteCharAt(resultLength - 1);
        }

        return result.toString();
    }
}
