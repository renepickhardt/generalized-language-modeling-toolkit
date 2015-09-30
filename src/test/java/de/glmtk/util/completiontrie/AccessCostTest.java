package de.glmtk.util.completiontrie;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import de.glmtk.Constants;
import de.glmtk.util.StringUtils;


public class AccessCostTest {
    public static final Path TEST_FILE =
        Constants.TEST_RESSOURCES_DIR.resolve("completiontrie-large.tsv");

    @Test
    public void testHashMapRandomAccessCost() throws IOException {
        Map<String, Long> map = new HashMap<>();

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));
                map.put(string, score);
            }
        }

        BigInteger timeSum = BigInteger.ZERO;
        int n = 0;

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));

                long timeBefore = System.nanoTime();

                long scoreTrie = map.get(string);

                long timeAfter = System.nanoTime();

                assertEquals(score, scoreTrie);

                timeSum =
                    timeSum.add(BigInteger.valueOf(timeAfter - timeBefore));
                ++n;
            }
        }

        BigInteger timePerAccess = timeSum.divide(BigInteger.valueOf(n));
        System.out.println("HashMap Average Random Access: "
            + timePerAccess.toString() + "ns");
    }

    @Test
    public void testTreeMapRandomAccessCost() throws IOException {
        Map<String, Long> map = new TreeMap<>();

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));
                map.put(string, score);
            }
        }

        BigInteger timeSum = BigInteger.ZERO;
        int n = 0;

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));

                long timeBefore = System.nanoTime();

                long scoreTrie = map.get(string);

                long timeAfter = System.nanoTime();

                assertEquals(score, scoreTrie);

                timeSum =
                    timeSum.add(BigInteger.valueOf(timeAfter - timeBefore));
                ++n;
            }
        }

        BigInteger timePerAccess = timeSum.divide(BigInteger.valueOf(n));
        System.out.println("TreeMap Average Random Access: "
            + timePerAccess.toString() + "ns");
    }

    @Test
    public void testCompletionTrieRandomAccessCost() throws IOException {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
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
        builder = null;

        BigInteger timeSum = BigInteger.ZERO;
        int n = 0;

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));

                long timeBefore = System.nanoTime();

                long scoreTrie = trie.get(string);

                long timeAfter = System.nanoTime();

                assertEquals(score, scoreTrie);

                timeSum =
                    timeSum.add(BigInteger.valueOf(timeAfter - timeBefore));
                ++n;
            }
        }

        BigInteger timePerAccess = timeSum.divide(BigInteger.valueOf(n));
        System.out.println("CompletionTrie Average Random Access: "
            + timePerAccess.toString() + "ns");
    }

    @Test
    public void testCompletionTrieSortedAccessCost() throws IOException {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        Map<String, Long> map = new HashMap<>();

        try (BufferedReader reader =
            Files.newBufferedReader(TEST_FILE, Charset.defaultCharset())) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.split(line, '\t');
                String string = split.get(1);
                long score = Long.parseLong(split.get(0));
                builder.add(string, score);
                map.put(string, score);
            }
        }

        CompletionTrie trie = builder.build();
        builder.reset();
        builder = null;

        BigInteger timeSum = BigInteger.ZERO;
        int n = 0;

        Iterator<CompletionTrieEntry> iter = trie.iterator();
        while (iter.hasNext()) {
            long timeBefore = System.nanoTime();

            CompletionTrieEntry entry = iter.next();

            long timeAfter = System.nanoTime();

            assertEquals(map.get(entry.getString()).longValue(),
                entry.getScore());

            timeSum = timeSum.add(BigInteger.valueOf(timeAfter - timeBefore));
            ++n;
        }

        BigInteger timePerAccess = timeSum.divide(BigInteger.valueOf(n));
        System.out.println("CompletionTrie Average Sorted Access: "
            + timePerAccess.toString() + "ns");
    }
}
