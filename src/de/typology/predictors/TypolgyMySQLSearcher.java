package de.typology.predictors;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

public class TypolgyMySQLSearcher {
	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	private String user;
	private String databaseName;

	private HashSet<String> tabelNames;
	private HashMap<String, Float> hits;

	private int n;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();

		String test = "-";
		System.out.println(test);
		System.out.println(test.replaceAll("\\-", "\\\\-"));
		test = "?";
		System.out.println(test);
		System.out.println(test.replaceAll("\\?", "\\\\?"));
		test = "?";
		tmss.run();
	}

	public TypolgyMySQLSearcher() {
		this.user = Config.get().dbUser;
		this.databaseName = Config.get().dbName;

		this.n = 5;

		try {
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
			this.connect = DriverManager
					.getConnection("jdbc:mysql://localhost/"
							+ this.databaseName + "?" + "user=" + this.user
							+ "&password=");

			// Statements allow to issue SQL queries to the database
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

	public void run() {
		try {

			String testFile = "/var/lib/datasets/out/wikipedia/testGer7095.file";
			BufferedReader br = IOHelper.openReadFile(testFile);
			String line = "";
			long cnt = 0;
			long start = System.currentTimeMillis();
			IOHelper.setResultFile("mysql-typo-5-7095.log");
			while ((line = br.readLine()) != null) {
				/**
				 * # prepare single queries (log results for learning)
				 * 
				 * # merge results from query to get a ranking
				 * 
				 * # (apply weights)
				 * 
				 * # log results
				 * 
				 * # repeat for longer query.
				 */

				String[] words = line.split("\\ ");
				if (EvalHelper.badLine(words, this.n)) {
					continue;
				}

				String[] edgeQueryOfTyp = new String[this.n];
				String match = words[words.length - 1];
				// start new experiment with prefixes of various length:
				IOHelper.logResult(line + "  \t\tMATCH: " + match);
				for (int pfl = 0; pfl < match.length(); pfl++) {
					this.hits = new HashMap<String, Float>();

					// retrieve lists for every typology edge
					for (int i = 1; i < this.n; i++) {
						// TODO: include joinLength
						edgeQueryOfTyp[i] = this.prepareQuery(words, i, pfl);
						if (edgeQueryOfTyp[i] == null) {
							continue;
						}

						try {
							this.resultSet = this.statement
									.executeQuery(edgeQueryOfTyp[i]);

							this.logResult(i, pfl, match);

						} catch (Exception e) {
							System.err.println("error in query: "
									+ edgeQueryOfTyp[i]);
							continue;
						}
						if (cnt % 999 == 0) {
							long end = System.currentTimeMillis();
							System.out.println("queries: " + cnt + " \t time: "
									+ (end - start) + " \t qps: " + cnt * 1000
									/ (end - start));
							System.out.println(line + " \t\tWORD:"
									+ words[words.length - 1 - i]);
							this.showResult();
						}
						cnt++;
					}
					// collected results from all edges now find the topk, log
					// result and decide if to continue;

					if (1 == this.computeAndLogTop(pfl, match)) {
						break;
					}

				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int computeAndLogTop(int pfl, String match) {
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(
				this.hits, 10);
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.logResult("HIT\tRANK: " + topkCnt
							+ " \tPREFIXLENGTH: " + pfl + " ");
					if (topkCnt == 1) {
						IOHelper.logResult("KSS: " + (match.length() - pfl)
								+ " ");
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + pfl + " ");
		return -1;
	}

	private void logResult(int edgeTyp, int pfl, String match) {
		boolean inResults = false;
		try {
			float weight = this.getWeight(edgeTyp, pfl);
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				if (target.equals(match)) {
					inResults = true;
					// write score to learning file!
				}
				if (this.hits.containsKey(target)) {
					this.hits.put(target, this.hits.get(target) + weight
							* score);
				} else {
					this.hits.put(target, weight * score);
				}
			}
			if (inResults == false) {
				// write 0 prop to learning file
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// TODO: implement this function
	private float getWeight(int edgeTyp, int pfl) {
		return (float) 1.0;
	}

	private void showResult() {
		try {
			while (this.resultSet.next()) {
				String target = this.resultSet.getString("target");
				Float score = this.resultSet.getFloat("score");
				System.out.println("t: " + target + " \t s: " + score);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String prepareQuery(String[] words, int i, int pfl) {
		int joinLength = 10;
		int l = words.length;
		String target = words[l - 1];
		String source = words[l - 1 - i];
		source.replaceAll("\\-", "\\\\-");
		source.replaceAll("\\_", "\\\\_");
		source.replaceAll("\\?", "\\\\?");
		// if (source.equals("-")) {
		// System.err.println("deteced hyphen");
		// return null;
		// }
		if (pfl > target.length()) {
			System.out.println("target: '" + target
					+ "' to short for prefixlength: " + pfl);
			return null;
		}
		String prefix = target.substring(0, pfl) + "%";
		if (target.equals("-%")) {
			System.out.println("deteced hyphen");
			return null;
		}
		String tableName = i + "es" + source.charAt(0);
		if (!this.tabelNames.contains(tableName)) {
			tableName = i + "esother";
		}
		String query = "select * from " + tableName + " where source =\""
				+ source + "\" and target like \"" + prefix
				+ "\" order by score desc limit " + joinLength;

		return query;
	}
}
