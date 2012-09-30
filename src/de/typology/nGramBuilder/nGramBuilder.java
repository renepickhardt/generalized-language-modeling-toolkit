package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class nGramBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;
		new File(Config.get().typologyEdgesPathNotAggregated).mkdirs();

		String[] letters = countMostFrequentStartingLetters(62);
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek + " seconds to: count most frequent letters");

		createNGramChunks(Config.get().germanWikiText);
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek + " seconds to: finnish creating first chunks");

		createSecendLevelNGramChunks();
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek
				+ " seconds to: finnish creating detailed ngram chunks");

		aggregateNGrams();
		endTime = System.currentTimeMillis();
		sek = (endTime - startTime) / 1000;
		IOHelper.strongLog(sek + " seconds to: finnish aggregating ngrams");

		// for (int i = 1; i < 5; i++) {
		// new File(Config.get().typologyEdgesPathNotAggregated + i
		// + "/aggregated/").mkdirs();
		//
		// createTypologyEgeds(i);
		// aggregateTypologyEdges(i);
		// }
	}

	private static void createSecendLevelNGramChunks() {
		BufferedReader keys = IOHelper.openReadFile(Config.get().nGramKeyFile);
		String line = "";

		HashMap<String, Integer> usedKeys = new HashMap<String, Integer>();

		try {
			while ((line = keys.readLine()) != null) {
				File f = new File(Config.get().nGramsNotAggregatedPath + line);
				if (!f.exists()) {
					continue;
				}
				if (f.length() > 512 * 1024 * 1024) {
					IOHelper.log("need to further split file: " + line);
					createDetailedNGramChunks(
							Config.get().nGramsNotAggregatedPath + line,
							usedKeys);
					// TODO: aus keyfile loeschen
					f.delete();
				} else {
					usedKeys.put(line, 1);
					IOHelper.log("file '" + line + "' can be aggregated");
					// TODO:aggregieren sollte so bleiben wie vorher nur erst
					// wenn alle kleineren chunks existieren.
				}
			}
			keys.close();
			BufferedWriter keyFile = IOHelper
					.openWriteFile(Config.get().nGramKeyFile);
			for (String k : usedKeys.keySet()) {
				keyFile.write(k + "\n");
			}
			keyFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void aggregateNGrams() {
		HashMap<String, BufferedReader> readers = new HashMap<String, BufferedReader>();
		BufferedReader keyReader = IOHelper
				.openReadFile(Config.get().nGramKeyFile);// "/var/lib/datasets/out/wikipedia/letteroutput/keys.txt"
		try {
			String key = "";
			while ((key = keyReader.readLine()) != null) {
				IOHelper.log("processing key: " + key);

				File f = new File(Config.get().nGramsNotAggregatedPath + key);
				if (!f.exists()) {
					IOHelper.log(key
							+ " not found. probably becuase file was to big and got split into further chunks");
					continue;
				}

				BufferedReader br = IOHelper
						.openReadFile(Config.get().nGramsNotAggregatedPath// "/var/lib/datasets/out/wikipedia/letteroutput/"
								+ key);
				HashMap<String, Integer> nGrams = new HashMap<String, Integer>();
				String line = "";
				int lCnt = 0;
				while ((line = br.readLine()) != null) {
					Integer cnt = nGrams.get(line);
					lCnt++;
					if (cnt != null) {
						nGrams.put(line, cnt + 1);
					} else {
						nGrams.put(line, 1);
					}
					if (lCnt % 500000 == 0) {
						System.out.println(lCnt
								+ " nGrams processed for aggregating");
					}
				}
				System.out.println("aggregation done for key: " + key
						+ "\nstart writing to file");
				BufferedWriter bw = IOHelper.openWriteFile(
						Config.get().nGramsAggregatedPath// "/var/lib/datasets/out/wikipedia/letteroutput/aggregated/"
								+ key, 32 * 1024 * 1024);
				int nCnt = 0;
				for (String nGram : nGrams.keySet()) {
					bw.write(nGram + "\t#" + nGrams.get(nGram) + "\n");
					nCnt++;
					if (nCnt % 1000000 == 0) {
						bw.flush();
						System.out.println(nCnt + " written to file");
					}
				}
				bw.flush();
				bw.close();
				br.close();
				// DELETE THE UNAGGREGATED FILE TO SAVE DISKSPACE
				f.delete();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void createNGramChunks(String fromFile) {
		String[] mostFrequentLetters = countMostFrequentStartingLetters(62);

		BufferedReader br = IOHelper.openReadFile(fromFile, 8 * 1024 * 1024);// "/var/lib/datasets/out/wikipedia/testfile.txt");
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = createWriter(
				Config.get().nGramsNotAggregatedPath, mostFrequentLetters);// createWriterOld();

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split(" ");
				for (int i = Config.get().nGramLength; i < tokens.length; i++) {
					boolean first = true;
					BufferedWriter bw = null;
					for (int j = i - Config.get().nGramLength; j < i; j++) {
						if (first) {
							String token = tokens[i - Config.get().nGramLength];
							String key = null;
							key = token.substring(0, 1);
							bw = writers.get(key);
							if (bw == null) {
								key = "other";
								bw = writers.get(key);
							}
							first = false;
						}
						bw.write(tokens[j]);
						if (j < i - 1) {
							bw.write("\t");
						}
					}
					bw.write("\n");
				}
				if (cnt % 50000 == 0) {
					IOHelper.log("processed " + cnt + " articles into chunks:");
				}
			}
			BufferedWriter kw = IOHelper
					.openWriteFile(Config.get().nGramKeyFile);
			for (String k : writers.keySet()) {
				kw.write(k + "\n");
				BufferedWriter tmp = writers.get(k);
				tmp.flush();
				tmp.close();
			}
			kw.flush();
			kw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void createDetailedNGramChunks(String fromFile, HashMap<String, Integer> usedKeys) {
		String[] mostFrequentLetters = countMostFrequentStartingLetters(62);

		BufferedReader br = IOHelper.openReadFile(fromFile);// "/var/lib/datasets/out/wikipedia/testfile.txt");
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = createWriter(fromFile,
				mostFrequentLetters);

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split("\\s");
				if (tokens.length != Config.get().nGramLength) {
					continue;
				}
				String key = tokens[1].substring(0, 1);
				BufferedWriter bw = null;
				bw = writers.get(key);
				if (bw == null) {
					key = "other";
					bw = writers.get(key);
				}
				bw.write(line + "\n");
				if (cnt % 1000000 == 0) {
					for (String k : writers.keySet()) {
						writers.get(k).flush();
					}
					System.out.println("processed ngrams into smaller chunks:"
							+ cnt);
				}
			}
//			BufferedWriter kw = IOHelper
//					.openAppendFile(Config.get().nGramKeyFile);
			for (String k : writers.keySet()) {
				String[] tmp = fromFile.split("/");
				String prefix = tmp[tmp.length - 1];
				usedKeys.put(prefix+k, 1_);
//				kw.write(prefix + k + "\n");
				writers.get(k).flush();
				writers.get(k).close();
			}
//			kw.flush();
//			kw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void createNGramChunks() {
		BufferedReader br = IOHelper.openReadFile(Config.get().germanWikiText);// "/var/lib/datasets/out/wikipedia/testfile.txt");
		String line = "";
		int cnt = 0;

		HashMap<String, BufferedWriter> writers = createWriterOld();

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] tokens = line.split(" ");
				for (int i = Config.get().nGramLength; i < tokens.length; i++) {
					boolean first = true;
					BufferedWriter bw = null;
					for (int j = i - Config.get().nGramLength; j < i; j++) {
						if (first) {
							String token = tokens[i - Config.get().nGramLength];
							String key = null;
							key = token.substring(0, 1)
									+ tokens[i - Config.get().nGramLength + 1]
											.substring(0, 1);
							bw = writers.get(key);
							if (bw == null) {
								key = "other";
								bw = writers.get(key);
							}
							first = false;
						}
						bw.write(tokens[j]);
						if (j < i - 1) {
							bw.write("\t");
						}
					}
					bw.write("\n");
				}
				if (cnt % 20000 == 0) {
					for (String k : writers.keySet()) {
						writers.get(k).flush();
					}
					System.out.println("processed articles:" + cnt);
				}
			}
			BufferedWriter kw = IOHelper
					.openWriteFile("/var/lib/datasets/out/wikipedia/letteroutput/keys.txt");
			for (String k : writers.keySet()) {
				kw.write(k + "\n");
				writers.get(k).flush();
				writers.get(k).close();
			}
			kw.flush();
			kw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static HashMap<String, BufferedWriter> createWriter(String oldPath,
			String[] letters) {
		IOHelper.log("create chunks for most common letters: "
				+ letters.toString());
		HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
		for (String letter : letters) {
			BufferedWriter bw = IOHelper.openWriteFile(oldPath + letter,
					8 * 1024 * 1024);
			writers.put(letter, bw);
		}
		BufferedWriter bw = IOHelper.openWriteFile(oldPath + "other",
				8 * 1024 * 1024);
		writers.put("other", bw);

		IOHelper.log("all chunks are created");
		return writers;
	}

	private static HashMap<String, BufferedWriter> createWriterOld() {
		System.out.println("get most common letters");
		String[] letters = countMostFrequentStartingLetters(62);
		System.out.println("open files now");
		HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
		for (String letter : letters) {
			for (String letter2 : letters) {
				String tmp = letter + letter2;
				BufferedWriter bw = IOHelper.openWriteFile(
						Config.get().nGramsNotAggregatedPath + tmp, 128 * 1024);
				writers.put(tmp, bw);
			}
		}
		BufferedWriter bw = IOHelper
				.openWriteFile(Config.get().nGramsNotAggregatedPath + "other");
		writers.put("other", bw);

		System.out.println("files open lets go");
		return writers;
	}

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

	private static void createTypologyEgeds(int distance) {
		// String path = "/var/lib/datasets/out/wikipedia/letteroutput/";
		// String keyFile = path + "keys.txt";
		// String aggregatedPath = path + "aggregated/";
		// String typoPath = aggregatedPath + "typoedges/";

		BufferedReader br = IOHelper.openReadFile(Config.get().nGramKeyFile);
		String line = "";

		HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
		try {
			// create files in which typo edges will be written (not aggregated
			// yet)
			long startTime = System.currentTimeMillis();
			System.out.println("create writers");
			while ((line = br.readLine()) != null) {
				BufferedWriter bw = IOHelper.openWriteFile(
						Config.get().typologyEdgesPathNotAggregated + distance
								+ "/" + line, 128 * 1024);
				writers.put(line, bw);
			}
			br.close();
			br = IOHelper.openReadFile(Config.get().nGramKeyFile);
			System.out
					.println("writers created read every single aggreagted ngram file now");
			// for all aggregated NGramFiles put edges to typology files;
			int cnt = 0;
			int fileCnt = 0;
			while ((line = br.readLine()) != null) {
				BufferedReader nGramsReader = IOHelper.openReadFile(Config
						.get().nGramsAggregatedPath + line);
				String nGram = "";
				System.out.println(fileCnt++ + "\tprocessing: "
						+ Config.get().nGramsAggregatedPath + line);
				while ((nGram = nGramsReader.readLine()) != null) {
					String[] values = nGram.split("\\s");
					if (values.length != 6) {
						System.out.println("false nGramlenght");
						continue;
					}

					for (int i = 0; i < 5 - distance; i++) {
						String from = values[i];
						String to = values[i + distance];
						String key = from.substring(0, 1) + to.substring(0, 1);
						BufferedWriter bw = writers.get(key);
						if (bw == null) {
							key = "other";
							bw = writers.get(key);
						}

						bw.write(from + "\t" + to + "\t" + values[5] + "\n");
						cnt++;
						if (cnt % 50000000 == 0) {
							long endTime = System.currentTimeMillis();
							long sek = (startTime - endTime) / 1000;
							System.out.println(cnt
									+ "flush writers time passed: " + sek
									+ "\t ngrams per sec: " + cnt / (sek + 1));

							for (String k : writers.keySet()) {
								writers.get(k).flush();
							}
						}

					}
				}
				nGramsReader.close();
			}
			br.close();
			for (String k : writers.keySet()) {
				writers.get(k).flush();
				writers.get(k).close();
			}
			// TODO: need to aggregate all of the eges

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void aggregateTypologyEdges(int distance) {
		// String path = "/var/lib/datasets/out/wikipedia/letteroutput/";
		// String keyFile = path + "keys.txt";
		// String aggregatedPath = path + "aggregated/";
		// String typoPath = aggregatedPath + "typoedges/";

		BufferedReader br = IOHelper.openReadFile(Config.get().nGramKeyFile);
		String line = "";
		int gCnt = 0;

		try {
			// create files in which typo edges will be written (not aggregated
			// yet)
			long startTime = System.currentTimeMillis();
			System.out.println("create writers");
			while ((line = br.readLine()) != null) {
				BufferedWriter bw = IOHelper
						.openWriteFile(Config.get().typologyEdgesPathNotAggregated
								+ distance + "/aggregated/" + line);

				// System.out.println("aggregate: " + line);

				BufferedReader edgesReader = IOHelper
						.openReadFile(Config.get().typologyEdgesPathNotAggregated
								+ distance + "/" + line);

				String edge = "";
				HashMap<String, Integer> edges = new HashMap<String, Integer>();
				while ((edge = edgesReader.readLine()) != null) {
					String[] values = edge.split("\\s");
					if (values.length != 3) {
						// System.out.println("format error in edge files");
						continue;
					}
					// TODO:take away as soon as no # in count number!
					String tmp = values[2].replace("#", "");
					if (tmp.length() < 1) {
						continue;
					}
					Integer cnt = Integer.parseInt(tmp);
					// newChar)substring(1, values[2].length()));
					String key = values[0] + "\t" + values[1];
					Integer oldCnt = edges.get(key);
					if (oldCnt == null) {
						edges.put(key, cnt);
					} else {
						edges.put(key, cnt + oldCnt);
					}
					gCnt++;
					if (gCnt % 3000000 == 0) {
						long endTime = System.currentTimeMillis();
						long sek = (endTime - startTime) / 1000;
						System.out.println(gCnt / 1000000
								+ " mio. edges aggregated \t\ttime:" + sek
								+ "\t edges per sek: " + gCnt / (sek + 1));
					}
				}
				System.out.println("write to file: " + line);
				for (String key : edges.keySet()) {
					bw.write(key + "\t#" + edges.get(key) + "\n");
				}
				bw.flush();
				bw.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
