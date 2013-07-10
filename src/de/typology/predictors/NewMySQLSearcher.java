package de.typology.predictors;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public abstract class NewMySQLSearcher {

	protected Connection lowConnection = null;
	protected Connection lowDiscountConnection = null;
	protected Connection highConnection = null;
	protected Connection highDiscountConnection = null;
	protected String user;
	private HashMap<String, Float> totalResultMap;
	protected int n;
	protected int joinLength;

	public NewMySQLSearcher(String dataBaseName) {
		this.n = Config.get().modelLength;
		this.joinLength = 10;
		this.user = Config.get().dbUser;
		IOHelper.strongLog("dbName: " + dataBaseName);
		IOHelper.strongLog("userName: " + this.user);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dataBasePrefix = "jdbc:mysql://localhost/";
			String dataBaseSuffix = "?" + "user=" + this.user + "&password=";
			this.lowConnection = DriverManager.getConnection(dataBasePrefix
					+ dataBaseName + "_low" + dataBaseSuffix);
			this.lowDiscountConnection = DriverManager
					.getConnection(dataBasePrefix + dataBaseName
							+ "_low_discount" + dataBaseSuffix);
			this.highConnection = DriverManager.getConnection(dataBasePrefix
					+ dataBaseName + "_high" + dataBaseSuffix);
			this.highDiscountConnection = DriverManager
					.getConnection(dataBasePrefix + dataBaseName
							+ "_high_discount" + dataBaseSuffix);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(int n, int k, int numberOfQueries, String weights,
			String[] wordIndex) {
		this.n = n;
		// TODO: change this fileName
		String fileName = EvalHelper.gennerateFileName(this.getClass()
				.getName().replaceAll("de.typology.predictors.", "")
				.replaceAll("NewMySQLSearcher", "").toLowerCase()
				+ "k" + k, weights, this.n, this.joinLength, numberOfQueries);
		IOHelper.strongLog("log-file name: " + fileName);
		String testFile = "";
		IOHelper.strongLog("weights: " + weights);
		testFile = Config.get().outputDirectory + Config.get().testedOnDataSet
				+ "/" + Config.get().testedOnLang + "/testing-splitted.txt";
		IOHelper.strongLog("testFile: " + testFile);
		try {
			EvalHelper.openAndSetResultLogFile(fileName);

			BufferedReader br = IOHelper.openReadFile(testFile);
			String line = "";
			long cnt = 0;
			long start = System.currentTimeMillis();

			// for every line in our testing file
			while ((line = br.readLine()) != null) {
				String[] words = line.split("\\ ");

				// check if line is suitable to make an experiment
				if (words.length < n) {
					continue;
				}

				// remove the first words.length - n words
				String[] tempWords = new String[n];
				int offset = words.length - n;
				int tempWordsPointer = 0;
				for (int i = offset; i < words.length; i++) {
					tempWords[tempWordsPointer] = words[i];
					tempWordsPointer++;
				}
				words = tempWords;

				String match = words[words.length - 1];

				// start new experiment with prefixes of various length:
				IOHelper.logResult(line + "  \t\tMATCH: " + match);
				int lastRank = Integer.MAX_VALUE;
				for (int pfl = 0; pfl < match.length(); pfl++) {
					this.totalResultMap = new HashMap<String, Float>();
					for (int sequenceDecimal = 0; sequenceDecimal < Math.pow(2,
							this.n); sequenceDecimal++) {
						if (Integer.bitCount(sequenceDecimal) == k) {
							// calculate partial result
							HashMap<String, Float> tempResultMap = this
									.calculateResultSet(words, sequenceDecimal,
											pfl, wordIndex);
							// add partial result to totalResultMap
							for (Entry<String, Float> entry : tempResultMap
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

					// collected results from all edges now find the topk, log
					// result and decide if to continue;
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
		} catch (Exception e) {
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
				this.totalResultMap, this.joinLength);
		int topkCnt = 0;

		// TODO: idea plot KSS distribution as eval
		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.logResult("HIT\tRANK: " + topkCnt
							+ " \tPREFIXLENGTH: " + pfl + " ");
					if (topkCnt < lastRank) {
						for (int k = Math.min(5, lastRank); k >= topkCnt; k--) {
							float kss = (float) (match.length() - pfl)
									/ (float) match.length();
							IOHelper.logResult("NKSS AT " + k + ": " + kss
									+ " ");
							IOHelper.logResult("KSS AT " + k + ": "
									+ (match.length() - pfl) + " ");
						}
						lastRank = topkCnt;
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + pfl + " ");
		return Integer.MAX_VALUE;
	}

	protected abstract HashMap<String, Float> calculateResultSet(
			String[] words, int sequenceDecimal, int pfl, String[] index);
}