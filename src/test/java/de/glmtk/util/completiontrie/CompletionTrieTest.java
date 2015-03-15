package de.glmtk.util.completiontrie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;

import de.glmtk.util.PeekableIterator;
import de.glmtk.util.StringUtils;

public class CompletionTrieTest {
    @Test
    public void testEmptyTrie() throws IOException {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        CompletionTrie trie = builder.build();
        assertEquals(1, trie.getMemoryConsumption());
        assertTrue(CompletionTrieUtils.equal(trie, builder));
        Iterator<CompletionTrieEntry> trieIter = trie.iterator();
        assertFalse(trieIter.hasNext());
        try {
            trieIter.next();
            fail("Expected NoSuchElementException.");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testBuilder() throws IOException, InterruptedException {
        //@formatter:off
        List<CompletionTrieEntry> entries = Arrays.asList(
                new CompletionTrieEntry("new", 1),
                new CompletionTrieEntry("no", 2),
                new CompletionTrieEntry("not", 3),
                new CompletionTrieEntry("1997", 5825),
                new CompletionTrieEntry("19. Jahr", 5846),
                new CompletionTrieEntry("1. Sep", 5882)
                );
        //@formatter:on

        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        for (CompletionTrieEntry entry : entries)
            builder.add(entry.getString(), entry.getScore());

        CompletionTrie trie = builder.build();
        CompletionTrieUtils.visualize(trie);

        assertTrue(trie.contains("new"));
        assertFalse(trie.contains("now"));
        assertFalse(trie.contains("ne"));
        assertFalse(trie.contains(""));
        assertTrue(trie.containsPrefix("no"));
        assertTrue(trie.containsPrefix("ne"));
        assertTrue(trie.containsPrefix(""));
        assertFalse(trie.containsPrefix("na"));
        assertFalse(trie.containsPrefix("nott"));
        assertNull(trie.get("ne"));
        assertNull(trie.get("na"));
        assertNull(trie.get("nott"));

        for (CompletionTrieEntry entry : entries) {
            assertTrue(trie.contains(entry.getString()));
            assertTrue(trie.containsPrefix(entry.getString()));
            assertEquals(Long.valueOf(entry.getScore()),
                    trie.get(entry.getString()));
        }

        PeekableIterator<CompletionTrieEntry> iter = trie.getCompletions("1");
        while (iter.hasNext())
            assertEquals(iter.peek(), iter.next());
    }

    @Test
    @Ignore
    public void testAutocompletion() throws IOException {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        try (BufferedReader reader = Files.newBufferedReader(
                Paths.get("/home/lukas/langmodels/workspace/autocompletion/data/wiki-articles.nolink.tsv"),
                Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));
                builder.add(string, score);
            }
        }

        CompletionTrie trie = builder.build();
        builder.reset();

        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(
                System.in))) {
            System.out.println("Query:");
            String input;
            long timeBefore, timeAfter;
            while ((input = stdin.readLine()) != null) {
                timeBefore = System.nanoTime();
                List<CompletionTrieEntry> completions = trie.getTopKCompletions(
                        input, 5);
                timeAfter = System.nanoTime();
                System.out.println("Completion took... "
                        + (timeAfter - timeBefore) / 1000f + "us");
                for (CompletionTrieEntry entry : completions)
                    System.out.println(entry);
            }
        }
    }
}
