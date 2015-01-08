package de.glmtk.common;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.counting.Tagger;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.exceptions.StatusNotAccurateException;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.util.AbstractYamlParser;
import de.glmtk.util.HashUtils;
import de.glmtk.util.StringUtils;

public class Status {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Status.class);

    public static enum Training {
        NONE,
        UNTAGGED,
        TAGGED;
    }

    private static class QueryCache {
        private Set<Pattern> counted = new TreeSet<>();

        public Set<Pattern> getCounted() {
            return counted;
        }

        public void setCounted(Set<Pattern> counted) {
            this.counted = counted;
        }

        public void addCounted(Pattern pattern) {
            counted.add(pattern);
        }

        @Override
        public String toString() {
            return "{counted=" + counted.toString() + "}";
        }
    }

    private static class StatusRepresenter extends Representer {
        private class OrderedPropertyUtils extends PropertyUtils {
            @Override
            protected Set<Property> createPropertySet(Class<?> type,
                                                      BeanAccess beanAccess) throws IntrospectionException {
                Set<Property> result = new LinkedHashSet<>();
                result.add(getProperty(type, "hash", BeanAccess.FIELD));
                result.add(getProperty(type, "taggedHash", BeanAccess.FIELD));
                result.add(getProperty(type, "training", BeanAccess.FIELD));
                result.add(getProperty(type, "counted", BeanAccess.FIELD));
                result.add(getProperty(type, "chunked", BeanAccess.FIELD));
                result.add(getProperty(type, "nGramTimesCounted",
                        BeanAccess.FIELD));
                result.add(getProperty(type, "lengthDistribution",
                        BeanAccess.FIELD));
                result.add(getProperty(type, "queryCaches", BeanAccess.FIELD));
                return result;
            }
        }

        private class RepresentSet implements Represent {
            @Override
            public Node representData(Object data) {
                return representSequence(Tag.SEQ, ((Set<?>) data), true);
            }
        }

        private class RepresentPattern implements Represent {
            @Override
            public Node representData(Object data) {
                Pattern pattern = (Pattern) data;
                for (char c : pattern.toString().toCharArray())
                    if (!Character.isDigit(c))
                        return representScalar(Tag.STR, data.toString());
                return representScalar(Tag.INT, data.toString());
            }
        }

        private class RepresentQueryCache implements Represent {
            @Override
            public Node representData(Object data) {
                QueryCache queryCache = (QueryCache) data;
                Map<String, Set<Pattern>> represent = new TreeMap<>();
                represent.put("counted", queryCache.getCounted());
                return representMapping(Tag.MAP, represent, false);
            }
        }

        public StatusRepresenter() {
            super();
            setPropertyUtils(new OrderedPropertyUtils());
            addClassTag(Status.class, new Tag("!status"));
            representers.put(TreeSet.class, new RepresentSet());
            representers.put(Pattern.class, new RepresentPattern());
            representers.put(QueryCache.class, new RepresentQueryCache());
        }
    }

    private class StatusParser extends AbstractYamlParser {
        public void parseStatus(Event event,
                                Iterator<Event> iter) {
            Map<String, Boolean> keys = createValidKeysMap("hash",
                    "taggedHash", "training", "counted", "chunked",
                    "nGramTimesCounted", "lengthDistribution", "queryCaches");

            parseBegining(event, iter, "!status");
            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                if (!event.is(ID.Scalar))
                    throw new FileFormatException("Expected SclarEvent.");

                String key = ((ScalarEvent) event).getValue();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "hash":
                        hash = parseScalar(event, iter);
                        break;

                    case "taggedHash":
                        taggedHash = parseScalar(event, iter);
                        break;

                    case "training":
                        try {
                            training = Training.valueOf(parseScalar(event, iter));
                        } catch (IllegalArgumentException e) {
                            throw new FileFormatException(
                                    "Illegal training value. Possible values: TODO.");
                        }
                        break;

                    case "counted":
                        counted = parseSetPattern(event, iter);
                        break;

                    case "chunked":
                        chunked = parseMapPatternSetScalar(event, iter);
                        break;

                    case "nGramTimesCounted":
                        nGramTimesCounted = parseBoolean(event, iter);
                        break;

                    case "lengthDistribution":
                        lengthDistribution = parseBoolean(event, iter);
                        break;

                    case "queryCaches":
                        parseQueryCaches(event, iter);
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
            parseEnding(event, iter);
        }

        private Pattern parsePattern(Event event,
                                     Iterator<Event> iter) {
            String patternStr = parseScalar(event, iter);
            try {
                return Patterns.get(patternStr);
            } catch (IllegalArgumentException e) {
                throw new FileFormatException("Illegal pattern.");
            }
        }

        private Set<Pattern> parseSetPattern(Event event,
                                             Iterator<Event> iter) {
            if (!event.is(ID.SequenceStart))
                throw new FileFormatException("Expected SequenceStart.");

            Set<Pattern> result = new TreeSet<>();
            event = iter.next();
            while (!event.is(ID.SequenceEnd)) {
                result.add(parsePattern(event, iter));
                event = iter.next();
            }
            return result;
        }

        private Map<Pattern, Set<String>> parseMapPatternSetScalar(Event event,
                                                                   Iterator<Event> iter) {
            if (!event.is(ID.MappingStart))
                throw new FileFormatException("Expected MappingStart.");

            Map<Pattern, Set<String>> result = new TreeMap<>();
            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                Pattern pattern = parsePattern(event, iter);
                Set<String> scalars = parseSetScalar(iter.next(), iter);
                if (result.containsKey(pattern))
                    throw new FileFormatException("Duplicate pattern in map.");
                result.put(pattern, scalars);
                event = iter.next();
            }
            return result;
        }

        private void parseQueryCaches(Event event,
                                      Iterator<Event> iter) {
            if (!event.is(ID.MappingStart))
                throw new FileFormatException("Expected MappingStart.");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                String name = parseScalar(event, iter);
                if (queryCaches.containsKey(name))
                    throw new IllegalArgumentException("Duplicate key.");
                event = iter.next();
                QueryCache queryCache = parseQueryCache(event, iter);
                queryCaches.put(name, queryCache);
                event = iter.next();
            }
        }

        private QueryCache parseQueryCache(Event event,
                                           Iterator<Event> iter) {
            if (!event.is(ID.MappingStart))
                throw new FileFormatException("Expected MappingStart.");

            QueryCache result = new QueryCache();

            Map<String, Boolean> keys = createValidKeysMap("counted");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                if (!event.is(ID.Scalar))
                    throw new FileFormatException("Expected ScalarEvent.");

                String key = ((ScalarEvent) event).getValue();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "counted":
                        Set<Pattern> patterns = parseSetPattern(event, iter);
                        result.setCounted(patterns);
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
            return result;
        }
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
    private Map<Pattern, Set<String>> chunked;
    private Map<Pattern, Set<String>> absoluteChunked;
    private Map<Pattern, Set<String>> continuationChunked;
    private boolean nGramTimesCounted;
    private boolean lengthDistribution;
    private Map<String, QueryCache> queryCaches;

    public Status(GlmtkPaths paths,
                  Path corpus) throws IOException {
        this.paths = paths;
        file = paths.getStatusFile();
        this.corpus = corpus;
        corpusTagged = Tagger.detectFileTagged(corpus);

        setVoidSettings();
        if (Files.exists(file))
            readStatusFromFile();

        writeStatusToFile();
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
            chunked.put(pattern, new TreeSet<>(chunks));
            chunked(continuation).put(pattern, new TreeSet<>(chunks));
            writeStatusToFile();
        }
    }

    public void performMergeForChunks(boolean continuation,
                                      Pattern pattern,
                                      Collection<String> mergedChunks,
                                      String mergeFile) throws IOException {
        synchronized (this) {
            Set<String> chunks = chunked.get(pattern);
            chunks.removeAll(mergedChunks);
            chunks = chunked(continuation).get(pattern);
            chunks.removeAll(mergedChunks);
            chunks.add(mergeFile);
            writeStatusToFile();
        }
    }

    public void finishMerge(boolean continuation,
                            Pattern pattern) throws IOException {
        synchronized (this) {
            nGramTimesCounted = false;
            lengthDistribution = false;
            counted.add(pattern);
            counted(continuation).add(pattern);
            chunked.remove(pattern);
            chunked(continuation).remove(pattern);
            writeStatusToFile();
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

    public boolean isLengthDistribution() {
        return lengthDistribution;
    }

    public void setLengthDistribution() throws IOException {
        synchronized (this) {
            lengthDistribution = true;
            writeStatusToFile();
        }
    }

    public Set<Pattern> getQueryCacheCounted(String name) {
        synchronized (this) {
            QueryCache queryCache = queryCaches.get(name);
            if (queryCache == null)
                return new TreeSet<>();
            Set<Pattern> patterns = queryCache.getCounted();
            return patterns != null ? patterns : new TreeSet<Pattern>();
        }
    }

    public void addQueryCacheCounted(String name,
                                     Pattern pattern) throws IOException {
        synchronized (this) {
            QueryCache queryCache = queryCaches.get(name);
            if (queryCache == null) {
                queryCache = new QueryCache();
                queryCaches.put(name, queryCache);
            }
            queryCache.addCounted(pattern);
            writeStatusToFile();
        }
    }

    public void logStatus() {
        LOGGER.debug("Status %s", StringUtils.repeat("-",
                80 - "Status ".length()));
        LOGGER.debug("hash                         = %s", hash);
        LOGGER.debug("taggedHash                   = %s", taggedHash);
        LOGGER.debug("training                     = %s", training);
        LOGGER.debug("counted                      = %s", counted);
        LOGGER.debug("absoluteCounted              = %s", absoluteCounted);
        LOGGER.debug("continuationCounted          = %s", continuationCounted);
        LOGGER.debug("chunked                      = %s", chunked);
        LOGGER.debug("absoluteChunked              = %s", absoluteChunked);
        LOGGER.debug("continuationChunked          = %s", continuationChunked);
        LOGGER.debug("nGramTimesCounted            = %b", nGramTimesCounted);
        LOGGER.debug("lengthDistributionCalculated = %s", lengthDistribution);
        LOGGER.debug("queryCache                   = %s", queryCaches);
    }

    private void setVoidSettings() {
        training = Training.NONE;
        counted = new TreeSet<>();
        absoluteCounted = new TreeSet<>();
        continuationCounted = new TreeSet<>();
        chunked = new TreeMap<>();
        absoluteChunked = new TreeMap<>();
        continuationChunked = new TreeMap<>();
        nGramTimesCounted = false;
        lengthDistribution = false;
        queryCaches = new TreeMap<>();
    }

    private void checkWithFileSystem() {
        checkCounted("counted", counted, paths.getAbsoluteDir(),
                paths.getContinuationDir());
        checkChunked("absolute chunked", absoluteChunked,
                paths.getAbsoluteChunkedDir());
        checkChunked("continuation chunked", continuationChunked,
                paths.getContinuationChunkedDir());
        if (nGramTimesCounted && !Files.exists(paths.getNGramTimesFile()))
            throw new StatusNotAccurateException();
        if (lengthDistribution
                && !Files.exists(paths.getLengthDistributionFile()))
            throw new StatusNotAccurateException();
        for (Entry<String, QueryCache> entry : queryCaches.entrySet()) {
            String name = entry.getKey();
            QueryCache queryCache = entry.getValue();

            GlmtkPaths queryCachePaths = paths.newQueryCache(name);

            checkCounted("queryCache " + name, queryCache.getCounted(),
                    queryCachePaths.getAbsoluteDir(),
                    queryCachePaths.getContinuationDir());
        }
    }

    private void checkCounted(String name,
                              Set<Pattern> counted,
                              Path absoluteDir,
                              Path continuationDir) {
        for (Pattern pattern : counted) {
            Path countedDir = pattern.isAbsolute()
                    ? absoluteDir
                    : continuationDir;
            Path patternFile = countedDir.resolve(pattern.toString());
            if (!Files.exists(patternFile))
                throw new StatusNotAccurateException();
        }
    }

    private void checkChunked(String name,
                              Map<Pattern, Set<String>> chunked,
                              Path chunkedDir) {
        for (Entry<Pattern, Set<String>> entry : chunked.entrySet()) {
            Pattern pattern = entry.getKey();
            Set<String> chunks = entry.getValue();

            Path patternDir = chunkedDir.resolve(pattern.toString());
            for (String chunk : chunks) {
                Path chunkFile = patternDir.resolve(chunk);
                if (!Files.exists(chunkFile))
                    throw new StatusNotAccurateException();
            }
        }
    }

    private void readStatusFromFile() throws IOException {
        LOGGER.debug("Reading status from file '%s'.", file);

        Yaml yaml = new Yaml();
        try (BufferedReader reader = Files.newBufferedReader(file,
                Constants.CHARSET)) {
            Iterator<Event> iter = yaml.parse(reader).iterator();
            new StatusParser().parseStatus(iter.next(), iter);
        }

        for (Pattern pattern : counted)
            if (pattern.isAbsolute())
                absoluteCounted.add(pattern);
            else
                continuationCounted.add(pattern);

        for (Entry<Pattern, Set<String>> entry : chunked.entrySet()) {
            Pattern pattern = entry.getKey();
            Set<String> chunks = new TreeSet<>(entry.getValue());
            if (pattern.isAbsolute())
                absoluteChunked.put(pattern, chunks);
            else
                continuationChunked.put(pattern, chunks);
        }

        if (!validCorpusHash())
            setVoidSettings();
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

    private void writeStatusToFile() throws IOException {
        checkWithFileSystem();

        Path tmpFile = Paths.get(file + ".tmp");
        Files.deleteIfExists(tmpFile);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setWidth(80);
        dumperOptions.setIndent(4);
        dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new StatusRepresenter(), dumperOptions);

        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile,
                Constants.CHARSET)) {
            yaml.dump(this, writer);
        }

        Files.deleteIfExists(file);
        Files.move(tmpFile, file);
    }
}
