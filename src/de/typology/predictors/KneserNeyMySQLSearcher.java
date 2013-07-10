package de.typology.predictors;

import java.util.Arrays;
import java.util.HashMap;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class KneserNeyMySQLSearcher extends NewMySQLSearcher {

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

			for (int k = n; k > 1; k--) {
				glmmss.run(n, k, Config.get().numberOfQueries,
						Config.get().weight, wordIndex);
			}
		}
	}

	public KneserNeyMySQLSearcher(String databaseName) {
		super(databaseName);
	}

	@Override
	protected HashMap<String, Float> calculateResultSet(String[] words,
			int sequenceDecimal, int pfl, String[] wordIndex) {
		HashMap<String, Float> resultMap = new HashMap<String, Float>();
		int l = words.length;
		String target = words[l - 1];
		String source;
		int leadingZeros = 0;
		if (sequenceDecimal == 1) {
			source = "true";
			// TODO:remove this:
			return null;
		} else {
			if (sequenceDecimal % 2 == 0) {
				// no target in sequence (e.g. 110)
				return null;
			}

			// ------
			source = "";
			System.out.println(Arrays.toString(words) + " ... "
					+ sequenceDecimal + " ... " + pfl);
			// ------
		}
		if (pfl > target.length()) {
			System.out.println("target: '" + target
					+ "' to short for prefixlength: " + pfl);
			return null;
		}
		String prefix = target.substring(0, pfl) + "%";
		String tableName;

		if (sequenceDecimal == 1) {
			tableName = "1_all";
		} else {
			tableName = Integer.toBinaryString(sequenceDecimal) + "_"
					+ BinarySearch.rank(words[leadingZeros], wordIndex);

		}
		String query = "";
		if (pfl > 0) {
			query = "select * from " + tableName + " where " + source
					+ " and target like \"" + prefix
					+ "\" order by score desc limit " + this.joinLength;
		} else {
			query = "select * from " + tableName + " where " + source
					+ " order by score desc limit " + this.joinLength;
		}

		return resultMap;
	}
}
