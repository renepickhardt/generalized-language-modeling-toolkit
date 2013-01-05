package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;


public class NGramFromGoogleBuilder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		NGramFromGoogleBuilder.run("/home/martin/out/google/ger/","/home/martin/out/google/ger/normalized/1/1gram-normalized.txt");
	}
	public static void run(String trainingPath, String trainingFile) throws IOException {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;

		String[] letters = countMostFrequentStartingLetters(62, trainingFile,
				trainingPath);

		//cut down ngrams
		NGramNormalizer ngn=new NGramNormalizer();
		new File(trainingPath+"/cut/").mkdirs();
		ngn.removeNGrams(trainingPath+"/normalized/", trainingPath+"/cut/");

		for (int n = Config.get().nGramLength; n >= 2; n--) {
			new File(trainingPath + Config.get().nGramsNotAggregatedPath + "/"
					+ n).mkdirs();

			NGramAndCountChunkCreator ngaccc = new NGramAndCountChunkCreator(letters);
			if (Config.get().createNGramChunks) {

				ngaccc.createNGramChunks(trainingPath+"cut/"+n+"/"+n+"gram-normalized.txt", n, trainingPath);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating first chunks");
			}
			if (Config.get().createSecondLevelNGramChunks) {
				ngaccc.createSecondLevelChunks(trainingPath
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

			//the ngram counts are to damn high!

			//			if (Config.get().generateNGramDistribution) {
			//				NGramDistribution ngd = new NGramDistribution();
			//				ngd.countDistribution(trainingPath
			//						+ Config.get().nGramsNotAggregatedPath + "/" + n, "."
			//								+ n + "gs");
			//				endTime = System.currentTimeMillis();
			//				sek = (endTime - startTime) / 1000;
			//				IOHelper.strongLog(sek
			//						+ " seconds to: finnish creating distribution for ngrams");
			//			}

			new File(trainingPath + Config.get().typologyEdgesPathNotAggregated
					+ "/" + (n - 1)).mkdirs();

			TypologyAndCountChunkCreator taccc = new TypologyAndCountChunkCreator(letters);
			if (Config.get().createTypologyEdgeChunks) {
				taccc.createTypoelogyEdgeChunks(trainingPath+"cut/"+n+"/"+n+"gram-normalized.txt", trainingPath
						+ Config.get().typologyEdgesPathNotAggregated, n - 1);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating typology edge chunks of type: "
						+ (n - 1));
			}

			if (Config.get().createSecondLevelTypologyEdgeChunks) {
				taccc.createSecondLevelChunks(trainingPath
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

			if (Config.get().normalizeEdges) {
				try {
					String input = trainingPath
							+ Config.get().typologyEdgesPathNotAggregated+"/";
					String output = trainingPath
							+ Config.get().typologyEdgesPathNotAggregated
							+ "Normalized/";
					EdgeNormalizer en = new EdgeNormalizer();
					IOHelper.strongLog("normalizing edges from " + input
							+ " and storing updated edges at " + output);
					double time = en.normalize(input, output);
					IOHelper.strongLog("time for normalizing edges from "
							+ input + " : " + time);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//the edge counts are to damn high!

			//			if (Config.get().generateTypologyEdgeDistribution) {
			//				NGramDistribution ngd = new NGramDistribution();
			//				ngd.countDistribution(trainingPath
			//						+ Config.get().typologyEdgesPathNotAggregated + "/"
			//						+ (n - 1), "." + (n - 1) + "es");
			//				endTime = System.currentTimeMillis();
			//				sek = (endTime - startTime) / 1000;
			//				IOHelper.strongLog(sek
			//						+ " seconds to: finnish creating distribution for ngrams");
			//			}

		}

		if (Config.get().normalizeNGrams) {
			try {

				String input = trainingPath
						+ Config.get().nGramsNotAggregatedPath + "/";
				String output = trainingPath
						+ Config.get().nGramsNotAggregatedPath + "Normalized/";
				new File(output).mkdirs();
				ngn = new NGramNormalizer();
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
				String[] temp=line.split("\t");
				String[] tokens = Arrays.copyOf(temp, temp.length-1);
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
