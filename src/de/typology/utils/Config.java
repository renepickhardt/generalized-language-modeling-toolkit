package de.typology.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

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

	// CONTROLL PARAMETERS
	public boolean parseWiki;
	public boolean sampleSplitData;

	// STEP 0 GLOBAL CONFIGS
	public String wikiInputDirectory;
	public String wikiOutputDirectory;
	public int memoryLimitForWritingFiles;
	public int nGramLength;

	// STEP 2 SAMPLING AND MAKE TRAININGS DATA SPLIT
	public int sampleRate; // \in [0, 100] 0 means no data from input will be
	// used. 100 means all input data will be used
	public int splitDataRatio; // \in [0, 100] 0 means no training data. 100
	// means only training data

	// OPTIONAL only used in standalone scripts
	public String trainingPath;
	public String testingPath;

	// STEP3 ngrams & typology edges
	public boolean createNGramChunks;
	public boolean createSecondLevelNGramChunks;
	public boolean aggregateNGramChunks;
	public boolean sortNGrams;
	public boolean generateNGramDistribution;
	public boolean normalizeNGrams;
	// same flags for typology edge creating as for ngram building
	public boolean createTypologyEdgeChunks;
	public boolean createSecondLevelTypologyEdgeChunks;
	public boolean aggregateTypologyEdgeChunks;
	public boolean sortTypologyEdges;
	public boolean generateTypologyEdgeDistribution;
	public boolean normalizeEdges;

	// INPUT
	// should be the output of step1

	// temporary files
	public String parsedNGrams;
	public String sortedNGrams;
	public int fileChunkThreashhold;
	public String nGramKeyFile;
	public String nGramsNotAggregatedPath;
	public String typologyEdgesPathNotAggregated;

	// public String inputPath;
	// public String parsedDataPath;
	// public String nGramPath;
	// public String typologyEdgesPath;

	// STEP 1 PARSING DATA SETS used in de.typology.lexerparser:

	// INPUT:
	public String wikiXmlPath;
	public String reutersXmlPath;
	public String enronPath;
	public String DGTTMPath;
	public String ngramDownloadPath;

	// OUTPUT:
	public String parsedWikiOutputPath;
	public String parsedReutersOutputPath;
	public String parsedEnronOutputPath;
	public String parsedDGTTMOutputPath;
	public String DGTTMLanguage;

	public String normalizedWikiOutputPath;
	public String normalizedReutersOutputPath;
	public String normalizedEnronOutputPath;
	public String normalizedDGTTMOutputPath;
	public String ngramDownloadOutputPath;

	// COULD NOT REALLY MATCH
	public String wikiLinksOutputPath;
	public String wikiLinksHead;
	public String germanWikiText;
	// used in de.typology.googleNgrams:
	public String googleNgramsPath;
	public String googleNgramsMergedPath;

	// STEP 3 BUILDING NGRAMS TYPOEDGES AND SPLIT THE DATA SET
	// used in de.typology.nGramBuilder

	// CONFIG
	// Flags for ngram building

	// OUTPUT
	public String nGramsAggregatedPath;

	// used in de.typology.trainers
	public String edgeInput;
	public String normalizedEdges;

	public String nGramsInput;
	public String normalizedNGrams;

	public String indexPath;
	public String nGramIndexPath;

	// used in de.typology.utils/WordCounter
	public String wordCountInput;
	public String wordCountStats;
	// used in de.typology.utils/LineCounter
	public String lineCountInput;
	public String lineCountStats;

	public String parsedGoogleNGramsOutputPath;
	public String normalizedGoogleNgramsPath;

	private static final long serialVersionUID = -4439565094382127683L;

	static Config instance = null;

	public Config() {
		String file = "config.txt";
		try {
			BufferedInputStream stream = new BufferedInputStream(
					new FileInputStream(file));
			this.load(stream);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.initialize();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fills all fields with the data defined in the config file.
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void initialize() throws IllegalArgumentException,
			IllegalAccessException {
		Field[] fields = this.getClass().getFields();
		for (Field f : fields) {
			if (this.getProperty(f.getName()) == null) {
				System.err.print("Property '" + f.getName()
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
			instance = new Config();
		}
		return instance;
	}
}
