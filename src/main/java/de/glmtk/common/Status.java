/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.common;

import java.beans.IntrospectionException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.Event.ID;
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
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.exceptions.WrongStatusException;
import de.glmtk.logging.Logger;
import de.glmtk.util.AbstractYamlParser;
import de.glmtk.util.HashUtils;
import de.glmtk.util.StringUtils;

public class Status {
    private static final Logger LOGGER = Logger.get(Status.class);

    public static enum Training {
        NONE,
        UNTAGGED,
        TAGGED;
    }

    private static class Model {
        private boolean discounts = false;
        private Set<Pattern> alphas = new TreeSet<>();
        private Set<Pattern> lambdas = new TreeSet<>();

        public boolean getDiscounts() {
            return discounts;
        }

        public void setDiscounts(boolean discounts) {
            this.discounts = discounts;
        }

        public Set<Pattern> getAlphas() {
            return alphas;
        }

        public void setAlphas(Set<Pattern> alphas) {
            this.alphas = alphas;
        }

        public void addAlpha(Pattern pattern) {
            alphas.add(pattern);
        }

        public Set<Pattern> getLambdas() {
            return lambdas;
        }

        public void setLambdas(Set<Pattern> lambdas) {
            this.lambdas = lambdas;
        }

        public void addLambda(Pattern pattern) {
            lambdas.add(pattern);
        }

        /**
         * For logging and debugging.
         *
         * <p>
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "{discounts=" + Boolean.toString(discounts) + ", alphas"
                    + alphas.toString() + ", lambdas=" + lambdas.toString()
                    + "}";
        }
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

        /**
         * For logging and debugging.
         *
         * <p>
         * {@inheritDoc}
         */
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
                result.add(getProperty(type, "ngramTimesCounted",
                        BeanAccess.FIELD));
                result.add(getProperty(type, "lengthDistribution",
                        BeanAccess.FIELD));
                result.add(getProperty(type, "models", BeanAccess.FIELD));
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

        private class RepresentModel implements Represent {
            @Override
            public Node representData(Object data) {
                Model model = (Model) data;
                Map<String, Object> represent = new LinkedHashMap<>();
                represent.put("discounts", model.getDiscounts());
                represent.put("alphas", model.getAlphas());
                represent.put("lambdas", model.getLambdas());
                return representMapping(Tag.MAP, represent, false);
            }
        }

        private class RepresentQueryCache implements Represent {
            @Override
            public Node representData(Object data) {
                QueryCache queryCache = (QueryCache) data;
                Map<String, Set<Pattern>> represent = new LinkedHashMap<>();
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
            representers.put(Model.class, new RepresentModel());
            representers.put(QueryCache.class, new RepresentQueryCache());
        }
    }

    private class StatusParser extends AbstractYamlParser {
        public StatusParser(Path file) {
            super(file, "status");
        }

        @Override
        protected void parse() {
            parseBegining("!status");
            parseStatus();
            parseEnding();
        }

        private void parseStatus() {
            Map<String, Boolean> keys = createValidKeysMap("hash",
                    "taggedHash", "training", "counted", "chunked",
                    "ngramTimesCounted", "lengthDistribution", "models",
                    "queryCaches");

            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                String key = parseScalar();
                registerKey(keys, key);

                nextEvent();
                switch (key) {
                    case "hash":
                        hash = parseScalar();
                        break;

                    case "taggedHash":
                        taggedHash = parseScalar();
                        break;

                    case "training":
                        String trainingStr = parseScalar();
                        try {
                            training = Training.valueOf(trainingStr);
                        } catch (IllegalArgumentException e) {
                            List<String> possible = new ArrayList<>();
                            for (Training training : Training.values())
                                possible.add(training.toString());
                            throw newFileFormatException(
                                    "Illegal training value: '%s'. Possible values: '%s'.",
                                    trainingStr, StringUtils.join(possible,
                                            "', '"));
                        }
                        break;

                    case "counted":
                        counted = parseSetPattern();
                        break;

                    case "chunked":
                        chunked = parseMapPatternSetScalar();
                        break;

                    case "ngramTimesCounted":
                        ngramTimesCounted = parseBoolean();
                        break;

                    case "lengthDistribution":
                        lengthDistribution = parseBoolean();
                        break;

                    case "models":
                        parseModels();
                        break;

                    case "queryCaches":
                        parseQueryCaches();
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                nextEvent();
            }
        }

        private Pattern parsePattern() {
            String patternStr = parseScalar();
            try {
                return Patterns.get(patternStr);
            } catch (IllegalArgumentException e) {
                throw newFileFormatException("Illegal pattern: '%s'. %s",
                        patternStr, e.getMessage());
            }
        }

        private Set<Pattern> parseSetPattern() {
            assertEventIsId(ID.SequenceStart);

            Set<Pattern> result = new TreeSet<>();
            nextEvent();
            while (!event.is(ID.SequenceEnd)) {
                result.add(parsePattern());
                nextEvent();
            }
            return result;
        }

        private Map<Pattern, Set<String>> parseMapPatternSetScalar() {
            assertEventIsId(ID.MappingStart);

            Map<Pattern, Set<String>> result = new TreeMap<>();
            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                Pattern pattern = parsePattern();
                if (result.containsKey(pattern))
                    throw newFileFormatException(
                            "Map contains pattern multiple times as key: '%s'.",
                            pattern);
                nextEvent();
                Set<String> scalars = parseSetScalar();
                result.put(pattern, scalars);
                nextEvent();
            }
            return result;
        }

        private void parseModels() {
            assertEventIsId(ID.MappingStart);

            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                String name = parseScalar();
                if (models.containsKey(name))
                    throw newFileFormatException(
                            "Model name occurs multiple time: '%s'.", name);
                nextEvent();
                Model model = parseModel();
                models.put(name, model);
                nextEvent();
            }
        }

        private Model parseModel() {
            assertEventIsId(ID.MappingStart);

            Model result = new Model();

            Map<String, Boolean> keys = createValidKeysMap("discounts",
                    "alphas", "lambdas");

            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                String key = parseScalar();
                registerKey(keys, key);

                nextEvent();
                switch (key) {
                    case "discounts":
                        result.setDiscounts(parseBoolean());
                        break;

                    case "alphas":
                        result.setAlphas(parseSetPattern());
                        break;

                    case "lambdas":
                        result.setLambdas(parseSetPattern());
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                nextEvent();
            }
            return result;
        }

        private void parseQueryCaches() {
            assertEventIsId(ID.MappingStart);

            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                String name = parseScalar();
                if (queryCaches.containsKey(name))
                    throw newFileFormatException(
                            "QueryCache name occurs multiple times: '%s'.",
                            name);
                nextEvent();
                QueryCache queryCache = parseQueryCache();
                queryCaches.put(name, queryCache);
                nextEvent();
            }
        }

        private QueryCache parseQueryCache() {
            assertEventIsId(ID.MappingStart);

            QueryCache result = new QueryCache();

            Map<String, Boolean> keys = createValidKeysMap("counted");

            nextEvent();
            while (!event.is(ID.MappingEnd)) {
                String key = parseScalar();
                registerKey(keys, key);

                nextEvent();
                switch (key) {
                    case "counted":
                        result.setCounted(parseSetPattern());
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                nextEvent();
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
    private boolean ngramTimesCounted;
    private boolean lengthDistribution;
    private Map<String, Model> models;
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

    public synchronized Training getTraining() {
        return training;
    }

    public synchronized void setTraining(Training training) throws IOException {
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

    public synchronized Set<Pattern> getCounted() {
        return Collections.unmodifiableSet(counted);
    }

    public synchronized Set<Pattern> getCounted(boolean absolute) {
        return Collections.unmodifiableSet(counted(absolute));
    }

    public synchronized Set<Pattern> getChunkedPatterns(boolean absolute) {
        return Collections.unmodifiableSet(chunked(absolute).keySet());
    }

    public synchronized Set<String> getChunksForPattern(Pattern pattern) {
        boolean isAbsolute = pattern.isAbsolute();
        return Collections.unmodifiableSet(chunked(isAbsolute).get(pattern));
    }

    public synchronized void setChunksForPattern(Pattern pattern,
                                                 Collection<String> chunks) throws IOException {
        boolean isAbsolute = pattern.isAbsolute();
        chunked.put(pattern, new TreeSet<>(chunks));
        chunked(isAbsolute).put(pattern, new TreeSet<>(chunks));
        writeStatusToFile();
    }

    public synchronized void performMergeForChunks(Pattern pattern,
                                                   Collection<String> mergedChunks,
                                                   String mergeFile) throws IOException {
        boolean isAbsolute = pattern.isAbsolute();
        Set<String> chunks = chunked.get(pattern);
        chunks.removeAll(mergedChunks);
        chunks.add(mergeFile);
        chunks = chunked(isAbsolute).get(pattern);
        chunks.removeAll(mergedChunks);
        chunks.add(mergeFile);
        writeStatusToFile();
    }

    public synchronized void finishMerge(Pattern pattern) throws IOException {
        boolean isAbsolute = pattern.isAbsolute();
        ngramTimesCounted = false;
        lengthDistribution = false;
        counted.add(pattern);
        counted(isAbsolute).add(pattern);
        chunked.remove(pattern);
        chunked(isAbsolute).remove(pattern);
        writeStatusToFile();
    }

    private Set<Pattern> counted(boolean absolute) {
        return absolute ? absoluteCounted : continuationCounted;
    }

    private Map<Pattern, Set<String>> chunked(boolean absolute) {
        return absolute ? absoluteChunked : continuationChunked;
    }

    public synchronized boolean isNGramTimesCounted() {
        return ngramTimesCounted;
    }

    public synchronized void setNGramTimesCounted() throws IOException {
        ngramTimesCounted = true;
        writeStatusToFile();
    }

    public synchronized boolean isLengthDistribution() {
        return lengthDistribution;
    }

    public synchronized void setLengthDistribution() throws IOException {
        lengthDistribution = true;
        writeStatusToFile();
    }

    public synchronized boolean isModel(String name) {
        return models.containsKey(name);
    }

    public synchronized boolean areDiscountsCalculated(String model) {
        Model m = models.get(model);
        if (m == null)
            return false;
        return m.getDiscounts();
    }

    public synchronized void setDiscountsCalculated(String model) throws IOException {
        Model m = models.get(model);
        if (m == null) {
            m = new Model();
            models.put(model, m);
        }
        m.setDiscounts(true);
        writeStatusToFile();
    }

    public synchronized Set<Pattern> getAlphas(String model) {
        Model m = models.get(model);
        if (m == null)
            return new TreeSet<>();
        return m.getAlphas();
    }

    public synchronized void addAlpha(String model,
                                      Pattern pattern) throws IOException {
        Model m = models.get(model);
        if (m == null) {
            m = new Model();
            models.put(model, m);
        }
        m.addAlpha(pattern);
        writeStatusToFile();
    }

    public synchronized Set<Pattern> getLambdas(String model) {
        Model m = models.get(model);
        if (m == null)
            return new TreeSet<>();
        return m.getLambdas();
    }

    public synchronized void addLambda(String model,
                                       Pattern pattern) throws IOException {
        Model m = models.get(model);
        if (m == null) {
            m = new Model();
            models.put(model, m);
        }
        m.addLambda(pattern);
        writeStatusToFile();
    }

    public synchronized Set<Pattern> getQueryCacheCounted(String queryCache) {
        QueryCache qc = queryCaches.get(queryCache);
        if (qc == null)
            return new TreeSet<>();
        Set<Pattern> patterns = qc.getCounted();
        return patterns != null ? patterns : new TreeSet<Pattern>();
    }

    public synchronized void addQueryCacheCounted(String model,
                                                  Pattern pattern) throws IOException {
        QueryCache qc = queryCaches.get(model);
        if (qc == null) {
            qc = new QueryCache();
            queryCaches.put(model, qc);
        }
        qc.addCounted(pattern);
        writeStatusToFile();
    }

    public synchronized void logStatus() {
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
        LOGGER.debug("ngramTimesCounted            = %b", ngramTimesCounted);
        LOGGER.debug("lengthDistributionCalculated = %s", lengthDistribution);
        LOGGER.debug("models                       = %s", models);
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
        ngramTimesCounted = false;
        lengthDistribution = false;
        models = new TreeMap<>();
        queryCaches = new TreeMap<>();
    }

    private void checkWithFileSystem() {
        checkCounted("counting", counted, paths.getAbsoluteDir(),
                paths.getContinuationDir());
        checkChunked("absolute chunking", absoluteChunked,
                paths.getAbsoluteChunkedDir());
        checkChunked("continuation chunking", continuationChunked,
                paths.getContinuationChunkedDir());
        if (ngramTimesCounted && !Files.exists(paths.getNGramTimesFile()))
            throw new WrongStatusException("ngram times counting",
                    paths.getNGramTimesFile());
        if (lengthDistribution
                && !Files.exists(paths.getLengthDistributionFile()))
            throw new WrongStatusException("length distribution calculating",
                    paths.getLengthDistributionFile());
        for (Entry<String, QueryCache> entry : queryCaches.entrySet()) {
            String name = entry.getKey();
            QueryCache queryCache = entry.getValue();

            GlmtkPaths queryCachePaths = paths.newQueryCache(name);

            checkCounted("queryCache " + name + " counting",
                    queryCache.getCounted(), queryCachePaths.getAbsoluteDir(),
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
                throw new WrongStatusException(name + " pattern " + pattern,
                        patternFile);
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
                    throw new WrongStatusException(
                            name + " pattern " + pattern, chunkFile);
            }
        }
    }

    private void readStatusFromFile() throws IOException {
        LOGGER.debug("Reading status from file '%s'.", file);

        new StatusParser(file).run();

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
