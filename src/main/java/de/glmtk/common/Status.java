package de.glmtk.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.counting.Tagger;
import de.glmtk.util.HashUtils;
import de.glmtk.util.StringUtils;

public class Status {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Status.class);

    public static enum Training {
        NONE,
        UNTAGGED,
        TAGGED;
    }

    private static enum NextLine {
        EOF,
        SECTION,
        KEYVALUE;
    }

    private GlmtkPaths paths;
    private Path file;
    private Path corpus;
    private boolean corpusTagged;
    private String hash;
    private String taggedHash;
    private Training training;
    private Set<Pattern> counted;
    private Set<Pattern> absoluteCounted;
    private Set<Pattern> continuationCounted;
    private boolean nGramTimesCounted = true;
    private boolean lengthDistributionCalculated = true;
    private Map<Pattern, Set<String>> absoluteChunked;
    private Map<Pattern, Set<String>> continuationChunked;
    private Map<String, Set<Pattern>> queryCacheCounted;
    private List<String> lines;
    private String line;
    private int lineNo;

    public Status(GlmtkPaths paths,
                  Path corpus) throws Exception {
        this.paths = paths;
        file = paths.getStatusFile();
        this.corpus = corpus;
        corpusTagged = Tagger.detectFileTagged(corpus);

        setVoidSettings();
        if (Files.exists(file))
            readStatusFromFile();
        writeStatusToFile();
    }

    private void setVoidSettings() {
        training = Training.NONE;
        counted = new HashSet<Pattern>();
        nGramTimesCounted = true;
        lengthDistributionCalculated = true;
        absoluteCounted = new HashSet<Pattern>();
        continuationCounted = new HashSet<Pattern>();
        absoluteChunked = new HashMap<Pattern, Set<String>>();
        continuationChunked = new HashMap<Pattern, Set<String>>();
        queryCacheCounted = new HashMap<String, Set<Pattern>>();
    }

    public void logStatus() {
        LOGGER.debug("Status %s", StringUtils.repeat("-",
                80 - "Status ".length()));
        LOGGER.debug("hash                = %s", hash);
        LOGGER.debug("taggedHash          = %s", taggedHash);
        LOGGER.debug("training            = %s", training);
        LOGGER.debug("absoluteCounted     = %s", absoluteCounted);
        LOGGER.debug("continuationCounted = %s", continuationCounted);
        LOGGER.debug("absoluteChunked     = %s", absoluteChunked);
        LOGGER.debug("continuationChunked = %s", continuationChunked);
    }

    public boolean isCorpusTagged() {
        return corpusTagged;
    }

    public Training getTraining() {
        return training;
    }

    public void setTraining(Training training) throws IOException {
        synchronized (this) {
            if (training == Training.NONE) {
                hash = null;
                taggedHash = null;
                this.training = Training.NONE;
            }

            String hash = HashUtils.generateMd5Hash(paths.getTrainingFile());
            if (training == Training.UNTAGGED) {
                if (this.hash == null)
                    this.hash = hash;
                if (this.hash.equals(hash)) {
                    writeStatusToFile();
                    return;
                }
                this.hash = hash;
            } else {
                if (taggedHash == null)
                    taggedHash = hash;
                if (taggedHash.equals(hash)) {
                    writeStatusToFile();
                    return;
                }
                taggedHash = hash;
            }

            LOGGER.info("Setting training with different hash than last execution. Can't continue with state of last execution.");
            setVoidSettings();
            writeStatusToFile();
        }
    }

    private Set<Pattern> counted(boolean continuation) {
        return !continuation ? absoluteCounted : continuationCounted;
    }

    private Map<Pattern, Set<String>> chunked(boolean continuation) {
        return !continuation ? absoluteChunked : continuationChunked;
    }

    public Set<Pattern> getCounted() {
        synchronized (this) {
            return Collections.unmodifiableSet(counted);
        }
    }

    public Set<Pattern> getCounted(boolean continuation) {
        synchronized (this) {
            return Collections.unmodifiableSet(counted(continuation));
        }
    }

    public Set<Pattern> getChunkedPatterns(boolean continuation) {
        synchronized (this) {
            return Collections.unmodifiableSet(chunked(continuation).keySet());
        }
    }

    public Set<String> getChunksForPattern(boolean continuation,
            Pattern pattern) {
        synchronized (this) {
            return Collections.unmodifiableSet(chunked(continuation).get(
                    pattern));
        }
    }

    public void setChunksForPattern(boolean continuation,
                                    Pattern pattern,
                                    Collection<String> chunks) throws IOException {
        synchronized (this) {
            chunked(continuation).put(pattern,
                    new LinkedHashSet<String>(chunks));
            writeStatusToFile();
        }
    }

    public void performMergeForChunks(boolean continuation,
                                      Pattern pattern,
                                      Collection<String> mergedChunks,
                                      String mergeFile) throws IOException {
        synchronized (this) {
            Set<String> chunks = chunked(continuation).get(pattern);
            chunks.removeAll(mergedChunks);
            chunks.add(mergeFile);
            writeStatusToFile();
        }
    }

    public void finishMerge(boolean continuation,
                            Pattern pattern) throws IOException {
        synchronized (this) {
            nGramTimesCounted = false;
            lengthDistributionCalculated = false;
            counted.add(pattern);
            counted(continuation).add(pattern);
            chunked(continuation).remove(pattern);
            writeStatusToFile();
        }
    }

    public Set<Pattern> getQueryCacheCounted(String name) {
        synchronized (this) {
            Set<Pattern> patterns = queryCacheCounted.get(name);
            return patterns != null ? patterns : new HashSet<Pattern>();
        }
    }

    public boolean isNGramTimesCounted() {
        return nGramTimesCounted;
    }

    public void setNGramTimesCounted() throws IOException {
        synchronized (this) {
            nGramTimesCounted = true;
            writeStatusToFile();
        }
    }

    public boolean isLengthDistributionCalculated() {
        return lengthDistributionCalculated;
    }

    public void setLengthDistributionCalculated() throws IOException {
        synchronized (this) {
            lengthDistributionCalculated = true;
            writeStatusToFile();
        }
    }

    public void addQueryCacheCounted(String name,
                                     Pattern pattern) throws IOException {
        synchronized (this) {
            Set<Pattern> patterns = queryCacheCounted.get(name);
            if (patterns == null) {
                patterns = new HashSet<Pattern>();
                queryCacheCounted.put(name, patterns);
            }
            patterns.add(pattern);
            writeStatusToFile();
        }
    }

    private void readStatusFromFile() throws Exception {
        LOGGER.debug("Reading status from file '%s'.", file);

        lines = Files.readAllLines(file, Constants.CHARSET);
        lineNo = 0;

        hash = readNextValue("hash");
        taggedHash = readNextValue("taggedHash");
        if (!validCorpusHash()) {
            setVoidSettings();
            return;
        }

        training = readTraining("training");
        absoluteCounted = readCounted("absoluteCounted");
        continuationCounted = readCounted("continuationCounted");
        nGramTimesCounted = readBoolean("nGramTimesCounted");
        lengthDistributionCalculated = readBoolean("lengthDistributionCalculated");
        absoluteChunked = readChunked("absoluteChunked");
        continuationChunked = readChunked("continuationChunked");
        queryCacheCounted = readQueryCacheCounted("queryCacheCounted");

        counted = new HashSet<Pattern>();
        counted.addAll(absoluteCounted);
        counted.addAll(continuationCounted);
    }

    private void assertNextSection(String expectedSection) throws Exception {
        LOGGER.trace("Status.assertNextSection(expectedSection=%s)",
                expectedSection);

        String section = readNextSection();

        if (!section.equals(expectedSection))
            try (Formatter f = new Formatter()) {
                f.format("Illegal line '%d' in file '%s'.%n", lineNo, file);
                f.format("Expected section '%s', but was '%s' instead.%n",
                        expectedSection, section);
                f.format("Line was: '%s'.", line);
                throw new Exception(f.toString());
            }
    }

    private String readNextSection() throws Exception {
        LOGGER.trace("Status.readNextSection()");

        readNextLine(false);

        String trimmed = line.trim();
        if (trimmed.endsWith(":")) {
            String section = line.substring(0, line.length() - 1).trim();
            if (section != null && !section.isEmpty())
                return section;
        }

        try (Formatter f = new Formatter()) {
            f.format("Illegal line '%d' in file '%s'.%n", lineNo, file);
            f.format("Expected line to have format: '<sectionname>:'.");
            f.format("Line was: '%s'.", line);
            throw new Exception(f.toString());
        }
    }

    private String readNextValue(String expectedKey) throws Exception {
        LOGGER.trace("Status.readNextValue(expectedKey=%s)", expectedKey);

        Entry<String, String> entry = readNextKeyValue();
        String key = entry.getKey();
        String value = entry.getValue();

        if (!key.equals(expectedKey))
            try (Formatter f = new Formatter()) {
                f.format("Illegal key on line '%d' in file '%s'.%n", lineNo,
                        file);
                f.format("Expected key '%s', but was '%s' instead.%n",
                        expectedKey, key);
                f.format("Line was: '%s'.", line);
                throw new Exception(f.toString());
            }

        return value;
    }

    private Entry<String, String> readNextKeyValue() throws Exception {
        LOGGER.trace("Status.readNextKeyValue()");

        readNextLine(false);

        List<String> split = StringUtils.splitAtChar(line, '=');
        if (split.size() != 2)
            try (Formatter f = new Formatter()) {
                f.format("Illegal line '%d' in file '%s'.%n", lineNo, file);
                f.format("Expected line to have format: '<key> = <value>'.%n");
                f.format("Line was: '%s'.", line);
                throw new Exception(f.toString());
            }

        String key = split.get(0).trim();
        String value = split.get(1).trim();
        if (value.equals("null") || value.isEmpty())
            value = null;
        return new SimpleEntry<String, String>(key, value);
    }

    private NextLine peekNextLine() throws Exception {
        LOGGER.trace("Status.peekNextLine()");

        int numReadLines = readNextLine(true);
        lineNo -= numReadLines;

        if (line == null)
            return NextLine.EOF;
        else if (line.trim().endsWith(":"))
            return NextLine.SECTION;
        else
            return NextLine.KEYVALUE;
    }

    private int readNextLine(boolean allowEof) throws Exception {
        LOGGER.trace("Status.readNextLine(allowEof=%b)", allowEof);

        int numReadLines = 0;
        do {
            ++numReadLines;
            try {
                line = lines.get(lineNo++);
            } catch (IndexOutOfBoundsException e) {
                if (allowEof) {
                    line = null;
                    break;
                } else
                    throw new Exception(String.format(
                            "Unexcpected End of File in file '%s'.", file));
            }
        } while (line.trim().isEmpty());
        return numReadLines;
    }

    private boolean validCorpusHash() throws IOException {
        if (!corpusTagged) {
            String corpusHash = HashUtils.generateMd5Hash(corpus);
            if (hash == null)
                hash = corpusHash;
            if (hash.equals(corpusHash))
                return true;
            hash = corpusHash;
        } else {
            String taggedCorpusHash = HashUtils.generateMd5Hash(corpus);
            if (taggedHash == null)
                taggedHash = taggedCorpusHash;
            if (taggedHash.equals(taggedCorpusHash))
                return true;
            taggedHash = taggedCorpusHash;
        }

        LOGGER.info("Executing with corpus with different hash than last execution. Can't continue with state of last execution.");
        return false;
    }

    private Training readTraining(String key) throws Exception {
        String value = readNextValue("training");
        try {
            return Training.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            String possibleValues = "'"
                    + StringUtils.join(Training.values(), "', '") + "'";
            try (Formatter f = new Formatter()) {
                f.format("Illegal training value on line '%d' in file '%s'.%n",
                        lineNo, file);
                f.format("Possible values are: %s.%n", possibleValues);
                f.format("Found value '%s' instead.", value);
                throw new Exception(f.toString());
            }
        }
    }

    private Set<Pattern> readCounted(String key) throws Exception {
        String value = readNextValue(key);
        Set<Pattern> result = new HashSet<Pattern>();
        if (value == null)
            return result;
        for (String patternStr : StringUtils.splitAtChar(value, ','))
            result.add(readPattern(patternStr));
        return result;
    }

    private Map<Pattern, Set<String>> readChunked(String section) throws Exception {
        assertNextSection(section);

        Map<Pattern, Set<String>> result = new HashMap<Pattern, Set<String>>();
        while (peekNextLine() == NextLine.KEYVALUE) {
            Entry<String, String> entry = readNextKeyValue();
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null)
                continue;

            Set<String> chunks = new HashSet<String>();
            for (String chunkStr : StringUtils.splitAtChar(value, ','))
                chunks.add(chunkStr);

            Pattern pattern = readPattern(key);
            if (result.containsKey(pattern))
                try (Formatter f = new Formatter()) {
                    f.format("Illegal key on line '%d' in file '%s'.%n",
                            lineNo, file);
                    f.format(
                            "Key '%s' was already found previously for section '%s'.",
                            key, section);
                }
            result.put(pattern, chunks);
        }
        return result;
    }

    private boolean readBoolean(String key) throws Exception {
        String value = readNextValue(key);
        return Boolean.parseBoolean(value);
    }

    private Map<String, Set<Pattern>> readQueryCacheCounted(String section) throws Exception {
        assertNextSection(section);

        Map<String, Set<Pattern>> result = new HashMap<String, Set<Pattern>>();
        while (peekNextLine() == NextLine.KEYVALUE) {
            Entry<String, String> entry = readNextKeyValue();
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null)
                continue;

            Set<Pattern> patterns = new HashSet<Pattern>();
            for (String patternStr : StringUtils.splitAtChar(value, ','))
                patterns.add(readPattern(patternStr));

            if (result.containsKey(key))
                try (Formatter f = new Formatter()) {
                    f.format("Illegal key on line '%d' in file '%s'.%n",
                            lineNo, file);
                    f.format(
                            "Key '%s' was already found previously for section '%s'.",
                            key, section);
                }
            result.put(key, patterns);
        }
        return result;
    }

    private Pattern readPattern(String patternStr) {
        try {
            return Patterns.get(patternStr);
        } catch (RuntimeException e) {
            // TODO: Better error.
            throw e;
        }
    }

    private void writeStatusToFile() throws IOException {
        Path tmpFile = Paths.get(file + ".tmp");
        Files.deleteIfExists(tmpFile);
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile,
                Constants.CHARSET)) {
            writeKeyValue(writer, "hash", hash);
            writeKeyValue(writer, "taggedHash", taggedHash);
            writeKeyValue(writer, "training", training.toString());
            writeSet(writer, "absoluteCounted", absoluteCounted);
            writeSet(writer, "continuationCounted", continuationCounted);
            writeBoolean(writer, "nGramTimesCounted", nGramTimesCounted);
            writeBoolean(writer, "lengthDistributionCalculated",
                    lengthDistributionCalculated);
            writeMapSet(writer, "absoluteChunked", absoluteChunked);
            writeMapSet(writer, "continuationChunked", continuationChunked);
            writeMapSet(writer, "queryCacheCounted", queryCacheCounted);
        }

        Files.deleteIfExists(file);
        Files.move(tmpFile, file);
    }

    private void writeSection(BufferedWriter writer,
                              String section) throws IOException {
        writer.append('\n').append(section).append(":\n");
    }

    private <T, V> void writeKeyValue(BufferedWriter writer,
                                      T key,
                                      V value) throws IOException {
        String valueStr = value != null ? value.toString() : "null";
        writer.append(key.toString()).append(" = ").append(valueStr).append(
                '\n');
    }

    private <T> void writeBoolean(BufferedWriter writer,
                                  T key,
                                  boolean value) throws IOException {
        writeKeyValue(writer, key, Boolean.toString(value));
    }

    private <T, V> void writeSet(BufferedWriter writer,
                                 T key,
                                 Set<V> set) throws IOException {
        writeKeyValue(writer, key, StringUtils.join(set, ","));
    }

    private <T, V> void writeMapSet(BufferedWriter writer,
                                    String section,
                                    Map<T, Set<V>> mapSet) throws IOException {
        writeSection(writer, section);
        for (Entry<T, Set<V>> entry : mapSet.entrySet()) {
            T key = entry.getKey();
            Set<V> set = entry.getValue();
            writeSet(writer, key, set);
        }
    }
}
