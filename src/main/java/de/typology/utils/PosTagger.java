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
                Paths.get("stanford-postagger-full-2014-01-04/models/english-left3words-distsim.tagger");
        Path input =
                Paths.get(Config.get().outputDir + "/"
                        + Config.get().inputDataSet + "/training.txt");
        Path output =
                Paths.get(Config.get().outputDir + "/"
                        + Config.get().inputDataSet + "/tagged.txt");

        try (BufferedReader reader =
                Files.newBufferedReader(input, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(output,
                                Charset.defaultCharset(),
                                StandardOpenOption.CREATE)) {
            MaxentTagger tagger = new MaxentTagger(model.toString());

            String line;
            while ((line = reader.readLine()) != null) {
                // Tag
                String[] sentence = line.split("\\s");
                List<TaggedWord> taggedSentence =
                        tagger.tagSentence(arrayToListHasWords(sentence));

                // Write
                boolean first = true;
                for (TaggedWord tagged : taggedSentence) {
                    writer.write((first ? "" : " ") + tagged.word() + "/"
                            + tagged.tag());
                    first = false;
                }
                writer.write("\n");
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
