package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class nGramBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		new File(Config.get().typologyEdgesPathNotAggregated).mkdirs();

		createNGramChunks();
		aggregateNGrams();
		for (int i = 1; i < 5; i++) {
			new File(Config.get().typologyEdgesPathNotAggregated + i
					+ "/aggregated/").mkdirs();

			createTypologyEgeds(i);
			aggregateTypologyEdges(i);
		}
	}

	private static void aggregateNGrams() {
		HashMap<String, BufferedReader> readers = new HashMap<String, BufferedReader>();
		BufferedReader keyReader = IOHelper
				.openReadFile(Config.get().nGramKeyFile);// "/var/lib/datasets/out/wikipedia/letteroutput/keys.txt"
		try {
			String key = "";
			while ((key = keyReader.readLine()) != null) {
				System.out.println("processing key: " + key);
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
				BufferedWriter bw = IOHelper
						.openWriteFile(Config.get().nGramsAggregatedPath// "/var/lib/datasets/out/wikipedia/letteroutput/aggregated/"
								+ key);
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
			}
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
		String[] result = new String[k];

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
