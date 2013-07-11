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
	protected Connection lowConnection = null;
	protected Connection lowDiscountConnection = null;
	protected Connection highConnection = null;
	protected Connection highDiscountConnection = null;

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
			String highQuery = this.getQuery(wordsWithoutLast, matchCut, n,
					sequenceBinaryWithoutTarget, subResultSize);
			ResultSet highResultSet = highStatement.executeQuery(highQuery);
			this.addToResultMap(highResultSet, subResultMap);

			if (subResultMap.size() < subResultSize) {
				// select high discount and lower result
				// //make statement for result and discount
				// //execute statement
			}

			// while (subResultMap.size() < subResultSize) {
			// // select low discount and lower result
			// // //make statement for result and discount
			// // //execute statement
			// }

			// ------
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// if (pfl > target.length()) {
		// System.out.println("target: '" + target
		// + "' to short for prefixlength: " + pfl);
		// return null;
		// }
		// String prefix = target.substring(0, pfl) + "%";
		// String tableName;
		//
		// if (sequenceDecimal == 1) {
		// tableName = "1_all";
		// } else {
		// tableName = Integer.toBinaryString(sequenceDecimal)
		// + "_"
		// + BinarySearch.rank(wordsWithoutLast[leadingZeros],
		// wordIndex);
		//
		// }
		// String query = "";
		// if (pfl > 0) {
		// query = "select * from " + tableName + " where " + source
		// + " and target like \"" + prefix
		// + "\" order by score desc limit " + this.joinLength;
		// } else {
		// query = "select * from " + tableName + " where " + source
		// + " order by score desc limit " + this.joinLength;
		// }

		System.out.println("---->");
		return subResultMap;
	}

	private String getQuery(String[] wordsWithoutLast, String match, int n,
			String sequenceBinaryWithoutTarget, int subResultSize) {
		String query = "select * from ";
		int tableName = BinarySearch.rank(wordsWithoutLast[0], this.wordIndex);

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
				query += "source" + (i - offset) + " =\"" + wordsWithoutLast[i]
						+ "\" ";
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
		query += "order by score desc limit " + subResultSize;
		System.out.println(query);
		return query;
	}
}
