package de.typology.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an interface class to the Config file for this project. For each
 * class field one java property must be defined in config.txt. The fields will
 * be automatically filled!
 * 
 * Allowed Types are String, int, boolean, String[] and long[] where arrays are
 * defined by semicolon-separated Strings like "array=a;b;c" boolen fields are
 * initialized with true or false
 * 
 * lines starting with # will be ignored and can serve as comments
 * 
 * @author Jonas Kunze, Rene Pickhardt
 * 
 */
public class Config extends Properties {

    private static final long serialVersionUID = -4439565094382127683L;

    private static Logger logger = LoggerFactory.getLogger(Config.class);

    private static Config instance = null;

    // START CONFIG VALUES

    // BASIC SETTINGS //////////////////////////////////////////////////////////

    /**
     * dir from which we will start to work
     */
    public String outputDir;

    /**
     * length of the model to be trained
     */
    public int modelLength;

    /**
     * amount of threads that should be concurrently assigned to the program
     * needs to be >= 2
     */
    public int numberOfCores;

    /**
     * name of the input data set (this is supposed to be a subfolder of
     * outputDir) in this folder the trainingfile should be named
     * normalized.txt and should contain one sentence per line.
     */
    public String inputDataSet;

    /**
     * can be used for multiple languages
     */
    public String languages;

    // STAGES //////////////////////////////////////////////////////////////////

    /**
     * first the data sets are split to training and test data
     */
    public boolean splitData;

    /**
     * state if the index of words should be build. The index is used to create
     * subfiles for counting and aggregating sequences
     */
    public boolean buildIndex;

    /**
     * split trainingsdata into sequences
     */
    public boolean buildSequences;

    /**
     * if the absolute values for skipped sequences should be build
     */
    public boolean buildGLM;

    /**
     * states if also all the continuation values should be build.
     */
    public boolean buildContinuationGLM;

    /**
     * the absolute counts and continuation counts from the entire LM which are
     * needed for the testing-samples will be extracted and stored in
     * testing-samples/ pay attention. If your testing-samples are too large you
     * might run out of memory when running the experiment since all the data
     * needed will be stored into main memory
     */
    public boolean extractContinuationGLM;

    /**
     * set this to true if you want to build a standard kneser ney (generalized)
     * language model
     */
    public boolean buildKneserNey;

    /**
     * set this to true if you want to build a modified kneser ney (generalized)
     * language model
     * 
     * will only be build if buildKneserNey is also true
     */
    public boolean buildModKneserNey;

    /**
     * currently unused since there is currently an accompanying python
     * script for the task
     */
    public boolean calculateEntropy;

    /**
     * calculate a standard language model
     */
    public boolean kneserNeySimple;

    /**
     * calculate a generalized language model
     */
    public boolean kneserNeyComplex;

    /**
     * use absolute discounting for interpolated probabilities (this should be
     * set to false for the standard (modified) kneser ney implementation)
     */
    public boolean backoffAbsolute;

    /**
     * don't use any smoothing but just calculate conditional probabilities.
     */
    public boolean conditionalProbabilityOnly;

    // MISC ////////////////////////////////////////////////////////////////////

    /**
     * should be used to save space
     */
    public boolean deleteTempFiles;

    /**
     * whether training should be part of speech tagged, and whether pos
     * information should be used later on
     */
    public boolean withPos;

    /**
     * .tagger file used for tagging
     */
    public String model;

    /**
     * if n-1 tokens should be added before and after sentences
     */
    public boolean surroundWithTokens;

    /**
     * sort absolute and continuation counts
     */
    public boolean sortCounts;

    /**
     * number of decimal places that will be used for calculation of smoothing
     * algorithms
     */
    public int decimalPlaces;

    // TRAINING DATA ///////////////////////////////////////////////////////////

    /**
     * number of test queries which will be sampled from the test query set
     */
    public int numberOfQueries;

    /**
     * used for splitting files in which the skipped ngrams are stored and for
     * index building
     */
    public int maxWordCountDivider;

    public int maxPosCountDivier;

    // SPLITS //////////////////////////////////////////////////////////////////

    /**
     * 20 means that only 20% of the input data will be thrown away
     * 
     * in [0, 100] 0 means no data from input will be used. 100 means all input
     * data will be used
     */
    public int sampleRate;

    /**
     * 90 means that 90% of data will be training data
     * 
     * in [0, 100] 0 means no training data. 100 means only training data
     */
    public int splitDataRatio;

    /**
     * in [0, 100] 0 means all data is stored in test file. 100 means all data
     * is stored in (smaller) learning file
     */
    public int splitTestRatio;

    // END CONFIG VALUES

    private Config() throws IOException, IllegalArgumentException,
            IllegalAccessException {
        String file = "config.txt";
        try (BufferedReader reader =
                Files.newBufferedReader(Paths.get(file),
                        Charset.defaultCharset())) {
            load(reader);
        }
        initialize();
    }

    /**
     * Fills all fields with the data defined in the config file.
     */
    private void initialize() throws IllegalArgumentException,
            IllegalAccessException {
        Field[] fields = getClass().getFields();
        for (Field f : fields) {
            if (this.getProperty(f.getName()) == null) {
                logger.error("Property '" + f.getName()
                        + "' not defined in config file");
            }
            if (f.getType().equals(String.class)) {
                f.set(this, this.getProperty(f.getName()));
            } else if (f.getType().equals(long.class)) {
                f.setLong(this, Long.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(int.class)) {
                f.setInt(this, Integer.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(boolean.class)) {
                f.setBoolean(this,
                        Boolean.valueOf(this.getProperty(f.getName())));
            } else if (f.getType().equals(String[].class)) {
                f.set(this, this.getProperty(f.getName()).split(";"));
            } else if (f.getType().equals(int[].class)) {
                String[] tmp = this.getProperty(f.getName()).split(";");
                int[] ints = new int[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    ints[i] = Integer.parseInt(tmp[i]);
                }
                f.set(this, ints);
            } else if (f.getType().equals(long[].class)) {
                String[] tmp = this.getProperty(f.getName()).split(";");
                long[] longs = new long[tmp.length];
                for (int i = 0; i < tmp.length; i++) {
                    longs[i] = Long.parseLong(tmp[i]);
                }
                f.set(this, longs);
            }
        }
    }

    public static Config get() {
        if (instance == null) {
            try {
                instance = new Config();
            } catch (IllegalArgumentException | IllegalAccessException
                    | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }
}
