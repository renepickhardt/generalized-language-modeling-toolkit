package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

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

public class nGramBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;

		String[] letters = countMostFrequentStartingLetters(62);
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek + " seconds to: count most frequent letters");

		for (int n = Config.get().nGramLength; n >= 2; n--) {
			new File(Config.get().nGramsNotAggregatedPath + "/" + n).mkdirs();

			NGramChunkCreator ngcc = new NGramChunkCreator(letters);
			if (Config.get().createNGramChunks) {

				ngcc.createNGramChunks(Config.get().germanWikiText, n);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating first chunks");
			}
			if (Config.get().createSecondLevelNGramChunks) {
				ngcc.createSecondLevelChunks(
						Config.get().nGramsNotAggregatedPath + "/" + n, "." + n
								+ "gc");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating detailed ngram chunks");
			}
			if (Config.get().aggregateNGramChunks) {
				Aggregator a = new Aggregator();
				a.aggregateNGrams(Config.get().nGramsNotAggregatedPath + "/"
						+ n, "." + n + "gc", 3);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish aggregating ngrams");
			}

			if (Config.get().sortNGrams) {
				Sorter s = new Sorter();
				s.sort(Config.get().nGramsNotAggregatedPath + "/" + n, "." + n
						+ "ga");
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish sorting aggregated ngrams");
			}
		}

		TypologyChunkCreator tcc = new TypologyChunkCreator(letters);

		// for (int i = 1; i < 5; i++) {
		// new File(Config.get().typologyEdgesPathNotAggregated + i
		// + "/aggregated/").mkdirs();
		// createTypologyEgeds(i);
		//
		// endTime = System.currentTimeMillis();
		// sek = (endTime - startTime) / 1000;
		// IOHelper.strongLog(sek
		// +
		// " seconds to: finnish creating first chunks of typo edges of distance "
		// + i);

		// TODO: correct createSecendLevelTypologyEdges(int) such that to
		// big chunk files are being seperated again. similar to ngrams...
		// and then aggregate everyting. until here everything should work
		// fine
		// aggregateTypologyEdges(i);
		// }
	}

	// TODO: need to generalize this function if it should fit in more general
	// tests!
	private static String[] countMostFrequentStartingLetters(int k) {
		IOHelper.log("Counting the " + k + " most frequent letters in: "
				+ Config.get().germanWikiText);
		String[] result = new String[k];
		try {
			IOHelper.log("Check if most frequent letters have been counted");
			File file = new File(Config.get().nGramsNotAggregatedPath
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

		BufferedReader br = IOHelper.openReadFile(Config.get().germanWikiText);// Config.get().nGramKeyFile);
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

		File file = new File(Config.get().nGramsNotAggregatedPath
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
	/*
	 * private static void createSecendLevelNGramChunks() { BufferedReader keys
	 * = IOHelper.openReadFile(Config.get().nGramKeyFile); String line = "";
	 * 
	 * HashMap<String, Integer> usedKeys = new HashMap<String, Integer>();
	 * 
	 * try { while ((line = keys.readLine()) != null) { File f = new
	 * File(Config.get().nGramsNotAggregatedPath + line); if (!f.exists()) {
	 * continue; } if (f.length() > 512 * 1024 * 1024) {
	 * IOHelper.log("need to further split file: " + line); usedKeys =
	 * createDetailedNGramChunks( Config.get().nGramsNotAggregatedPath + line,
	 * usedKeys); // TODO: aus keyfile loeschen f.delete(); } else {
	 * usedKeys.put(line, 1); IOHelper.log("file '" + line +
	 * "' can be aggregated"); // TODO:aggregieren sollte so bleiben wie vorher
	 * nur erst // wenn alle kleineren chunks existieren. } } keys.close();
	 * BufferedWriter keyFile = IOHelper
	 * .openWriteFile(Config.get().nGramKeyFile); for (String k :
	 * usedKeys.keySet()) { keyFile.write(k + "\n"); } keyFile.close(); } catch
	 * (IOException e) { // TODO Auto-generated catch block e.printStackTrace();
	 * } }
	 * 
	 * private static void aggregateNGrams() { HashMap<String, BufferedReader>
	 * readers = new HashMap<String, BufferedReader>(); BufferedReader keyReader
	 * = IOHelper .openReadFile(Config.get().nGramKeyFile);//
	 * "/var/lib/datasets/out/wikipedia/letteroutput/keys.txt" try { String key
	 * = ""; while ((key = keyReader.readLine()) != null) {
	 * IOHelper.log("processing key: " + key);
	 * 
	 * File f = new File(Config.get().nGramsNotAggregatedPath + key); if
	 * (!f.exists()) { IOHelper.log(key +
	 * " not found. probably becuase file was to big and got split into further chunks"
	 * ); continue; }
	 * 
	 * BufferedReader br = IOHelper
	 * .openReadFile(Config.get().nGramsNotAggregatedPath//
	 * "/var/lib/datasets/out/wikipedia/letteroutput/" + key); HashMap<String,
	 * Integer> nGrams = new HashMap<String, Integer>(); String line = ""; int
	 * lCnt = 0; while ((line = br.readLine()) != null) { Integer cnt =
	 * nGrams.get(line); lCnt++; if (cnt != null) { nGrams.put(line, cnt + 1); }
	 * else { nGrams.put(line, 1); } if (lCnt % 500000 == 0) {
	 * System.out.println(lCnt + " nGrams processed for aggregating"); } }
	 * System.out.println("aggregation done for key: " + key +
	 * "\nstart writing to file"); BufferedWriter bw = IOHelper.openWriteFile(
	 * Config.get().nGramsAggregatedPath//
	 * "/var/lib/datasets/out/wikipedia/letteroutput/aggregated/" + key, 32 *
	 * 1024 * 1024); int nCnt = 0; for (String nGram : nGrams.keySet()) {
	 * bw.write(nGram + "\t#" + nGrams.get(nGram) + "\n"); nCnt++; if (nCnt %
	 * 1000000 == 0) { bw.flush(); System.out.println(nCnt +
	 * " written to file"); } } bw.flush(); bw.close(); br.close(); // DELETE
	 * THE UNAGGREGATED FILE TO SAVE DISKSPACE f.delete();
	 * 
	 * } } catch (IOException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } }
	 * 
	 * private static void createNGramChunks(String fromFile) { String[]
	 * mostFrequentLetters = countMostFrequentStartingLetters(62);
	 * 
	 * BufferedReader br = IOHelper.openReadFile(fromFile, 8 * 1024 * 1024);//
	 * "/var/lib/datasets/out/wikipedia/testfile.txt"); String line = ""; int
	 * cnt = 0;
	 * 
	 * HashMap<String, BufferedWriter> writers = createWriter(
	 * Config.get().nGramsNotAggregatedPath, mostFrequentLetters);//
	 * createWriterOld();
	 * 
	 * try { while ((line = br.readLine()) != null) { cnt++; String[] tokens =
	 * line.split(" "); for (int i = Config.get().nGramLength; i <
	 * tokens.length; i++) { boolean first = true; BufferedWriter bw = null; for
	 * (int j = i - Config.get().nGramLength; j < i; j++) { if (first) { String
	 * token = tokens[i - Config.get().nGramLength]; String key = null; key =
	 * token.substring(0, 1); bw = writers.get(key); if (bw == null) { key =
	 * "other"; bw = writers.get(key); } first = false; } bw.write(tokens[j]);
	 * if (j < i - 1) { bw.write("\t"); } } bw.write("\n"); } if (cnt % 50000 ==
	 * 0) { IOHelper.log("processed " + cnt + " articles into chunks:"); } }
	 * BufferedWriter kw = IOHelper .openWriteFile(Config.get().nGramKeyFile);
	 * for (String k : writers.keySet()) { kw.write(k + "\n"); BufferedWriter
	 * tmp = writers.get(k); tmp.flush(); tmp.close(); } kw.flush(); kw.close();
	 * } catch (IOException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); }
	 * 
	 * }
	 * 
	 * private static HashMap<String, Integer> createDetailedNGramChunks( String
	 * fromFile, HashMap<String, Integer> usedKeys) { String[]
	 * mostFrequentLetters = countMostFrequentStartingLetters(62);
	 * 
	 * BufferedReader br = IOHelper.openReadFile(fromFile);//
	 * "/var/lib/datasets/out/wikipedia/testfile.txt"); String line = ""; int
	 * cnt = 0;
	 * 
	 * HashMap<String, BufferedWriter> writers = createWriter(fromFile,
	 * mostFrequentLetters);
	 * 
	 * try { while ((line = br.readLine()) != null) { cnt++; String[] tokens =
	 * line.split("\\s"); if (tokens.length != Config.get().nGramLength) {
	 * continue; } String key = tokens[1].substring(0, 1); BufferedWriter bw =
	 * null; bw = writers.get(key); if (bw == null) { key = "other"; bw =
	 * writers.get(key); } bw.write(line + "\n"); if (cnt % 1000000 == 0) { for
	 * (String k : writers.keySet()) { writers.get(k).flush(); }
	 * System.out.println("processed ngrams into smaller chunks:" + cnt); } } //
	 * BufferedWriter kw = IOHelper //
	 * .openAppendFile(Config.get().nGramKeyFile); for (String k :
	 * writers.keySet()) { String[] tmp = fromFile.split("/"); String prefix =
	 * tmp[tmp.length - 1]; usedKeys.put(prefix + k, 1); // kw.write(prefix + k
	 * + "\n"); writers.get(k).flush(); writers.get(k).close(); } // kw.flush();
	 * // kw.close(); } catch (IOException e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } return usedKeys; }
	 * 
	 * private static HashMap<String, BufferedWriter> createWriter(String
	 * oldPath, String[] letters) {
	 * IOHelper.log("create chunks for most common letters: " +
	 * letters.toString()); HashMap<String, BufferedWriter> writers = new
	 * HashMap<String, BufferedWriter>(); for (String letter : letters) {
	 * BufferedWriter bw = IOHelper.openWriteFile(oldPath + letter, 8 * 1024 *
	 * 1024); writers.put(letter, bw); } BufferedWriter bw =
	 * IOHelper.openWriteFile(oldPath + "other", 8 * 1024 * 1024);
	 * writers.put("other", bw);
	 * 
	 * IOHelper.log("all chunks are created"); return writers; }
	 * 
	 * private static HashMap<String, BufferedWriter> createWriterOld() {
	 * System.out.println("get most common letters"); String[] letters =
	 * countMostFrequentStartingLetters(62);
	 * System.out.println("open files now"); HashMap<String, BufferedWriter>
	 * writers = new HashMap<String, BufferedWriter>(); for (String letter :
	 * letters) { for (String letter2 : letters) { String tmp = letter +
	 * letter2; BufferedWriter bw = IOHelper.openWriteFile(
	 * Config.get().nGramsNotAggregatedPath + tmp, 128 * 1024); writers.put(tmp,
	 * bw); } } BufferedWriter bw = IOHelper
	 * .openWriteFile(Config.get().nGramsNotAggregatedPath + "other");
	 * writers.put("other", bw);
	 * 
	 * System.out.println("files open lets go"); return writers; }
	 * 
	 * private static void createTypologyEgeds(int distance) { BufferedReader br
	 * = IOHelper.openReadFile(Config.get().nGramKeyFile); String line = "";
	 * 
	 * HashMap<String, BufferedWriter> writers = new HashMap<String,
	 * BufferedWriter>(); try { // create files in which typo edges will be
	 * written (not aggregated // yet) long startTime =
	 * System.currentTimeMillis(); String[] startLetters =
	 * countMostFrequentStartingLetters(62);
	 * IOHelper.log("create writers for typology edges"); for (String l :
	 * startLetters) { BufferedWriter bw = IOHelper.openWriteFile(
	 * Config.get().typologyEdgesPathNotAggregated + distance + "/" + l, 8 *
	 * 1024 * 1024); writers.put(l, bw); } BufferedWriter bwo =
	 * IOHelper.openWriteFile( Config.get().typologyEdgesPathNotAggregated +
	 * distance + "/other", 8 * 1024 * 1024); writers.put("other", bwo);
	 * 
	 * IOHelper.log("writers created read every single aggreagted ngram file now"
	 * ); // for all aggregated NGramFiles put edges to typology files; int cnt
	 * = 0; int fileCnt = 0; while ((line = br.readLine()) != null) {
	 * BufferedReader nGramsReader = IOHelper.openReadFile(Config
	 * .get().nGramsAggregatedPath + line); String nGram = "";
	 * IOHelper.log(fileCnt++ + "\tprocessing: " +
	 * Config.get().nGramsAggregatedPath + line); while ((nGram =
	 * nGramsReader.readLine()) != null) { String[] values = nGram.split("\\s");
	 * if (values.length != 6) { System.out.println("false nGramlenght");
	 * continue; }
	 * 
	 * for (int i = 0; i < 5 - distance; i++) { String from = values[i]; String
	 * to = values[i + distance]; String key = from.substring(0, 1);// +
	 * to.substring(0, // 1); BufferedWriter bw = writers.get(key); if (bw ==
	 * null) { key = "other"; bw = writers.get(key); }
	 * 
	 * bw.write(from + "\t" + to + "\t" + values[5] + "\n"); cnt++; if (cnt %
	 * 50000000 == 0) { long endTime = System.currentTimeMillis(); long sek =
	 * (endTime - startTime) / 1000; IOHelper.log(cnt +
	 * " flush writers time passed: " + sek + "\t ngrams per sec: " + cnt / (sek
	 * + 1));
	 * 
	 * for (String k : writers.keySet()) { writers.get(k).flush(); } }
	 * 
	 * } } nGramsReader.close(); } br.close(); for (String k : writers.keySet())
	 * { writers.get(k).flush(); writers.get(k).close(); } // TODO: need to
	 * aggregate all of the eges
	 * 
	 * } catch (IOException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } }
	 * 
	 * private static void createSecendLevelTypologyEdges(int distance) {
	 * String[] letters = countMostFrequentStartingLetters(62); String line =
	 * "";
	 * 
	 * try { for (String letter : letters) { line = letter; File f = new
	 * File(Config.get().typologyEdgesPathNotAggregated + "/" + distance + "/" +
	 * line); if (!f.exists()) { continue; } if (f.length() > 512 * 1024 * 1024)
	 * { IOHelper.log("need to further split file: " + line); //
	 * createDetailedNGramChunks( // Config.get().nGramsNotAggregatedPath +
	 * line, // usedKeys); // TODO: aus keyfile loeschen f.delete(); } else { //
	 * usedKeys.put(line, 1); // IOHelper.log("file '" + line +
	 * "' can be aggregated"); // TODO:aggregieren sollte so bleiben wie vorher
	 * nur erst // wenn alle kleineren chunks existieren. } } } catch (Exception
	 * e) { // TODO Auto-generated catch block e.printStackTrace(); } }
	 * 
	 * private static void aggregateTypologyEdges(int distance) { // String path
	 * = "/var/lib/datasets/out/wikipedia/letteroutput/"; // String keyFile =
	 * path + "keys.txt"; // String aggregatedPath = path + "aggregated/"; //
	 * String typoPath = aggregatedPath + "typoedges/";
	 * 
	 * BufferedReader br = IOHelper.openReadFile(Config.get().nGramKeyFile);
	 * String line = ""; int gCnt = 0;
	 * 
	 * try { // create files in which typo edges will be written (not aggregated
	 * // yet) long startTime = System.currentTimeMillis();
	 * System.out.println("create writers"); while ((line = br.readLine()) !=
	 * null) { BufferedWriter bw = IOHelper
	 * .openWriteFile(Config.get().typologyEdgesPathNotAggregated + distance +
	 * "/aggregated/" + line);
	 * 
	 * // System.out.println("aggregate: " + line);
	 * 
	 * BufferedReader edgesReader = IOHelper
	 * .openReadFile(Config.get().typologyEdgesPathNotAggregated + distance +
	 * "/" + line);
	 * 
	 * String edge = ""; HashMap<String, Integer> edges = new HashMap<String,
	 * Integer>(); while ((edge = edgesReader.readLine()) != null) { String[]
	 * values = edge.split("\\s"); if (values.length != 3) { //
	 * System.out.println("format error in edge files"); continue; } //
	 * TODO:take away as soon as no # in count number! String tmp =
	 * values[2].replace("#", ""); if (tmp.length() < 1) { continue; } Integer
	 * cnt = Integer.parseInt(tmp); // newChar)substring(1,
	 * values[2].length())); String key = values[0] + "\t" + values[1]; Integer
	 * oldCnt = edges.get(key); if (oldCnt == null) { edges.put(key, cnt); }
	 * else { edges.put(key, cnt + oldCnt); } gCnt++; if (gCnt % 3000000 == 0) {
	 * long endTime = System.currentTimeMillis(); long sek = (endTime -
	 * startTime) / 1000; System.out.println(gCnt / 1000000 +
	 * " mio. edges aggregated \t\ttime:" + sek + "\t edges per sek: " + gCnt /
	 * (sek + 1)); } } System.out.println("write to file: " + line); for (String
	 * key : edges.keySet()) { bw.write(key + "\t#" + edges.get(key) + "\n"); }
	 * bw.flush(); bw.close(); } } catch (Exception e) { e.printStackTrace(); }
	 * }
	 */
	// private static void createNGramChunks() {
	// BufferedReader br = IOHelper.openReadFile(Config.get().germanWikiText);//
	// "/var/lib/datasets/out/wikipedia/testfile.txt");
	// String line = "";
	// int cnt = 0;
	//
	// HashMap<String, BufferedWriter> writers = createWriterOld();
	//
	// try {
	// while ((line = br.readLine()) != null) {
	// cnt++;
	// String[] tokens = line.split(" ");
	// for (int i = Config.get().nGramLength; i < tokens.length; i++) {
	// boolean first = true;
	// BufferedWriter bw = null;
	// for (int j = i - Config.get().nGramLength; j < i; j++) {
	// if (first) {
	// String token = tokens[i - Config.get().nGramLength];
	// String key = null;
	// key = token.substring(0, 1)
	// + tokens[i - Config.get().nGramLength + 1]
	// .substring(0, 1);
	// bw = writers.get(key);
	// if (bw == null) {
	// key = "other";
	// bw = writers.get(key);
	// }
	// first = false;
	// }
	// bw.write(tokens[j]);
	// if (j < i - 1) {
	// bw.write("\t");
	// }
	// }
	// bw.write("\n");
	// }
	// if (cnt % 20000 == 0) {
	// for (String k : writers.keySet()) {
	// writers.get(k).flush();
	// }
	// System.out.println("processed articles:" + cnt);
	// }
	// }
	// BufferedWriter kw = IOHelper
	// .openWriteFile("/var/lib/datasets/out/wikipedia/letteroutput/keys.txt");
	// for (String k : writers.keySet()) {
	// kw.write(k + "\n");
	// writers.get(k).flush();
	// writers.get(k).close();
	// }
	// kw.flush();
	// kw.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

}
