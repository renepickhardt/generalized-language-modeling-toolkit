package de.typology.predictors;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public abstract class NewMySQLSearcher {

	protected Connection connect = null;
	protected Statement statement = null;
	protected ResultSet resultSet = null;
	protected String user;
	protected String databaseName;
	protected HashSet<String> tabelNames;
	private HashMap<String, Float> hits;
	protected int n;
	protected int joinLength;

	public NewMySQLSearcher(String databaseName) {
		this.n = Config.get().modelLength;
		this.joinLength = 10;
		this.user = Config.get().dbUser;
		this.databaseName = databaseName;
		IOHelper.strongLog("dbName: " + this.databaseName);
		IOHelper.strongLog("userName: " + this.user);
		try {
			Class.forName("com.mysql.jdbc.Driver");
			this.connect = DriverManager
					.getConnection("jdbc:mysql://localhost/"
							+ this.databaseName + "?" + "user=" + this.user
							+ "&password=");

			this.statement = this.connect.createStatement();

			this.tabelNames = new HashSet<String>();
			this.resultSet = this.statement.executeQuery("show tables");
			while (this.resultSet.next()) {
				String tableName = this.resultSet.getString(1);
				this.tabelNames.add(tableName);
			}

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
				+ "/" + Config.get().testedOnLang + "/learning-splitted.txt";
		IOHelper.strongLog("testFile: " + testFile);
		try {
			EvalHelper.openAndSetResultLogFile(fileName);

			BufferedReader br = IOHelper.openReadFile(testFile);
			String line = "";
			long cnt = 0;
			long start = System.currentTimeMillis();

			// for every line in our test data
			while ((line = br.readLine()) != null) {
				// check if line is suitable to make an experiment
				String[] words = line.split("\\ ");
				if (EvalHelper.badLine(words, this.n)) {
					continue;
				}

				// create datstructure to hold the mysql query
				String[] edgeQueryOfTyp = new String[(int) Math.pow(2, this.n)];
				// TODO: replace revert replace hyphon...
				String match = words[words.length - 1];

				// start new experiment with prefixes of various length:
				IOHelper.logResult(line + "  \t\tMATCH: " + match);
				int lastRank = Integer.MAX_VALUE;
				for (int pfl = 0; pfl < match.length(); pfl++) {
					// final results will be stored in hits hashmap
					this.hits = new HashMap<String, Float>();

					for (int i = 0; i < Math.pow(2, this.n); i++) {
						edgeQueryOfTyp[i] = this.prepareQuery(words, i, pfl,
								wordIndex);
						if (edgeQueryOfTyp[i] == null) {
							continue;
						}

						try {
							this.resultSet = this.statement
									.executeQuery(edgeQueryOfTyp[i]);

							this.logSingleQueryResult(i, pfl, match);
							// this.logSingleQueryWithMultResult(i, pfl, match);

						} catch (Exception e) {
							System.err.println("error in query: "
									+ edgeQueryOfTyp[i]);
							e.printStackTrace();

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
				this.hits, this.joinLength);
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

	/**
	 * this method addes the results of a single query to the result hashmap
	 * hits
	 * 
	 * @param edgeTyp
	 *            Typ of current query
	 * @param pfl
	 *            Prefix length
	 * @param match
	 *            correct word that needs to predicted.
	 */
	private void logSingleQueryResult(int edgeTyp, int pfl, String match) {
		try {
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				if (this.hits.containsKey(target)) {
					this.hits.put(target, this.hits.get(target) + score);
				} else {
					this.hits.put(target, score);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void logSingleQueryWithMultResult(int edgeTyp, int pfl, String match) {
		try {
			HashMap<String, Float> tmp = new HashMap<String, Float>();
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				// if (this.hits.containsKey(target)) {
				// this.hits.put(target, this.hits.get(target) * weight
				// * score);
				//
				// } else {
				// this.hits.put(target, weight * score);
				// }

				if (edgeTyp == 1) {
					this.hits.put(target, score);
				}
				if (edgeTyp > 1) {
					if (this.hits.containsKey(target)) {
						tmp.put(target, this.hits.get(target) * score);
					} else {
						tmp.put(target,
								(float) (score * Math.pow(0.00000001, edgeTyp)));
					}
				}
			}
			if (edgeTyp > 1) {
				// this.hits = new HashMap<String, Float>();
				for (String str : tmp.keySet()) {
					this.hits.put(str, tmp.get(str));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected abstract String prepareQuery(String[] words, int i, int pfl,
			String[] index);
}