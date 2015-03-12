package de.glmtk.util.completiontrie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.glmtk.util.NioUtils;

public class CompletionTrieUtils {
    public static void visualize(CompletionTrie trie) throws IOException, InterruptedException {
        Path fileDot = Files.createTempFile("completionTrie", ".dot");
        Path filePng = Paths.get(fileDot + ".png");

        try (BufferedWriter writer = Files.newBufferedWriter(fileDot,
                Charset.forName("UTF-8"))) {
            trie.printDot(writer);
        }

        Runtime r = Runtime.getRuntime();

        Process dot = r.exec("dot -Tpng " + fileDot);
        NioUtils.copy(dot.getInputStream(), Files.newOutputStream(filePng));
        dot.waitFor();

        Process feh = r.exec("feh " + filePng);
        feh.waitFor();
    }

    public static void visualize(CompletionTrieBuilder trie) throws IOException, InterruptedException {
        Path fileDot = Files.createTempFile("completionTrie", ".dot");
        Path filePng = Paths.get(fileDot + ".png");

        try (BufferedWriter writer = Files.newBufferedWriter(fileDot,
                Charset.forName("UTF-8"))) {
            trie.printDot(writer);
        }

        Runtime r = Runtime.getRuntime();

        Process dot = r.exec("dot -Tpng " + fileDot);
        NioUtils.copy(dot.getInputStream(), Files.newOutputStream(filePng));
        dot.waitFor();

        Process feh = r.exec("feh " + filePng);
        feh.waitFor();
    }

    public static boolean equal(CompletionTrie trie,
                                CompletionTrieBuilder builder) throws IOException {
        Path trieFile = Paths.get("/tmp/completionTrieEqual0");
        Path builderFile = Paths.get("/tmp/completionTrieEqual1");

        Charset charset = Charset.forName("UTF-8");

        try (BufferedWriter writer = Files.newBufferedWriter(trieFile, charset)) {
            trie.printDot(writer);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(builderFile,
                charset)) {
            builder.printDot(writer);
        }

        try (BufferedReader trieReader = Files.newBufferedReader(trieFile,
                charset);
                BufferedReader builderReader = Files.newBufferedReader(
                        builderFile, charset)) {
            int lineNo = 0;
            String trieLine, builderLine;
            while (true) {
                trieLine = trieReader.readLine();
                builderLine = builderReader.readLine();

                if (trieLine == null || builderLine == null) {
                    if (trieLine == null && builderLine == null)
                        return true;
                    System.err.println("Number of lines differ.");
                    return false;
                }

                ++lineNo;
                if (lineNo == 1)
                    continue;

                trieLine = trieLine.replaceAll("\"([^\"]*)-[0-9a-f]+\"",
                        "\"$1\"");
                builderLine = builderLine.replaceAll("\"([^\"]*)-[0-9a-f]+\"",
                        "\"$1\"");
                if (!trieLine.equals(builderLine)) {
                    System.out.println("Lines " + lineNo + " differ.");
                    System.out.println("TrieLine   : " + trieLine);
                    System.out.println("BuiderLine : " + builderLine);
                    return false;
                }
            }
        }
    }
}
