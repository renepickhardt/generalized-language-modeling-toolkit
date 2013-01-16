package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import de.typology.stats.NGramDistribution;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * INPUT: Huge textfile with clean text.
 * 
 * OUTPUT: N grams as well as typology edges of distance 1,...,n
 * 
 * REMARK1: the N Gram Files are not sorted by frequency!
 * 
 * REMARK2: See Below to know which variables can and should be set in the
 * config file.
 * 
 * REMARK3: this programm can work on relativly few memory (less than 1 GB
 * though 6 GB is suggested) it does not run into "too many open files issues",
 * should be able to process almost 1 TB of text into N-grams and is also
 * relatively fast. The programm will not scale to arbitrary files though this
 * could be easily achieved by doing aggregation steps more frequently and
 * splitting ngram chunks further down. Even more speed could probably be
 * obtained by introducing good multi threading. Now multithreading is included
 * by the code of http://ostermiller.org/utils/Parallelizer.html Thanks!
 * 
 * To achieve the output the code does the following:
 * 
 * # reading text from some file and finding the top k most frequent case
 * sensitive starting letters.
 * 
 * # writing out all possible N grams to files. in the format
 * w_1\tw_2\t...\tw_n\n The N grams are written to a file starting with the
 * respective starting letter or are saved to a file 'other'
 * 
 * # looks through the N gram files. If a file is bigger than X MB (in our case
 * 512 MB) the file is divided into further chunks that correspond to the start
 * letter of the second word of the N gram. For reusability this function works
 * on any file with the format w_1\tw_2\t...\n
 * 
 * # an aggregation function will be able to aggregate common lines in the
 * produced files. This aggregation process is done via Hashmaps in Memory. One
 * reason why the unaggregated N gram Chunks need to be small and fit into
 * memory. The Format of the aggregation process is: w_1\tw_2\t...\tw_n\t#cnt\n
 * 
 * # now the typology edges are created. This is done in a similar manner as the
 * N Grams but for reusability issues creation of N Grams and Typology edges is
 * seperated
 * 
 * # first from every N Gram typology edges of distance 1,2,...,n-1 are written
 * to files named with the starting letter of the first word. Those edges also
 * take the count of the N Gram into the file format: w_1\tw_2\t#cnt. Every
 * typology edge type has its own folder with its files.
 * 
 * # Now the edges are further divided if necessary and afterwards aggregated
 * with the same methods that have been used for the N Grams.
 * 
 * aggregateFile(String inputFileWithPath, String outputFileWithPath); (in order
 * to be general this function needs also unaggregated ngrams with #1 at the end
 * or with a boolean switch flag)
 * 
 * countMostFrequentLetters()
 * 
 * createUnaggregatedNGramChunks(String fromFile, String toPath)
 * 
 * buildFurtherChunks(String inputFileWithPath, String outputPath)
 * 
 * buildFurtherChunks(String inputPath)
 * 
 * file extensions:
 * 
 * .5gc = 5gram chunk
 * 
 * .4gc = 4gram chunk and so on...
 * 
 * 1ec = 1edge chunk (standing for typology edges of type 1)
 * 
 * 2ec = 2edge chunk and so on...
 * 
 * the same extensions also exist with an 'a' instead of 'c' in this case the
 * chunks are aggreaged. The last version is that there will be an 's' instead
 * of 'a' or 'c' in this case the cunks are sorted descending by counts
 * 
 * @author rpickhardt
 * 
 */

public class NGramBuilder {

	/**
	 * 
	 * TODO:
	 * 
	 * * introduce a loop that goes through all data source files and start n
	 * gram and typoedge building
	 * 
	 * * keep track of output directories. make then different for each source
	 * data set
	 * 
	 * * keep even better track of output of processed results (like
	 * distributions)
	 * 
	 * * include gnuplot from here so plots are automatically generated
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

	}

	public static void run(String trainingPath, String trainingFile) {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;

		String[] letters = countMostFrequentStartingLetters(62, trainingFile,
				trainingPath);
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek + " seconds to: count most frequent letters");

		// wrap everyting into a cool programm that runs through all the data
		// sets that we have an creates the appropreate output

		for (int n = Config.get().nGramLength; n >= 2; n--) {
			new File(trainingPath + Config.get().nGramsNotAggregatedPath + "/"
					+ n).mkdirs();

			NGramChunkCreator ngcc = new NGramChunkCreator(letters);
			if (Config.get().createNGramChunks) {

				ngcc.createNGramChunks(trainingFile, n, trainingPath);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating first chunks");
			}
			if (Config.get().createSecondLevelNGramChunks) {
				ngcc.createSecondLevelChunks(trainingPath
						+ Config.get().nGramsNotAggregatedPath + "/" + n, "."
								+ n + "gc");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating detailed ngram chunks");
			}
			if (Config.get().aggregateNGramChunks) {
				Aggregator a = new Aggregator();
				a.aggregateNGrams(trainingPath
						+ Config.get().nGramsNotAggregatedPath + "/" + n, "."
								+ n + "gc", 3);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish aggregating ngrams");
			}

			if (Config.get().sortNGrams) {
				Sorter s = new Sorter();
				s.sort(trainingPath + Config.get().nGramsNotAggregatedPath
						+ "/" + n, "." + n + "ga");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish sorting aggregated ngrams");
			}

			if (Config.get().generateNGramDistribution) {
				NGramDistribution ngd = new NGramDistribution();
				ngd.countDistribution(trainingPath
						+ Config.get().nGramsNotAggregatedPath + "/" + n, "."
								+ n + "gs");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating distribution for ngrams");
			}

			new File(trainingPath + Config.get().typologyEdgesPathNotAggregated
					+ "/" + (n - 1)).mkdirs();
			if (Config.get().createTypologyEdgeChunks) {
				TypologyChunkCreator tcc = new TypologyChunkCreator(letters);
				tcc.createTypoelogyEdgeChunks(trainingFile, trainingPath
						+ Config.get().typologyEdgesPathNotAggregated, n - 1);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating typology edge chunks of type: "
						+ (n - 1));
			}

			if (Config.get().createSecondLevelTypologyEdgeChunks) {
				TypologyChunkCreator tcc = new TypologyChunkCreator(letters);
				tcc.createSecondLevelChunks(trainingPath
						+ Config.get().typologyEdgesPathNotAggregated + "/"
						+ (n - 1), "." + (n - 1) + "ec");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish putting typology edge chunks of type: "
						+ (n - 1) + " into smaller chunks");
			}

			if (Config.get().aggregateTypologyEdgeChunks) {
				Aggregator a = new Aggregator();
				a.aggregateNGrams(trainingPath
						+ Config.get().typologyEdgesPathNotAggregated + "/"
						+ (n - 1), "." + (n - 1) + "ec", 3);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish aggregating ngrams");
			}

			if (Config.get().sortTypologyEdges) {
				Sorter s = new Sorter();
				s.sort(trainingPath
						+ Config.get().typologyEdgesPathNotAggregated + "/"
						+ (n - 1), "." + (n - 1) + "ea");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish sorting aggregated ngrams");
			}

			if (Config.get().generateTypologyEdgeDistribution) {
				NGramDistribution ngd = new NGramDistribution();
				ngd.countDistribution(trainingPath
						+ Config.get().typologyEdgesPathNotAggregated + "/"
						+ (n - 1), "." + (n - 1) + "es");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating distribution for ngrams");
			}

		}

		if (Config.get().normalizeNGrams) {
			try {

				String input = trainingPath
						+ Config.get().nGramsNotAggregatedPath + "/";
				String output = trainingPath
						+ Config.get().nGramsNotAggregatedPath + "Normalized/";
				new File(output).mkdirs();
				NGramNormalizer ngn = new NGramNormalizer();
				IOHelper.strongLog("normalizing ngrams from " + input
						+ " and storing updated ngrams at " + output);
				double time = ngn.normalize(input, output);
				IOHelper.strongLog("time for normalizing ngrams from " + input
						+ " : " + time);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (Config.get().normalizeEdges) {
			try {
				String input = trainingPath
						+ Config.get().typologyEdgesPathNotAggregated + "/";
				String output = trainingPath
						+ Config.get().typologyEdgesPathNotAggregated
						+ "Normalized/";
				EdgeNormalizer ngn = new EdgeNormalizer();
				IOHelper.strongLog("normalizing edges from " + input
						+ " and storing updated edges at " + output);
				double time = ngn.normalize(input, output);
				IOHelper.strongLog("time for normalizing edges from " + input
						+ " : " + time);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// TODO: need to generalize this function if it should fit in more general
	// tests! need to take away the pointing to trainingFile and
	// put it as a parameter sourceFile. also the output file should be saved to
	// a different spot

	private static String[] countMostFrequentStartingLetters(int k,
			String trainingFile, String trainingPath) {
		IOHelper.log("Counting the " + k + " most frequent letters in: "
				+ trainingFile);
		String[] result = new String[k];
		try {
			IOHelper.log("Check if most frequent letters have been counted");
			File file = new File(trainingPath
					+ Config.get().nGramsNotAggregatedPath
					+ "mostFrequentLetters.txt");
			if (file.exists()) {
				IOHelper.log("Yes! load from file");
				FileInputStream f = new FileInputStream(file);
				ObjectInputStream s;
				s = new ObjectInputStream(f);
				result = (String[]) s.readObject();
				s.close();
				return result;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IOHelper.log("No! just start counting now");

		BufferedReader br = IOHelper.openReadFile(trainingFile);// Config.get().nGramKeyFile);
		String line = "";
		int cnt = 0;

		HashMap<String, Integer> letters = new HashMap<String, Integer>();

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split(" ");
				for (String token : tokens) {
					if (token == null || token.length() < 1) {
						continue;
					}
					String key = token.substring(0, 1);
					Integer c = letters.get(key);
					if (c == null) {
						letters.put(key, 1);
					} else {
						letters.put(key, c + 1);
					}
				}
				if (cnt % 50000 == 0) {
					System.out.println(cnt);
				}
			}

			int done = 0;
			while (letters.size() > 0 && done < k) {
				int max = 0;
				String maxLetter = null;
				for (String key : letters.keySet()) {
					int tmp = letters.get(key);
					if (tmp > max) {
						maxLetter = key;
						max = tmp;
					}
				}
				letters.remove(maxLetter);
				if (maxLetter.equals("-")) {
					continue;
				}
				result[done] = maxLetter;
				done++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		File file = new File(trainingPath
				+ Config.get().nGramsNotAggregatedPath
				+ "mostFrequentLetters.txt");
		try {
			IOHelper.log("Counting done! save results to file");

			FileOutputStream f = new FileOutputStream(file);
			ObjectOutputStream s = new ObjectOutputStream(f);

			s.writeObject(result);
			s.flush();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
