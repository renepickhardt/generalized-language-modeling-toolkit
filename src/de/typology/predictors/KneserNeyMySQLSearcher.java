package de.typology.predictors;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class KneserNeyMySQLSearcher extends NewMySQLSearcher {
	protected Connection lowConnection;
	protected Connection lowDiscountConnection;
	protected Connection highConnection;
	protected Connection highDiscountConnection;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IndexBuilder ib = new IndexBuilder();
		String databaseName = Config.get().trainedOnDataSet + "_"
				+ Config.get().trainedOnLang + "_" + Config.get().dataBaseType;
		String indexPath = Config.get().outputDirectory + "/"
				+ Config.get().trainedOnDataSet + "/"
				+ Config.get().trainedOnLang + "/index.txt";
		String[] wordIndex = ib.deserializeIndex(indexPath);

		KneserNeyMySQLSearcher glmmss = new KneserNeyMySQLSearcher(databaseName);
		// Config.get().weight = "no";
		// for (int i = 5; i > 1; i--) {
		// IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
		// + i);
		// tmss.run(i, 100000, Config.get().weight);
		// }
		// Config.get().weight = "pic";

		for (int n = 5; n > 1; n--) {
			IOHelper.strongLog("model parameter: " + n);
			for (int k = n - 1; k > 0; k--) {
				glmmss.run(n, k, Config.get().numberOfQueries, wordIndex);
			}
		}
	}

	public KneserNeyMySQLSearcher(String dataBaseName) {
		super();
		this.smoothingType = Config.get().dataBaseType.replaceAll("_", "");
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

	@Override
	protected HashMap<String, Float> calculateResultSet(
			String[] wordsWithoutLast, String match, int n,
			int sequenceDecimal, int pfl, String[] wordIndex) {
		System.out.println("<----");
		String matchCut = match.substring(0, pfl);
		HashMap<String, Float> subResultMap = new HashMap<String, Float>();
		int subResultSize = Config.get().subResultSize;
		try {
			System.out.println(Arrays.toString(wordsWithoutLast) + " seqDec: "
					+ sequenceDecimal + " pfl: " + pfl);
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			String sequenceBinaryWithoutTarget = sequenceBinary.substring(0,
					sequenceBinary.length() - 1);

			// ------
			// select high result
			Statement highStatement = this.highConnection.createStatement();
			String highQuery = this.getResultQuery(wordsWithoutLast, matchCut,
					n, sequenceBinaryWithoutTarget);
			ResultSet highResultSet = highStatement.executeQuery(highQuery);
			this.addToResultMap(highResultSet, subResultMap, 1.0f);
			highResultSet.close();
			highStatement.close();

			if (subResultMap.size() < subResultSize) {
				// get high discount
				Statement highDiscountStatement = this.highDiscountConnection
						.createStatement();
				String highDiscountResultQuery = this.getDiscountQuery(
						wordsWithoutLast, matchCut, n,
						sequenceBinaryWithoutTarget);
				ResultSet highDiscountResultSet = highDiscountStatement
						.executeQuery(highDiscountResultQuery);
				if (!highDiscountResultSet.next()) {
					return subResultMap;
				}
				float highDiscountResult = highDiscountResultSet
						.getFloat("score");
				System.out.println("highDiscount: " + highDiscountResult);
				highDiscountResultSet.close();
				highDiscountStatement.close();

				Statement lowStatement = this.lowConnection.createStatement();
				String[] wordsWithoutLastRemoveFirst = new String[wordsWithoutLast.length - 1];
				for (int i = 1; i < wordsWithoutLast.length; i++) {
					wordsWithoutLastRemoveFirst[i - 1] = wordsWithoutLast[i];
				}
				System.out.println(sequenceBinaryWithoutTarget);

				// remove first and leading zeros
				String sequenceBinaryWithoutTargetRemoveFirst = sequenceBinaryWithoutTarget
						.substring(1);
				while (sequenceBinaryWithoutTargetRemoveFirst.startsWith("0")) {
					sequenceBinaryWithoutTargetRemoveFirst = sequenceBinaryWithoutTargetRemoveFirst
							.substring(1);
				}

				String lowQuery = this.getResultQuery(
						wordsWithoutLastRemoveFirst, matchCut, n - 1,
						sequenceBinaryWithoutTargetRemoveFirst);
				ResultSet lowResultSet = lowStatement.executeQuery(lowQuery);
				this.addToResultMap(lowResultSet, subResultMap,
						highDiscountResult);
				lowResultSet.close();
				lowStatement.close();
				if (sequenceBinaryWithoutTargetRemoveFirst.length() == 0) {
					return subResultMap;
				}

				while (subResultMap.size() < subResultSize) {
					Statement lowDiscountStatement = this.lowDiscountConnection
							.createStatement();
					String lowDiscountResultQuery = this.getDiscountQuery(
							wordsWithoutLastRemoveFirst, matchCut, n - 1,
							sequenceBinaryWithoutTargetRemoveFirst);
					ResultSet lowDiscountResultSet = lowDiscountStatement
							.executeQuery(lowDiscountResultQuery);
					if (!lowDiscountResultSet.next()) {
						return subResultMap;
					}
					float lowDiscountResult = lowDiscountResultSet
							.getFloat("score");
					System.out.println("lowDiscount: " + lowDiscountResult);
					lowDiscountResultSet.close();
					lowDiscountStatement.close();

					lowStatement = this.lowConnection.createStatement();
					wordsWithoutLastRemoveFirst = new String[wordsWithoutLast.length - 1];
					for (int i = 1; i < wordsWithoutLast.length; i++) {
						wordsWithoutLastRemoveFirst[i - 1] = wordsWithoutLast[i];
					}

					// remove first and leading zeros
					sequenceBinaryWithoutTargetRemoveFirst = sequenceBinaryWithoutTargetRemoveFirst
							.substring(1);
					while (sequenceBinaryWithoutTargetRemoveFirst
							.startsWith("0")) {
						sequenceBinaryWithoutTargetRemoveFirst = sequenceBinaryWithoutTargetRemoveFirst
								.substring(1);
					}
					if (sequenceBinaryWithoutTargetRemoveFirst.length() == 0) {
						// calculate lowest order probability
						return subResultMap;
					}

					lowQuery = this.getResultQuery(wordsWithoutLastRemoveFirst,
							matchCut, n - 1,
							sequenceBinaryWithoutTargetRemoveFirst);
					lowResultSet = lowStatement.executeQuery(lowQuery);
					this.addToResultMap(lowResultSet, subResultMap,
							lowDiscountResult);
					lowResultSet.close();
					lowStatement.close();
				}
			}

			// ------
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("---->");
		return subResultMap;
	}

	private String getResultQuery(String[] wordsWithoutLast, String match,
			int n, String sequenceBinaryWithoutTarget) {
		String query = "select * from ";
		if (!sequenceBinaryWithoutTarget.isEmpty()) {
			int tableName = BinarySearch.rank(wordsWithoutLast[0],
					this.wordIndex);

			// add tableName
			query += sequenceBinaryWithoutTarget + "1_" + tableName + " ";
			query += "where ";

			// add sources
			int numberOfSources = Integer.bitCount(Integer.parseInt(
					sequenceBinaryWithoutTarget, 2));
			int currentNumberOfSources = 0;
			int offset = 0;
			String sequenceBinaryWithoutTargetWithLeadingZeros = sequenceBinaryWithoutTarget;
			while (sequenceBinaryWithoutTargetWithLeadingZeros.length() < n - 1) {
				sequenceBinaryWithoutTargetWithLeadingZeros = "0"
						+ sequenceBinaryWithoutTargetWithLeadingZeros;
				offset++;
			}
			System.out.println("n " + n);
			System.out.println(sequenceBinaryWithoutTargetWithLeadingZeros);
			for (int i = 0; i < sequenceBinaryWithoutTargetWithLeadingZeros
					.length(); i++) {
				if (sequenceBinaryWithoutTargetWithLeadingZeros.charAt(i) == '1') {
					query += "source" + (i - offset) + " =\""
							+ wordsWithoutLast[i] + "\" ";
					currentNumberOfSources++;
					if (currentNumberOfSources < numberOfSources) {
						query += "and ";
					}
				}
			}
			if (!match.isEmpty()) {
				// add target
				query += "and target like \"" + match + "%\" ";
			}
		} else {
			query += "1_all ";
			// no source
			if (!match.isEmpty()) {
				// add target
				query += "where target like \"" + match + "%\" ";
			}
		}
		query += "order by score desc limit " + this.subResultSize;
		System.out.println(query);
		return query;
	}

	private String getDiscountQuery(String[] wordsWithoutLast, String match,
			int n, String sequenceBinaryWithoutTarget) {
		String query = "select * from ";

		// add tableName
		if (Integer.bitCount(Integer.parseInt(sequenceBinaryWithoutTarget, 2)) == 1) {
			query += sequenceBinaryWithoutTarget + "_all ";
		} else {
			int tableName = BinarySearch.rank(wordsWithoutLast[0],
					this.wordIndex);
			query += sequenceBinaryWithoutTarget + "_" + tableName + " ";
		}
		query += "where ";

		// add sources
		int numberOfSources = Integer.bitCount(Integer.parseInt(
				sequenceBinaryWithoutTarget, 2));
		int currentNumberOfSources = 0;
		int offset = 0;
		String sequenceBinaryWithoutTargetWithLeadingZeros = sequenceBinaryWithoutTarget;
		while (sequenceBinaryWithoutTargetWithLeadingZeros.length() < n - 1) {
			sequenceBinaryWithoutTargetWithLeadingZeros = "0"
					+ sequenceBinaryWithoutTargetWithLeadingZeros;
			offset++;
		}
		System.out.println("n " + n);
		System.out.println(sequenceBinaryWithoutTargetWithLeadingZeros);
		for (int i = 0; i < sequenceBinaryWithoutTargetWithLeadingZeros
				.length(); i++) {
			if (sequenceBinaryWithoutTargetWithLeadingZeros.charAt(i) == '1') {
				query += "source" + (i - offset) + " =\"" + wordsWithoutLast[i]
						+ "\" ";
				currentNumberOfSources++;
				if (currentNumberOfSources < numberOfSources) {
					query += "and ";
				}
			}
		}
		query += "order by score desc limit " + this.subResultSize;
		System.out.println(query);
		return query;
	}
}
