package de.typology.predictors;

import java.io.BufferedReader;
import java.io.IOException;
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

public abstract class MySQLSearcher {

	protected Connection connect = null;
	protected Statement statement = null;
	protected ResultSet resultSet = null;
	protected String user;
	protected String databaseName;
	protected HashSet<String> tabelNames;
	private HashMap<String, Float> hits;
	protected int n;
	protected int joinLength;
	protected boolean useWeights;

	protected float[] learnHMMScores;
	private float[][] learnPicWeights;
	private float[][] picWeights;
	private float[][] HMMWeights;
	private final int MAX_PFL;

	public MySQLSearcher() {
		super();
		this.n = 5;
		this.MAX_PFL = 1024;
		this.joinLength = 10;
		this.user = Config.get().dbUser;
		this.databaseName = Config.get().dbName;
		this.useWeights = false;
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

	public void run(int n, int numberOfQueries, String weights) {
		this.n = n;
		this.learnHMMScores = new float[this.n];
		this.learnPicWeights = new float[this.MAX_PFL][this.n];
		this.HMMWeights = new float[this.MAX_PFL][this.n];
		for (int i = 0; i < this.MAX_PFL; i++) {
			for (int j = 0; j < n; j++) {
				this.learnPicWeights[i][j] = 0.0f;
			}
		}
		String fileName = EvalHelper.gennerateFileName(this.getClass()
				.getName().replaceAll("de.typology.predictors.", "")
				.replaceAll("MySQLSearcher", "").toLowerCase(), weights,
				this.n, this.joinLength, numberOfQueries);

		String testFile = "";
		if (!weights.equals("no")) {
			this.useWeights = true;
			testFile = Config.get().learningPath;
			if (weights.equals("pic")) {
				this.openPicWeigths("rawlog/learnPic-" + fileName);
			}
			if (weights.equals("HMM")) {
				this.openHMMWeigths("rawlog/HMMWeights-" + fileName);
			}
		} else {
			testFile = Config.get().testingPath;
			this.useWeights = false;
		}
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
				String[] edgeQueryOfTyp = new String[this.n];
				// TODO: replace revert replace hyphon...
				String match = words[words.length - 1];

				// start new experiment with prefixes of various length:
				IOHelper.logResult(line + "  \t\tMATCH: " + match);
				int lastRank = Integer.MAX_VALUE;
				for (int pfl = 0; pfl < match.length(); pfl++) {
					// final results will be stored in hits hashmap
					this.hits = new HashMap<String, Float>();

					// if we don't use weights we have to set the array of
					// scores for HMM learning to 0
					if (!this.useWeights) {
						this.resetHMMArray();
					}

					// retrieve results lists for every Typology edge
					for (int i = 1; i < this.n; i++) {
						edgeQueryOfTyp[i] = this.prepareQuery(words, i, pfl);
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
							continue;
						}
					}
					// collected results from all edges now find the topk, log
					// result and decide if to continue;
					lastRank = this.computeAndLogTop(pfl, match, lastRank);
					// TODO: log pic weights learning method.
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
				if (cnt > numberOfQueries) {
					this.logPicWeights();
					return;
				}
			}
			this.logPicWeights();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void openHMMWeigths(String fileName) {
		BufferedReader br = IOHelper.openReadFile(fileName.replace("HMM-",
				"no-"));
		String line = "";
		try {
			this.HMMWeights = new float[this.MAX_PFL][this.n];
			int pfl = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\t");
				this.HMMWeights[pfl][0] = 0.0f;
				for (int i = 1; i < this.n; i++) {
					this.HMMWeights[pfl][i] = Float.parseFloat(values[i + 1]);
				}
				pfl++;
			}
			while (pfl < this.MAX_PFL) {
				for (int i = 0; i < this.n; i++) {
					this.HMMWeights[pfl][i] = 1.0f;
				}
				pfl++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void openPicWeigths(String fileName) {
		BufferedReader br = IOHelper
				.openReadFile(fileName.replace("pic", "no"));
		String line = "";
		try {
			this.picWeights = new float[this.MAX_PFL][this.n];
			int pfl = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\t");
				this.picWeights[pfl][0] = 0.0f;
				float max = 0.0f;
				for (int i = 1; i < this.n; i++) {
					this.picWeights[pfl][i] = Float.parseFloat(values[i + 1]);
					if (this.picWeights[pfl][i] > max) {
						max = this.picWeights[pfl][i];
					}
				}
				if (max > 0.1f) {
					for (int i = 1; i < this.n; i++) {
						this.picWeights[pfl][i] = this.picWeights[pfl][i] / max
								* 100.0f;
					}
				}
				pfl++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void logPicWeights() {
		String res = "";
		for (int i = 0; i < this.MAX_PFL; i++) {
			res = "" + i;
			for (int j = 0; j < this.n; j++) {
				res = res + "\t" + this.learnPicWeights[i][j];
			}
			IOHelper.logLearnPic(res);
		}
	}

	private void resetHMMArray() {
		for (int i = 0; i < this.n; i++) {
			this.learnHMMScores[i] = 0.0f;
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
					if (!this.useWeights) {
						this.logHMMWeights(pfl);
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + pfl + " ");
		if (!this.useWeights) {
			this.logHMMWeights(pfl);
		}
		return Integer.MAX_VALUE;
	}

	private void logHMMWeights(int pfl) {
		String res = "" + pfl;
		for (int i = 1; i < this.n; i++) {
			res = res + "\t" + this.learnHMMScores[i];
		}
		IOHelper.logLearnHMM(res);
	}

	private void updateRankWeights(int pfl, int edgeType, float mrr) {
		this.learnPicWeights[pfl][edgeType] += mrr;
	}

	/**
	 * this method addes the results of a single query to the result hashmap
	 * hits
	 * 
	 * it also logs information for HMM weight learning and for renes weight
	 * learning method
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
			float weight = this.getWeight(edgeTyp, pfl);
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				// log for weight learning
				if (target.equals(match)) {
					this.learnHMMScores[edgeTyp] = score;
					this.updateRankWeights(pfl, edgeTyp, 1.0f / score);
				}
				if (this.hits.containsKey(target)) {
					this.hits.put(target, this.hits.get(target) + weight
							* score);

				} else {
					this.hits.put(target, weight * score);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void logSingleQueryWithMultResult(int edgeTyp, int pfl, String match) {
		try {
			float weight = this.getWeight(edgeTyp, pfl);
			HashMap<String, Float> tmp = new HashMap<String, Float>();
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				// log for weight learning
				if (target.equals(match)) {
					this.learnHMMScores[edgeTyp] = score;
					this.updateRankWeights(pfl, edgeTyp, 1.0f / score);
				}
				// if (this.hits.containsKey(target)) {
				// this.hits.put(target, this.hits.get(target) * weight
				// * score);
				//
				// } else {
				// this.hits.put(target, weight * score);
				// }

				if (edgeTyp == 1) {
					this.hits.put(target, weight * score);
				}
				if (edgeTyp > 1) {
					if (this.hits.containsKey(target)) {
						tmp.put(target, this.hits.get(target) * weight * score);
					} else {
						tmp.put(target, (float) (weight * score * Math.pow(
								0.00000001, edgeTyp)));
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

	/**
	 * this function can look up and return the weight for a current query
	 * 
	 * @param edgeTyp
	 * @param pfl
	 * @return
	 */
	private float getWeight(int edgeTyp, int pfl) {
		if (!this.useWeights) {
			return 1.0f;
		} else {
			if (Config.get().weight.equals("pic")) {
				return this.picWeights[pfl][edgeTyp];
			} else if (Config.get().weight.equals("HMM")) {
				return this.HMMWeights[pfl][edgeTyp];
			} else {
				return 1.0f;
			}

		}
	}

	protected abstract String prepareQuery(String[] words, int i, int pfl);
}