package de.typology.predictorsOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public abstract class NewMySQLSearcher {

	protected String user;
	private HashMap<String, Float> totalResultMap;
	protected int subResultSize;
	protected String smoothingType;
	protected String[] wordIndex;
	protected BufferedWriter resultLogWriter;

	public NewMySQLSearcher() {
		this.subResultSize = Config.get().subResultSize;
		this.user = Config.get().dbUser;
		IOHelper.strongLog("userName: " + this.user);
	}

	public void run(int n, int k, int numberOfQueries, String[] wordIndex) {
		this.wordIndex = wordIndex;
		// TODO: change this fileName
		String fileName = EvalHelper.generateFileName(this.smoothingType, n, k,
				this.subResultSize, numberOfQueries);
		IOHelper.strongLog("log-file name: " + fileName);
		String testFile = "";
		testFile = Config.get().outputDirectory + Config.get().testedOnDataSet
				+ "/" + Config.get().testedOnLang + "/testing-splitted.txt";
		IOHelper.strongLog("testFile: " + testFile);
		File resultLogDir = new File(Config.get().outputDirectory
				+ Config.get().resultLogDirName);
		resultLogDir.mkdir();
		this.resultLogWriter = IOHelper.openWriteFile(
				resultLogDir.getAbsolutePath() + "/" + fileName,
				Config.get().memoryLimitForWritingFiles);

		BufferedReader testFileReader = IOHelper.openReadFile(testFile);
		String line = "";
		long cnt = 0;
		long start = System.currentTimeMillis();

		try {
			// for every line in our testing file
			while ((line = testFileReader.readLine()) != null) {
				String[] words = line.split("\\ ");

				// check if line is suitable to make an experiment
				if (words.length < n) {
					continue;
				}

				// remove the first words.length - n words
				int offset = words.length - n;
				String[] wordsWithoutLast = new String[n - 1];
				int wordsWithoutLastPointer = 0;
				for (int i = offset; i < words.length - 1; i++) {
					wordsWithoutLast[wordsWithoutLastPointer] = words[i];
					wordsWithoutLastPointer++;
				}

				String match = words[words.length - 1];

				// start new experiment with prefixes of various length:
				for (String word : wordsWithoutLast) {
					this.resultLogWriter.write(word + " ");
				}
				this.resultLogWriter.write("\t\tMATCH: " + match + "\n");
				for (int pfl = 0; pfl < match.length(); pfl++) {
					this.totalResultMap = new HashMap<String, Float>();
					int lastRank = Integer.MAX_VALUE;
					for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
							n); sequenceDecimal++) {
						if (sequenceDecimal % 2 == 0) {
							// no target in sequence (e.g. 110)
							continue;
						}
						// k+1 for target
						if (Integer.bitCount(sequenceDecimal) - 1 == k) {
							// calculate partial result
							HashMap<String, Float> subResultMap = this
									.calculateResultSet(wordsWithoutLast,
											match, n, sequenceDecimal, pfl,
											wordIndex);
							// add partial result to totalResultMap
							for (Entry<String, Float> entry : subResultMap
									.entrySet()) {
								if (this.totalResultMap.containsKey(entry
										.getKey())) {
									this.totalResultMap.put(
											entry.getKey(),
											this.totalResultMap.get(entry
													.getKey())
													+ entry.getValue());
								} else {
									this.totalResultMap.put(entry.getKey(),
											entry.getValue());
								}
							}
						}
					}
					// collected results from all edges now find the topk,
					// log
					// result and decide if to continue;
					// System.out.println("results for "
					// + Arrays.toString(wordsWithoutLast) + " with n="
					// + n + " and k=" + k + ":");
					for (Entry<String, Float> e : this.totalResultMap
							.entrySet()) {
						// System.out.println(e.getKey() + " --> " +
						// e.getValue());

					}
					lastRank = this.computeAndLogTop(pfl, match, lastRank);
					if (1 == lastRank) {
						break;
					}

				}
				cnt++;
				if (cnt % 999 == 0) {
					long end = System.currentTimeMillis();
					System.out.println("queries: " + cnt + " \t time: "
							+ (end - start) + " \t qps: " + cnt * 1000
							/ (end - start));
				}
			}
			this.resultLogWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * after all queries have been computed and results are stored in hits
	 * hashmap this function computes the top k elements
	 * 
	 * besides it also logs the results and computes KSS AT K and normalized KSS
	 * AT K
	 * 
	 * @param pfl
	 * @param match
	 * @param lastRank
	 * @return
	 */
	private int computeAndLogTop(int pfl, String match, int lastRank) {
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(
				this.totalResultMap, this.subResultSize);
		int topkCnt = 0;

		try {
			// TODO: idea plot KSS distribution as eval
			for (Float score : topkSuggestions.descendingKeySet()) {
				for (String suggestion : topkSuggestions.get(score)) {
					topkCnt++;
					if (suggestion.equals(match)) {
						this.resultLogWriter.write("HIT\tRANK: " + topkCnt
								+ " \tPREFIXLENGTH: " + pfl + "\n");
						if (topkCnt < lastRank) {
							for (int k = Math.min(5, lastRank); k >= topkCnt; k--) {
								float kss = (float) (match.length() - pfl)
										/ (float) match.length();
								this.resultLogWriter.write("NKSS AT " + k
										+ ": " + kss + "\n");
								this.resultLogWriter.write("KSS AT " + k + ": "
										+ (match.length() - pfl) + "\n");
							}
							lastRank = topkCnt;
						}
						return topkCnt;
					}
				}
			}
			this.resultLogWriter.write("NOTHING\tPREFIXLENGTH: " + pfl + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Integer.MAX_VALUE;
	}

	protected void addToResultMap(ResultSet resultSet,
			HashMap<String, Float> resultMap, float discountValue) {
		try {
			while (resultSet.next()) {
				String target = resultSet.getString("target");
				Float count = discountValue * resultSet.getFloat("score");
				if (!resultMap.containsKey(target)) {
					resultMap.put(target, count);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected abstract HashMap<String, Float> calculateResultSet(
			String[] wordsWithoutLast, String match, int n,
			int sequenceDecimal, int pfl, String[] index);
}