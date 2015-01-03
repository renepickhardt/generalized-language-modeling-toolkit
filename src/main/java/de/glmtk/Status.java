package de.glmtk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.util.HashUtils;
import de.glmtk.util.StringUtils;

public class Status {

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Status.class);

    public static enum Training {

        NONE, UNTAGGED, TAGGED;

    }

    private Glmtk glmtk;

    private Path file;

    private Path corpus;

    private boolean corpusTagged;

    private String hash;

    private String taggedHash;

    private Training training;

    private Set<Pattern> absoluteCounted;

    private Set<Pattern> continuationCounted;

    private Map<Pattern, Set<String>> absoluteChunked;

    private Map<Pattern, Set<String>> continuationChunked;

    private int lineNo;

    public Status(
            Glmtk glmtk,
            Path file,
            Path corpus) throws Exception {
        this.glmtk = glmtk;
        this.file = file;
        this.corpus = corpus;
        corpusTagged = detectCorpusTagged();

        setVoidSettings();
        if (Files.exists(file)) {
            readStatusFromFile();
        }
        writeStatusToFile();
    }

    private void setVoidSettings() {
        training = Training.NONE;
        absoluteCounted = new HashSet<Pattern>();
        continuationCounted = new HashSet<Pattern>();
        absoluteChunked = new HashMap<Pattern, Set<String>>();
        continuationChunked = new HashMap<Pattern, Set<String>>();
    }

    public void logStatus() {
        LOGGER.debug("Status %s", StringUtils.repeat("-", 80 - 7));
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

            String hash = HashUtils.generateMd5Hash(glmtk.getTrainingFile());
            if (training == Training.UNTAGGED) {
                if (this.hash == null) {
                    this.hash = hash;
                }
                if (this.hash.equals(hash)) {
                    writeStatusToFile();
                    return;
                }
                this.hash = hash;
            } else {
                if (taggedHash == null) {
                    taggedHash = hash;
                }
                if (taggedHash.equals(taggedHash)) {
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

    public Set<String>
        getChunksForPattern(boolean continuation, Pattern pattern) {
        synchronized (this) {
            return Collections.unmodifiableSet(chunked(continuation).get(
                    pattern));
        }
    }

    public void setChunksForPattern(
            boolean continuation,
            Pattern pattern,
            Collection<String> chunks) throws IOException {
        synchronized (this) {
            chunked(continuation).put(pattern,
                    new LinkedHashSet<String>(chunks));
            writeStatusToFile();
        }
    }

    public void performMergeForChunks(
            boolean continuation,
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

    public void finishMerge(boolean continuation, Pattern pattern)
            throws IOException {
        synchronized (this) {
            counted(continuation).add(pattern);
            chunked(continuation).remove(pattern);
            writeStatusToFile();
        }
    }

    private boolean detectCorpusTagged() throws IOException {
        try (BufferedReader reader =
                Files.newBufferedReader(corpus, Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                for (String tokenAndTag : StringUtils.splitAtChar(line, ' ')) {
                    int lastSlash = tokenAndTag.lastIndexOf('/');
                    if (lastSlash <= 0 || lastSlash == tokenAndTag.length() - 1) {
                        LOGGER.info(
                                "Detected Corpus untagged, because in line '%d' at least one token does not have the form '<token>/<pos>'.",
                                lineNo);
                        return false;
                    }
                }
            }
        }
        LOGGER.info("Detected Corpus tagged.");
        return true;
    }

    private void readStatusFromFile() throws Exception {
        LOGGER.debug("Reading status from file '%s'.", file);

        lineNo = 0;
        try (BufferedReader reader =
                Files.newBufferedReader(file, Constants.CHARSET)) {
            hash = readNextValue(reader, "hash");
            taggedHash = readNextValue(reader, "taggedHash");
            if (!validCorpusHash()) {
                setVoidSettings();
                return;
            }

            training = readTraining(readNextValue(reader, "training"));
            absoluteCounted =
                    readCounted(readNextValue(reader, "absoluteCounted"));
            continuationCounted =
                    readCounted(readNextValue(reader, "continuationCounted"));
            absoluteChunked =
                    readChunked(readNextValue(reader, "absoluteChunked"));
            continuationChunked =
                    readChunked(readNextValue(reader, "continuationChunked"));
        }
    }

    private String readNextValue(BufferedReader reader, String expectedKey)
            throws Exception {
        String line;
        do {
            line = reader.readLine();
            ++lineNo;

            if (line == null) {
                // EOF
                throw new Exception(String.format(
                        "Unexpected End of File in file '%s'.\n"
                                + "Expected key '%s'.", file, expectedKey));
            }
        } while (line.trim().isEmpty());

        List<String> split = StringUtils.splitAtChar(line, '=');
        if (split.size() != 2) {
            throw new Exception(String.format(
                    "Illegal line '%d' in file '%s'.\n"
                            + "Expected line to have format: '%s = <value>'.\n"
                            + "Line was: '%s'.", lineNo, file, expectedKey,
                    line));
        }

        String key = split.get(0).trim();
        String value = split.get(1).trim();

        if (!key.equals(expectedKey)) {
            throw new Exception(String.format(
                    "Illegal next key on line '%d' in file '%s'.\n"
                            + "Expected Key '%s', but was '%s'.\n"
                            + "Line was: '%s'.", lineNo, file, expectedKey,
                    key, line));
        }

        return value != "null" ? value : null;
    }

    private boolean validCorpusHash() throws IOException {
        if (!corpusTagged) {
            String corpusHash = HashUtils.generateMd5Hash(corpus);
            if (hash == null) {
                hash = corpusHash;
            }
            if (hash.equals(corpusHash)) {
                return true;
            }
            hash = corpusHash;
        } else {
            String taggedCorpusHash = HashUtils.generateMd5Hash(corpus);
            if (taggedHash == null) {
                taggedHash = taggedCorpusHash;
            }
            if (taggedHash.equals(taggedCorpusHash)) {
                return true;
            }
            taggedHash = taggedCorpusHash;
        }

        LOGGER.info("Executing with corpus with different hash than last execution. Can't continue with state of last execution.");
        return false;
    }

    private Training readTraining(String value) throws Exception {
        try {
            return Training.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            String possibleValues =
                    "'" + StringUtils.join(Training.values(), "', '") + "'";
            throw new Exception(String.format(
                    "Illegal training value '%s' on line '%d' in file '%s'.\n"
                            + "Possible values are: %s.", value, lineNo, file,
                    possibleValues));
        }
    }

    private Set<Pattern> readCounted(String value) {
        Set<Pattern> result = new HashSet<Pattern>();
        for (String patternStr : StringUtils.splitAtChar(value, ';')) {
            result.add(readPattern(patternStr));
        }
        return result;
    }

    private Map<Pattern, Set<String>> readChunked(String value)
            throws Exception {
        Map<Pattern, Set<String>> result = new HashMap<Pattern, Set<String>>();
        for (String patternAndChunks : StringUtils.splitAtChar(value, ';')) {
            List<String> split = StringUtils.splitAtChar(patternAndChunks, ':');
            if (split.size() != 2) {
                throw new Exception(
                        String.format(
                                "Illegal value on line '%d' in file '%s'.\n"
                                        + "Expected list of '<pattern>:<chunklist>' pairs.\n"
                                        + "Value was: '%s'.", lineNo, file,
                                value));
            }
            Pattern pattern = readPattern(split.get(0));
            Set<String> chunks =
                    new LinkedHashSet<String>(StringUtils.splitAtChar(
                            split.get(1), ','));
            result.put(pattern, chunks);
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
        try (BufferedWriter writer =
                Files.newBufferedWriter(tmpFile, Constants.CHARSET)) {
            writeKeyValue(writer, "hash", hash);
            writeKeyValue(writer, "taggedHash", taggedHash);
            writeKeyValue(writer, "training", training.toString());
            writeKeyValue(writer, "absoluteCounted",
                    serializeCounted(absoluteCounted));
            writeKeyValue(writer, "continuationCounted",
                    serializeCounted(continuationCounted));
            writeKeyValue(writer, "absoluteChunked",
                    serializedChunked(absoluteChunked));
            writeKeyValue(writer, "continuationChunked",
                    serializedChunked(continuationChunked));
        }

        Files.deleteIfExists(file);
        Files.move(tmpFile, file);
    }

    private void writeKeyValue(BufferedWriter writer, String key, String value)
            throws IOException {
        writer.append(String.format("%s = %s\n", key, value));
    }

    private String serializeCounted(Set<Pattern> counted) {
        return StringUtils.join(counted, ";");
    }

    private String serializedChunked(Map<Pattern, Set<String>> chunked) {
        List<String> patternAndChunks = new ArrayList<String>(chunked.size());
        for (Entry<Pattern, Set<String>> entry : chunked.entrySet()) {
            patternAndChunks.add(String.format("%s:%s", entry.getKey(),
                    StringUtils.join(entry.getValue(), ",")));
        }
        return StringUtils.join(patternAndChunks, ";");
    }

}
