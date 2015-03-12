package de.glmtk.util.completiontrie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.Constants;
import de.glmtk.util.PrintUtils;
import de.glmtk.util.StringUtils;

@RunWith(Parameterized.class)
public class FileTest {
    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        //@formatter:off
        return Arrays.asList(new Object[][]{
                { Constants.TEST_RESSOURCES_DIR.resolve("completiontrie-small.tsv") },
                { Constants.TEST_RESSOURCES_DIR.resolve("completiontrie-large.tsv") }
        });
        //@formatter:on
    }

    private Path path;

    public FileTest(Path path) {
        this.path = path;
    }

    @Test
    public void test() throws IOException {
        System.out.println("--- " + path);

        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);

        long timeBefore, timeAfter;

        try (BufferedReader reader = Files.newBufferedReader(path,
                Charset.defaultCharset())) {
            timeBefore = System.currentTimeMillis();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));
                builder.add(string, score);
            }
            timeAfter = System.currentTimeMillis();
            System.out.println("Adding all entries... "
                    + (timeAfter - timeBefore) + "ms");
        }

        timeBefore = System.currentTimeMillis();
        CompletionTrie trie = builder.build();
        timeAfter = System.currentTimeMillis();
        System.out.println("Building packed trie... "
                + (timeAfter - timeBefore) + "ms");

        System.out.println("Memory consumption... "
                + PrintUtils.humanReadableByteCount(trie.getMemoryConsumption()));

        timeBefore = System.currentTimeMillis();
        assertTrue(CompletionTrieUtils.equal(trie, builder));
        timeAfter = System.currentTimeMillis();
        System.out.println("Verifying equals input trie... "
                + (timeAfter - timeBefore) + "ms");

        builder.reset();

        Deque<CompletionTrieEntry> entries = new ArrayDeque<>();
        timeBefore = System.currentTimeMillis();
        for (CompletionTrieEntry entry : trie)
            entries.addFirst(entry);
        timeAfter = System.currentTimeMillis();
        System.out.println("Iterating elements... " + (timeAfter - timeBefore)
                + "ms");

        Iterator<CompletionTrieEntry> entriesIter = entries.iterator();
        try (BufferedReader reader = Files.newBufferedReader(path,
                Charset.defaultCharset())) {
            String line, lastLine = null;
            while ((line = reader.readLine()) != null) {
                if (line.equals(lastLine))
                    continue;
                CompletionTrieEntry entry = entriesIter.next();
                assertEquals(line, entry.getScore() + "\t" + entry.getString());
                lastLine = line;

                assertTrue(trie.contains(entry.getString()));
                assertTrue(trie.containsPrefix(entry.getString()));
                assertEquals(Long.valueOf(entry.getScore()),
                        trie.get(entry.getString()));
            }
        }
        assertFalse(entriesIter.hasNext());
    }
}
