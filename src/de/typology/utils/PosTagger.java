package de.typology.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class PosTagger {

    public static void main(String[] args) throws IOException {
        Path model =
                Paths.get("/home/lukas/Downloads/stanford-postagger-full-2014-01-04/models/english-left3words-distsim.tagger");
        Path input = Paths.get("/home/lukas/Downloads/en008t/training.txt");
        Path output = Paths.get("/home/lukas/Downloads/en008t/tagged.txt");

        try (BufferedReader r =
                Files.newBufferedReader(input, Charset.defaultCharset());
                BufferedWriter w =
                        Files.newBufferedWriter(output,
                                Charset.defaultCharset(),
                                StandardOpenOption.CREATE)) {
            MaxentTagger tagger = new MaxentTagger(model.toString());

            String line;
            while ((line = r.readLine()) != null) {
                // Tag
                String[] sentence = line.split(" ");
                List<TaggedWord> taggedSentence =
                        tagger.tagSentence(arrayToListHasWords(sentence));

                // Write
                boolean first = true;
                for (TaggedWord tagged : taggedSentence) {
                    if (first) {
                        first = false;
                    } else {
                        w.write(" ");
                    }
                    w.write(tagged.word() + "/" + tagged.tag());
                }
                w.write("\n");
            }
        }
    }

    private static List<HasWord> arrayToListHasWords(String[] array) {
        List<HasWord> result = new LinkedList<HasWord>();
        for (String word : array) {
            result.add(new Word(word));
        }
        return result;
    }
}
